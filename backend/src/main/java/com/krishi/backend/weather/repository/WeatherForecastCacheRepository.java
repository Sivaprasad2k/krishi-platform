package com.krishi.backend.weather.repository;

import com.krishi.backend.weather.entity.WeatherForecastCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeatherForecastCacheRepository extends JpaRepository<WeatherForecastCache, UUID> {

    Optional<WeatherForecastCache> findByGridKey(String gridKey);

    @Modifying
    @Query("DELETE FROM WeatherForecastCache c WHERE c.expiresAt < :cutoff")
    void deleteExpired(@Param("cutoff") OffsetDateTime cutoff);
}
