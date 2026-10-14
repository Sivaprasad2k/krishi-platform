package com.krishi.backend.cropplanting.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherObservationSummary {
    private Double temperatureCelsius;
    private Double relativeHumidityPercent;
    private Double precipitationMm;
    private Double windSpeedKmh;
    private Integer weatherCode;
    private String weatherDescription;
}
