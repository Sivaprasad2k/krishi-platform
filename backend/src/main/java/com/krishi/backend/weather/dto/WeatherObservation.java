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
public class WeatherObservation {
    private Double temperatureCelsius;
    private Integer relativeHumidity;
    private Integer precipitationProbability;
    private Double precipitationMm;
    private Double windSpeedKmh;
    private Double windGustsKmh;
    private Integer weatherCode;
    private String weatherCondition;
    private OffsetDateTime observedAt;
}
