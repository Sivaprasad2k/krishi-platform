package com.krishi.backend.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyForecastItem {
    private LocalDate date;
    private Double maxTempCelsius;
    private Double minTempCelsius;
    private Double precipitationMm;
    private Integer precipitationProbabilityMax;
    private Double maxWindSpeedKmh;
    private Double maxWindGustsKmh;
    private Integer weatherCode;
    private String weatherCondition;
}
