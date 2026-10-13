package com.krishi.backend.weather.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "weather.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherSchedulingConfig {
}
