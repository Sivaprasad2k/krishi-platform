package com.krishi.backend.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyForecastItem {
    private OffsetDateTime time;
    private Double temperatureCelsius;
    private Double precipitationMm;
    private Integer precipitationProbability;
    private Double windSpeedKmh;
    private Integer weatherCode;
}
