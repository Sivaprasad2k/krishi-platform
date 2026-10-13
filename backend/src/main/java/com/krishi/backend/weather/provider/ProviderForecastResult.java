package com.krishi.backend.weather.provider;

import com.krishi.backend.weather.dto.DailyForecastItem;
import com.krishi.backend.weather.dto.HourlyForecastItem;
import com.krishi.backend.weather.dto.WeatherObservation;
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
public class ProviderForecastResult {
    private WeatherObservation current;
    private List<DailyForecastItem> daily;
    private List<HourlyForecastItem> hourly;
    private String rawPayloadJson;
    private OffsetDateTime fetchedAt;
}
