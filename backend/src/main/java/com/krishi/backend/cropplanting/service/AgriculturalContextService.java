package com.krishi.backend.cropplanting.service;

import com.krishi.backend.common.exception.LocationUnavailableException;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropcycle.entity.CropCycle;
import com.krishi.backend.cropplanting.dto.context.*;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import com.krishi.backend.cultivation.entity.CultivationPlan;
import com.krishi.backend.cultivation.entity.CultivationPlanStatus;
import com.krishi.backend.cultivation.repository.CultivationPlanRepository;
import com.krishi.backend.cultivation.service.AgronomicStageResolverService;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.weather.dto.CoordinateSource;
import com.krishi.backend.weather.dto.ResolvedLocation;
import com.krishi.backend.weather.dto.WeatherAdvisoryResponse;
import com.krishi.backend.weather.dto.WeatherForecastResponse;
import com.krishi.backend.weather.dto.WeatherObservation;
import com.krishi.backend.weather.entity.AdvisoryStatus;
import com.krishi.backend.weather.entity.WeatherAdvisory;
import com.krishi.backend.weather.provider.WeatherProviderException;
import com.krishi.backend.weather.repository.WeatherAdvisoryRepository;
import com.krishi.backend.weather.service.DistrictCentroidResolver;
import com.krishi.backend.weather.service.WeatherForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service establishing unified, contextual agricultural intelligence
 * for a specific crop planting, aggregating agronomic stage estimates,
 * farm location, active cultivation plans, actionable tasks, and weather conditions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgriculturalContextService {

    private final CropPlantingRepository cropPlantingRepository;
    private final AgronomicStageResolverService agronomicStageResolverService;
    private final CultivationPlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final WeatherForecastService weatherForecastService;
    private final WeatherAdvisoryRepository advisoryRepository;
    private final DistrictCentroidResolver districtCentroidResolver;

    /**
     * Resolves the complete Agricultural Context for a given crop planting as of an optional evaluation date.
     * Enforces farmer role and farm ownership verification.
     *
     * @param plantingId  the crop planting ID
     * @param asOfDate    optional evaluation date (defaults to current date)
     * @param userDetails authenticated user details
     * @return the resolved AgriculturalContextResponse
     */
    @PreAuthorize("hasRole('FARMER')")
    @Transactional(readOnly = true)
    public AgriculturalContextResponse getAgriculturalContext(UUID plantingId, LocalDate asOfDate, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        validateCycleOwnership(planting.getCropCycle(), userDetails.getUserId());

        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();
        OffsetDateTime evaluatedAt = OffsetDateTime.now();
        Farm farm = planting.getCropCycle().getFarm();

        // 1. Crop Identity Context
        CropIdentityContext cropContext = buildCropIdentityContext(planting, targetDate);

        // 2. Growth Stage Context (Deterministic model vs farmer-observed)
        GrowthStageContext growthStageContext = buildGrowthStageContext(planting, targetDate);

        // 3. Location Context
        LocationContext locationContext = buildLocationContext(farm);

        // 4. Active Cultivation Plan Summary
        CultivationPlanSummaryContext activePlanContext = buildActivePlanSummary(planting.getId());

        // 5. Relevant Operational Tasks
        List<OperationalTaskContext> relevantTasks = buildRelevantTasks(planting.getId(), targetDate);

        // 6. Weather Context & Active Advisories
        WeatherContext weatherContext = resolveWeatherContext(farm, planting.getId(), targetDate, userDetails);

        return AgriculturalContextResponse.builder()
                .asOfDate(targetDate)
                .evaluatedAt(evaluatedAt)
                .crop(cropContext)
                .growthStage(growthStageContext)
                .location(locationContext)
                .activePlan(activePlanContext)
                .relevantTasks(relevantTasks)
                .weather(weatherContext)
                .build();
    }

    /**
     * Resolves environmental weather context and active advisories for a given farm and planting as of today.
     */
    public WeatherContext resolveWeatherContext(Farm farm, UUID plantingId, CustomUserDetails userDetails) {
        return resolveWeatherContext(farm, plantingId, LocalDate.now(), userDetails);
    }

    /**
     * Resolves environmental weather context and active advisories for a given farm and planting.
     * Clearly distinguishes current environmental conditions from historical asOfDate evaluations.
     * Gracefully captures location unavailability and provider outages without failing the request.
     */
    public WeatherContext resolveWeatherContext(Farm farm, UUID plantingId, LocalDate targetDate, CustomUserDetails userDetails) {
        // Pre-validate location before calling weather forecast service
        try {
            districtCentroidResolver.resolveLocation(farm);
        } catch (LocationUnavailableException e) {
            log.info("Location coordinates or district unavailable for farm {}: {}", farm.getId(), e.getMessage());
            return WeatherContext.builder()
                    .freshness(WeatherFreshnessStatus.LOCATION_UNAVAILABLE)
                    .temporalScope("CURRENT_ENVIRONMENTAL_CONDITIONS")
                    .statusMessage(e.getMessage())
                    .activeAdvisories(getAdvisoriesForPlanting(farm.getId(), plantingId))
                    .build();
        }

        try {
            WeatherForecastResponse forecast = weatherForecastService.getWeatherForFarm(farm.getId(), userDetails);
            if (forecast == null) {
                return WeatherContext.builder()
                        .freshness(WeatherFreshnessStatus.UNAVAILABLE)
                        .temporalScope("CURRENT_ENVIRONMENTAL_CONDITIONS")
                        .statusMessage("No weather forecast returned for farm.")
                        .activeAdvisories(getAdvisoriesForPlanting(farm.getId(), plantingId))
                        .build();
            }

            WeatherFreshnessStatus freshness = forecast.isStale() ? WeatherFreshnessStatus.STALE : WeatherFreshnessStatus.FRESH;
            String statusMsg = forecast.isStale()
                    ? "Serving stale cached weather forecast"
                    : "Fresh weather forecast available";

            if (targetDate != null && !targetDate.equals(LocalDate.now())) {
                statusMsg += String.format(". Note: Environmental observations and advisories reflect current conditions at evaluation time, not historical conditions on asOfDate (%s).", targetDate);
            }

            WeatherObservationSummary currentObs = mapObservation(forecast.getCurrent());
            List<WeatherAdvisoryResponse> advisories = getAdvisoriesForPlanting(farm.getId(), plantingId);

            return WeatherContext.builder()
                    .freshness(freshness)
                    .temporalScope("CURRENT_ENVIRONMENTAL_CONDITIONS")
                    .fetchedAt(forecast.getFetchedAt())
                    .expiresAt(forecast.getExpiresAt())
                    .current(currentObs)
                    .dailyForecast(forecast.getDaily())
                    .activeAdvisories(advisories)
                    .statusMessage(statusMsg)
                    .attribution(forecast.getAttribution())
                    .attributionUrl(forecast.getAttributionUrl())
                    .build();

        } catch (LocationUnavailableException e) {
            log.info("Location coordinates or district unavailable for farm {}: {}", farm.getId(), e.getMessage());
            return WeatherContext.builder()
                    .freshness(WeatherFreshnessStatus.LOCATION_UNAVAILABLE)
                    .temporalScope("CURRENT_ENVIRONMENTAL_CONDITIONS")
                    .statusMessage(e.getMessage())
                    .activeAdvisories(getAdvisoriesForPlanting(farm.getId(), plantingId))
                    .build();

        } catch (WeatherProviderException e) {
            log.warn("External weather provider failed and no cache available for farm {}: {}", farm.getId(), e.getMessage());
            return WeatherContext.builder()
                    .freshness(WeatherFreshnessStatus.UNAVAILABLE)
                    .temporalScope("CURRENT_ENVIRONMENTAL_CONDITIONS")
                    .statusMessage("External weather provider is currently unavailable and no cached forecast exists.")
                    .activeAdvisories(getAdvisoriesForPlanting(farm.getId(), plantingId))
                    .build();

        } catch (org.springframework.dao.DataAccessException e) {
            log.error("Database error while accessing weather cache or advisories for farm {}: {}", farm.getId(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.warn("Unexpected error resolving weather for farm {}: {}", farm.getId(), e.getMessage());
            return WeatherContext.builder()
                    .freshness(WeatherFreshnessStatus.UNAVAILABLE)
                    .temporalScope("CURRENT_ENVIRONMENTAL_CONDITIONS")
                    .statusMessage("Weather service encountered an error: " + e.getMessage())
                    .activeAdvisories(getAdvisoriesForPlanting(farm.getId(), plantingId))
                    .build();
        }
    }

    private CropIdentityContext buildCropIdentityContext(CropPlanting planting, LocalDate targetDate) {
        Long elapsedDays = null;
        if (planting.getPlantingDate() != null) {
            elapsedDays = ChronoUnit.DAYS.between(planting.getPlantingDate().toLocalDate(), targetDate);
        }

        return CropIdentityContext.builder()
                .plantingId(planting.getId())
                .cropCycleId(planting.getCropCycle() != null ? planting.getCropCycle().getId() : null)
                .cropName(planting.getCropName())
                .cropVariety(planting.getCropVariety())
                .plantingDate(planting.getPlantingDate())
                .expectedHarvestDate(planting.getExpectedHarvestDate())
                .elapsedDays(elapsedDays)
                .build();
    }

    private GrowthStageContext buildGrowthStageContext(CropPlanting planting, LocalDate targetDate) {
        EstimatedGrowthStageResponse estimatedStage = agronomicStageResolverService.resolveGrowthStage(planting, targetDate);

        return GrowthStageContext.builder()
                .estimatedStage(estimatedStage)
                .observedStage(null) // Farmer-observed biological stage is currently not confirmed
                .agronomicBasis("CALENDAR_DAY_MODEL_ESTIMATE")
                .stageDiscrepancyNotice("Estimated growth stage is derived mathematically from ICAR standard calendar-day offsets. Field biological growth stage has not been confirmed by farmer observation.")
                .build();
    }

    private LocationContext buildLocationContext(Farm farm) {
        Double latitude = farm.getLatitude();
        Double longitude = farm.getLongitude();
        CoordinateSource coordinateSource = (latitude != null && longitude != null) ? CoordinateSource.FARM_GPS : null;
        String locationLabel = farm.getLocation();

        try {
            ResolvedLocation resolved = districtCentroidResolver.resolveLocation(farm);
            if (resolved != null) {
                latitude = resolved.getLatitude();
                longitude = resolved.getLongitude();
                coordinateSource = resolved.getCoordinateSource();
                locationLabel = resolved.getLocationLabel();
            }
        } catch (LocationUnavailableException e) {
            // Retain raw farm location without coordinates if centroid resolution fails
            if (locationLabel == null || locationLabel.trim().isEmpty()) {
                locationLabel = "Location Unavailable";
            }
        }

        return LocationContext.builder()
                .farmId(farm.getId())
                .farmName(farm.getName())
                .locationAddress(farm.getLocation())
                .state(farm.getState())
                .district(farm.getDistrict())
                .latitude(latitude)
                .longitude(longitude)
                .coordinateSource(coordinateSource)
                .locationLabel(locationLabel)
                .plotId(null) // Optional plot is not configured on farm
                .plotName(null)
                .build();
    }

    private CultivationPlanSummaryContext buildActivePlanSummary(UUID plantingId) {
        return planRepository
                .findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE)
                .map(plan -> {
                    int totalStages = (plan.getTemplate() != null && plan.getTemplate().getStages() != null)
                            ? plan.getTemplate().getStages().size() : 0;
                    int totalTasks = taskRepository.findByCultivationPlanId(plan.getId()).size();

                    return CultivationPlanSummaryContext.builder()
                            .planId(plan.getId())
                            .revisionNumber(plan.getPlanVersion())
                            .status(plan.getStatus().name())
                            .cropName(plan.getTemplate() != null ? plan.getTemplate().getCropName() : null)
                            .cropVariety(plan.getTemplate() != null ? plan.getTemplate().getCropVariety() : null)
                            .packageSource(plan.getTemplate() != null ? plan.getTemplate().getSource() : null)
                            .packageVersion(plan.getTemplate() != null ? plan.getTemplate().getVersion() : null)
                            .durationDays(plan.getTemplate() != null ? plan.getTemplate().getDurationDays() : null)
                            .totalStages(totalStages)
                            .totalTasks(totalTasks)
                            .build();
                })
                .orElse(null);
    }

    private List<OperationalTaskContext> buildRelevantTasks(UUID plantingId, LocalDate targetDate) {
        List<Task> allTasks = taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId);

        return allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING
                        || task.getStatus() == TaskStatus.IN_PROGRESS
                        || task.getStatus() == TaskStatus.OVERDUE)
                .sorted(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(task -> {
                    LocalDate dueDate = task.getDueDate() != null ? task.getDueDate().toLocalDate() : null;
                    boolean isOverdue = task.getStatus() == TaskStatus.OVERDUE
                            || (dueDate != null && dueDate.isBefore(targetDate));

                    Integer daysOverdue = null;
                    if (isOverdue && dueDate != null) {
                        daysOverdue = Math.max(1, (int) ChronoUnit.DAYS.between(dueDate, targetDate));
                    }

                    String stageName = null;
                    Integer stageOrder = null;
                    if (task.getTemplateTask() != null && task.getTemplateTask().getStage() != null) {
                        stageName = task.getTemplateTask().getStage().getStageName();
                        stageOrder = task.getTemplateTask().getStage().getStageOrder();
                    }

                    return OperationalTaskContext.builder()
                            .taskId(task.getId())
                            .title(task.getTitle())
                            .taskType(task.getTaskType() != null ? task.getTaskType().name() : null)
                            .status(task.getStatus())
                            .dueDate(dueDate)
                            .overdue(isOverdue)
                            .daysOverdue(daysOverdue)
                            .stageName(stageName)
                            .stageOrder(stageOrder)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<WeatherAdvisoryResponse> getAdvisoriesForPlanting(UUID farmId, UUID plantingId) {
        List<WeatherAdvisory> advisories = advisoryRepository.findByFarmIdAndStatusInOrderByTargetDateAsc(
                farmId, List.of(AdvisoryStatus.ACTIVE, AdvisoryStatus.ACKNOWLEDGED)
        );

        return advisories.stream()
                .filter(advisory -> advisory.getCropPlanting() == null || advisory.getCropPlanting().getId().equals(plantingId))
                .map(weatherForecastService::mapToAdvisoryResponse)
                .collect(Collectors.toList());
    }

    private WeatherObservationSummary mapObservation(WeatherObservation obs) {
        if (obs == null) {
            return null;
        }
        return WeatherObservationSummary.builder()
                .temperatureCelsius(obs.getTemperatureCelsius())
                .relativeHumidityPercent(obs.getRelativeHumidity() != null ? obs.getRelativeHumidity().doubleValue() : null)
                .precipitationMm(obs.getPrecipitationMm())
                .windSpeedKmh(obs.getWindSpeedKmh())
                .weatherCode(obs.getWeatherCode())
                .weatherDescription(obs.getWeatherCondition())
                .build();
    }

    private void validateFarmerRole(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Access denied. User is not authenticated.");
        }
        boolean isFarmer = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_FARMER"));
        if (!isFarmer) {
            throw new AccessDeniedException("Access denied. Only farmers are allowed to access this resource.");
        }
    }

    private void validateCycleOwnership(CropCycle cropCycle, UUID userId) {
        if (!cropCycle.getFarm().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this crop cycle.");
        }
    }
}
