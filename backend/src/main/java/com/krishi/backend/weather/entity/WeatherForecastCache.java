package com.krishi.backend.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "weather_forecast_cache", schema = "krishi")
public class WeatherForecastCache {

    @Id
    private UUID id;

    @Column(name = "grid_key", nullable = false, unique = true, length = 64)
    private String gridKey;

    @Column(name = "normalized_latitude", nullable = false)
    private Double normalizedLatitude;

    @Column(name = "normalized_longitude", nullable = false)
    private Double normalizedLongitude;

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "relative_humidity")
    private Integer relativeHumidity;

    @Column(name = "precipitation_probability")
    private Integer precipitationProbability;

    @Column(name = "precipitation_mm")
    private Double precipitationMm;

    @Column(name = "wind_speed_kmh")
    private Double windSpeedKmh;

    @Column(name = "wind_gusts_kmh")
    private Double windGustsKmh;

    @Column(name = "weather_condition", length = 100)
    private String weatherCondition;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "forecast_payload", nullable = false, columnDefinition = "jsonb")
    private String forecastPayload;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}
