package com.krishi.backend.weather.provider;

public class WeatherProviderException extends RuntimeException {
    public WeatherProviderException(String message) {
        super(message);
    }

    public WeatherProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
