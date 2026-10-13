package com.krishi.backend.weather.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishi.backend.common.exception.LocationUnavailableException;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.weather.dto.ResolvedLocation;
import com.krishi.backend.weather.entity.WeatherForecastCache;
import com.krishi.backend.weather.provider.ProviderForecastResult;
import com.krishi.backend.weather.provider.WeatherDataProvider;
import com.krishi.backend.weather.provider.WeatherProviderException;
import com.krishi.backend.weather.repository.WeatherForecastCacheRepository;
import com.krishi.backend.weather.service.DistrictCentroidResolver;
import com.krishi.backend.weather.service.WeatherAdvisoryEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "weather.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherSyncScheduler {

    // Stable 64-bit advisory lock ID for weather sync
    private static final long WEATHER_SYNC_LOCK_ID = 884729104857201948L;

    private final DataSource dataSource;
    private final FarmRepository farmRepository;
    private final DistrictCentroidResolver districtCentroidResolver;
    private final WeatherDataProvider weatherDataProvider;
    private final WeatherForecastCacheRepository cacheRepository;
    private final WeatherAdvisoryEngine weatherAdvisoryEngine;
    private final ObjectMapper objectMapper;

    /**
     * Executes scheduled weather sync twice daily (05:00 and 17:00 IST).
     * Pinned to a single JDBC connection to guarantee advisory lock acquisition and release.
     */
    @Scheduled(cron = "${weather.scheduler.cron:0 0 5,17 * * *}")
    public void syncScheduledWeatherForecasts() {
        try (Connection conn = dataSource.getConnection()) {
            boolean lockAcquired = false;

            try (PreparedStatement lockStmt = conn.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                lockStmt.setLong(1, WEATHER_SYNC_LOCK_ID);
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (rs.next()) {
                        lockAcquired = rs.getBoolean(1);
                    }
                }
            }

            if (!lockAcquired) {
                log.info("Another scheduler instance holds the weather sync lock. Skipping scheduled run.");
                return;
            }

            try {
                log.info("Acquired PostgreSQL advisory lock [{}]. Starting weather sync job.", WEATHER_SYNC_LOCK_ID);
                executeWeatherSyncJob();
            } finally {
                try (PreparedStatement unlockStmt = conn.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                    unlockStmt.setLong(1, WEATHER_SYNC_LOCK_ID);
                    try (ResultSet rs = unlockStmt.executeQuery()) {
                        if (rs.next()) {
                            log.info("Released PostgreSQL advisory lock [{}]: status={}", WEATHER_SYNC_LOCK_ID, rs.getBoolean(1));
                        }
                    }
                } catch (SQLException unlockEx) {
                    log.error("Failed to release advisory lock [{}]: {}", WEATHER_SYNC_LOCK_ID, unlockEx.getMessage());
                }
            }

        } catch (SQLException e) {
            log.error("Database connection error during scheduled weather sync: {}", e.getMessage());
        }
    }

    public void executeWeatherSyncJob() {
        List<Farm> allFarms = farmRepository.findAll();
        if (allFarms.isEmpty()) {
            log.info("No farms registered in system. Weather sync complete.");
            return;
        }

        // Group farms by spatial grid key so multiple farms in the same grid share 1 forecast fetch
        Map<String, List<Farm>> farmsByGrid = new HashMap<>();
        Map<String, ResolvedLocation> locationByGrid = new HashMap<>();

        for (Farm farm : allFarms) {
            try {
                ResolvedLocation loc = districtCentroidResolver.resolveLocation(farm);
                farmsByGrid.computeIfAbsent(loc.getGridKey(), k -> new ArrayList<>()).add(farm);
                locationByGrid.putIfAbsent(loc.getGridKey(), loc);
            } catch (LocationUnavailableException e) {
                log.debug("Skipping weather sync for farm {}: location unavailable", farm.getId());
            } catch (Exception e) {
                log.warn("Error resolving location for farm {}: {}", farm.getId(), e.getMessage());
            }
        }

        log.info("Identified {} unique weather grid cells across {} farms", farmsByGrid.size(), allFarms.size());

        for (Map.Entry<String, List<Farm>> entry : farmsByGrid.entrySet()) {
            String gridKey = entry.getKey();
            List<Farm> farmsInGrid = entry.getValue();
            ResolvedLocation location = locationByGrid.get(gridKey);

            try {
                Optional<WeatherForecastCache> cachedOpt = cacheRepository.findByGridKey(gridKey);
                ProviderForecastResult forecastResult;

                // If cache exists and expires > 1 hour in future, reuse it
                if (cachedOpt.isPresent() && cachedOpt.get().getExpiresAt().isAfter(OffsetDateTime.now().plusHours(1))) {
                    forecastResult = objectMapper.readValue(cachedOpt.get().getForecastPayload(), ProviderForecastResult.class);
                } else {
                    forecastResult = weatherDataProvider.fetchForecast(location.getLatitude(), location.getLongitude());
                    saveOrUpdateCache(location, forecastResult);

                    // Throttle 100ms between provider calls to respect rate limits
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }

                // Evaluate advisories for all farms sharing this grid
                for (Farm farm : farmsInGrid) {
                    try {
                        weatherAdvisoryEngine.evaluateAdvisories(farm, forecastResult.getDaily());
                    } catch (Exception advEx) {
                        log.warn("Advisory evaluation failed for farm {}: {}", farm.getId(), advEx.getMessage());
                    }
                }

            } catch (WeatherProviderException e) {
                log.warn("Failed weather sync for grid cell {}: {}", gridKey, e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error syncing grid cell {}: {}", gridKey, e.getMessage());
            }
        }

        // Cleanup expired cache rows
        try {
            cacheRepository.deleteExpired(OffsetDateTime.now().minusDays(1));
        } catch (Exception e) {
            log.warn("Error cleaning up expired weather cache: {}", e.getMessage());
        }

        log.info("Completed scheduled weather forecast sync job.");
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

        return cacheRepository.save(cache);
    }
}
