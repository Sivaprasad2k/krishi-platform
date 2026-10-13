package com.krishi.backend.weather.provider;

public interface WeatherDataProvider {
    ProviderForecastResult fetchForecast(double latitude, double longitude);
    String getProviderName();
    String getAttribution();
    String getAttributionUrl();
}
