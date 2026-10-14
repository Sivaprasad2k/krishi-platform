package com.krishi.backend.cropplanting.dto.context;

import com.krishi.backend.weather.dto.DailyForecastItem;
import com.krishi.backend.weather.dto.WeatherAdvisoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherContext {
    private WeatherFreshnessStatus freshness;
    private String temporalScope;
    private OffsetDateTime fetchedAt;
    private OffsetDateTime expiresAt;
    private WeatherObservationSummary current;
    private List<DailyForecastItem> dailyForecast;
    private List<WeatherAdvisoryResponse> activeAdvisories;
    private String statusMessage;
    private String attribution;
    private String attributionUrl;
}
