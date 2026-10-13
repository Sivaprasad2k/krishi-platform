package com.krishi.backend.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.weather.dto.*;
import com.krishi.backend.weather.entity.AdvisoryStatus;
import com.krishi.backend.weather.entity.WeatherAdvisory;
import com.krishi.backend.weather.entity.WeatherForecastCache;
import com.krishi.backend.weather.provider.ProviderForecastResult;
import com.krishi.backend.weather.provider.WeatherDataProvider;
import com.krishi.backend.weather.provider.WeatherProviderException;
import com.krishi.backend.weather.repository.WeatherAdvisoryRepository;
import com.krishi.backend.weather.repository.WeatherForecastCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherForecastService {

    private final FarmRepository farmRepository;
    private final TaskRepository taskRepository;
    private final DistrictCentroidResolver districtCentroidResolver;
    private final WeatherDataProvider weatherDataProvider;
    private final WeatherForecastCacheRepository cacheRepository;
    private final WeatherAdvisoryEngine weatherAdvisoryEngine;
    private final WeatherAdvisoryRepository advisoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WeatherForecastResponse getWeatherForFarm(UUID farmId, CustomUserDetails userDetails) {
        Farm farm = getFarmAndValidateOwnership(farmId, userDetails);
        ResolvedLocation location = districtCentroidResolver.resolveLocation(farm);

        Optional<WeatherForecastCache> cachedOpt = cacheRepository.findByGridKey(location.getGridKey());

        // Check if cache exists and is fresh (within 2 hours)
        if (cachedOpt.isPresent() && cachedOpt.get().getExpiresAt().isAfter(OffsetDateTime.now())) {
            WeatherForecastCache cache = cachedOpt.get();
            try {
                ProviderForecastResult result = objectMapper.readValue(cache.getForecastPayload(), ProviderForecastResult.class);
                return mapToResponse(farm.getId(), location, result, cache.getFetchedAt(), cache.getExpiresAt(), false);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached weather payload for grid key {}: {}", location.getGridKey(), e.getMessage());
            }
        }

        // Cache missing or expired: Fetch fresh forecast
        try {
            ProviderForecastResult freshResult = weatherDataProvider.fetchForecast(location.getLatitude(), location.getLongitude());
            WeatherForecastCache savedCache = saveOrUpdateCache(location, freshResult);

            // Trigger advisory evaluation
            weatherAdvisoryEngine.evaluateAdvisories(farm, freshResult.getDaily());

            return mapToResponse(farm.getId(), location, freshResult, savedCache.getFetchedAt(), savedCache.getExpiresAt(), false);
        } catch (WeatherProviderException e) {
            log.warn("External weather provider failed for farm {}: {}", farmId, e.getMessage());

            // Check if we can serve stale cache as a fallback
            if (cachedOpt.isPresent()) {
                WeatherForecastCache staleCache = cachedOpt.get();
                try {
                    ProviderForecastResult staleResult = objectMapper.readValue(staleCache.getForecastPayload(), ProviderForecastResult.class);
                    log.info("Serving stale cached forecast for farm {} (fetched at {})", farmId, staleCache.getFetchedAt());
                    return mapToResponse(farm.getId(), location, staleResult, staleCache.getFetchedAt(), staleCache.getExpiresAt(), true);
                } catch (Exception parseEx) {
                    log.error("Failed to parse stale forecast cache: {}", parseEx.getMessage());
                }
            }

            // No cache available to fall back upon
            throw e;
        }
    }

    @Transactional
    public WeatherForecastResponse refreshWeatherForFarm(UUID farmId, CustomUserDetails userDetails) {
        Farm farm = getFarmAndValidateOwnership(farmId, userDetails);
        ResolvedLocation location = districtCentroidResolver.resolveLocation(farm);

        Optional<WeatherForecastCache> cachedOpt = cacheRepository.findByGridKey(location.getGridKey());

        // Enforce 15-minute cooldown to prevent external API hammering
        if (cachedOpt.isPresent()) {
            WeatherForecastCache cache = cachedOpt.get();
            if (cache.getFetchedAt().isAfter(OffsetDateTime.now().minusMinutes(15))) {
                log.info("Forecast for grid key {} was fetched within 15 minutes. Returning recent cache.", location.getGridKey());
                try {
                    ProviderForecastResult result = objectMapper.readValue(cache.getForecastPayload(), ProviderForecastResult.class);
                    return mapToResponse(farm.getId(), location, result, cache.getFetchedAt(), cache.getExpiresAt(), false);
                } catch (Exception e) {
                    log.warn("Failed to parse cache payload during cooldown check: {}", e.getMessage());
                }
            }
        }

        // Fetch fresh forecast
        ProviderForecastResult freshResult = weatherDataProvider.fetchForecast(location.getLatitude(), location.getLongitude());
        WeatherForecastCache savedCache = saveOrUpdateCache(location, freshResult);

        // Re-evaluate advisories
        weatherAdvisoryEngine.evaluateAdvisories(farm, freshResult.getDaily());

        return mapToResponse(farm.getId(), location, freshResult, savedCache.getFetchedAt(), savedCache.getExpiresAt(), false);
    }

    @Transactional(readOnly = true)
    public List<WeatherAdvisoryResponse> getAdvisoriesForFarm(UUID farmId, CustomUserDetails userDetails) {
        getFarmAndValidateOwnership(farmId, userDetails);
        List<WeatherAdvisory> advisories = advisoryRepository.findByFarmIdAndStatusInOrderByTargetDateAsc(
                farmId, List.of(AdvisoryStatus.ACTIVE, AdvisoryStatus.ACKNOWLEDGED)
        );
        return advisories.stream().map(this::mapToAdvisoryResponse).collect(Collectors.toList());
    }

    @Transactional
    public WeatherAdvisoryResponse acknowledgeAdvisory(UUID advisoryId, CustomUserDetails userDetails) {
        WeatherAdvisory advisory = advisoryRepository.findById(advisoryId)
                .orElseThrow(() -> new IllegalArgumentException("Advisory not found with id: " + advisoryId));

        validateFarmOwnership(advisory.getFarm(), userDetails);

        advisory.setStatus(AdvisoryStatus.ACKNOWLEDGED);
        advisory.setAcknowledgedAt(OffsetDateTime.now());
        WeatherAdvisory saved = advisoryRepository.save(advisory);

        return mapToAdvisoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WeatherAdvisoryResponse> getAdvisoriesForTask(UUID taskId, CustomUserDetails userDetails) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));

        Farm farm = task.getCropPlanting().getCropCycle().getFarm();
        validateFarmOwnership(farm, userDetails);

        List<WeatherAdvisory> advisories = advisoryRepository.findByTaskIdAndStatus(taskId, AdvisoryStatus.ACTIVE);
        return advisories.stream().map(this::mapToAdvisoryResponse).collect(Collectors.toList());
    }

    private WeatherForecastCache saveOrUpdateCache(ResolvedLocation location, ProviderForecastResult result) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusHours(2);

        WeatherForecastCache cache = cacheRepository.findByGridKey(location.getGridKey())
                .orElseGet(() -> {
                    WeatherForecastCache newCache = new WeatherForecastCache();
                    newCache.setId(UUID.randomUUID());
                    newCache.setGridKey(location.getGridKey());
                    newCache.setNormalizedLatitude(location.getNormalizedLatitude());
                    newCache.setNormalizedLongitude(location.getNormalizedLongitude());
                    return newCache;
                });

        if (result.getCurrent() != null) {
            cache.setTemperatureCelsius(result.getCurrent().getTemperatureCelsius());
            cache.setRelativeHumidity(result.getCurrent().getRelativeHumidity());
            cache.setPrecipitationProbability(result.getCurrent().getPrecipitationProbability());
            cache.setPrecipitationMm(result.getCurrent().getPrecipitationMm());
            cache.setWindSpeedKmh(result.getCurrent().getWindSpeedKmh());
            cache.setWindGustsKmh(result.getCurrent().getWindGustsKmh());
            cache.setWeatherCode(result.getCurrent().getWeatherCode());
            cache.setWeatherCondition(result.getCurrent().getWeatherCondition());
        }

        try {
            cache.setForecastPayload(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Failed to serialize forecast payload: {}", e.getMessage());
            cache.setForecastPayload("{}");
        }

        cache.setFetchedAt(now);
        cache.setExpiresAt(expiresAt);

        try {
            return cacheRepository.save(cache);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            log.info("Concurrent insert detected for weather grid key {}. Loading committed cache.", location.getGridKey());
            return cacheRepository.findByGridKey(location.getGridKey()).orElse(cache);
        }
    }

    private WeatherForecastResponse mapToResponse(
            UUID farmId,
            ResolvedLocation location,
            ProviderForecastResult result,
            OffsetDateTime fetchedAt,
            OffsetDateTime expiresAt,
            boolean stale
    ) {
        return WeatherForecastResponse.builder()
                .farmId(farmId)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .coordinateSource(location.getCoordinateSource())
                .locationLabel(location.getLocationLabel())
                .stale(stale)
                .fetchedAt(fetchedAt)
                .expiresAt(expiresAt)
                .current(result.getCurrent())
                .daily(result.getDaily() != null ? result.getDaily() : Collections.emptyList())
                .hourly(result.getHourly() != null ? result.getHourly() : Collections.emptyList())
                .attribution(weatherDataProvider.getAttribution())
                .attributionUrl(weatherDataProvider.getAttributionUrl())
                .build();
    }

    private WeatherAdvisoryResponse mapToAdvisoryResponse(WeatherAdvisory advisory) {
        String cropName = advisory.getCropPlanting() != null ? advisory.getCropPlanting().getCropName() : null;
        String taskTitle = advisory.getTask() != null ? advisory.getTask().getTitle() : null;

        return WeatherAdvisoryResponse.builder()
                .id(advisory.getId())
                .farmId(advisory.getFarm().getId())
                .cropPlantingId(advisory.getCropPlanting() != null ? advisory.getCropPlanting().getId() : null)
                .cropName(cropName)
                .taskId(advisory.getTask() != null ? advisory.getTask().getId() : null)
                .taskTitle(taskTitle)
                .ruleCode(advisory.getRuleCode())
                .severity(advisory.getSeverity())
                .title(advisory.getTitle())
                .message(advisory.getMessage())
                .recommendedAction(advisory.getRecommendedAction())
                .targetDate(advisory.getTargetDate())
                .status(advisory.getStatus())
                .createdAt(advisory.getCreatedAt())
                .acknowledgedAt(advisory.getAcknowledgedAt())
                .build();
    }

    private Farm getFarmAndValidateOwnership(UUID farmId, CustomUserDetails userDetails) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + farmId));
        validateFarmOwnership(farm, userDetails);
        return farm;
    }

    private void validateFarmOwnership(Farm farm, CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Access denied. User is not authenticated.");
        }
        boolean isFarmer = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_FARMER"));
        if (!isFarmer) {
            throw new AccessDeniedException("Access denied. Only farmers are allowed to access this resource.");
        }
        if (!farm.getOwner().getId().equals(userDetails.getUserId())) {
            throw new AccessDeniedException("Access denied. You do not own this farm.");
        }
    }
}
