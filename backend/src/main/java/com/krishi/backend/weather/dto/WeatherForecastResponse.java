package com.krishi.backend.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecastResponse {
    private UUID farmId;
    private Double latitude;
    private Double longitude;
    private CoordinateSource coordinateSource;
    private String locationLabel;
    private boolean stale;
    private OffsetDateTime fetchedAt;
    private OffsetDateTime expiresAt;
    private WeatherObservation current;
    private List<DailyForecastItem> daily;
    private List<HourlyForecastItem> hourly;
    private String attribution;
    private String attributionUrl;
}
