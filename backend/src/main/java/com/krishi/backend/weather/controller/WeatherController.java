package com.krishi.backend.weather.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.weather.dto.WeatherAdvisoryResponse;
import com.krishi.backend.weather.dto.WeatherForecastResponse;
import com.krishi.backend.weather.service.WeatherForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherForecastService weatherForecastService;

    @GetMapping("/api/v1/farms/{farmId}/weather")
    public ResponseEntity<ApiResponse<WeatherForecastResponse>> getWeather(
            @PathVariable UUID farmId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        WeatherForecastResponse response = weatherForecastService.getWeatherForFarm(farmId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<WeatherForecastResponse>builder()
                        .success(true)
                        .message("Weather forecast retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/farms/{farmId}/weather/refresh")
    public ResponseEntity<ApiResponse<WeatherForecastResponse>> refreshWeather(
            @PathVariable UUID farmId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        WeatherForecastResponse response = weatherForecastService.refreshWeatherForFarm(farmId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<WeatherForecastResponse>builder()
                        .success(true)
                        .message("Weather forecast refreshed successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/farms/{farmId}/advisories")
    public ResponseEntity<ApiResponse<List<WeatherAdvisoryResponse>>> getFarmAdvisories(
            @PathVariable UUID farmId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<WeatherAdvisoryResponse> response = weatherForecastService.getAdvisoriesForFarm(farmId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<WeatherAdvisoryResponse>>builder()
                        .success(true)
                        .message("Weather advisories retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/advisories/{advisoryId}/acknowledge")
    public ResponseEntity<ApiResponse<WeatherAdvisoryResponse>> acknowledgeAdvisory(
            @PathVariable UUID advisoryId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        WeatherAdvisoryResponse response = weatherForecastService.acknowledgeAdvisory(advisoryId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<WeatherAdvisoryResponse>builder()
                        .success(true)
                        .message("Advisory acknowledged successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/tasks/{taskId}/advisories")
    public ResponseEntity<ApiResponse<List<WeatherAdvisoryResponse>>> getTaskAdvisories(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<WeatherAdvisoryResponse> response = weatherForecastService.getAdvisoriesForTask(taskId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<WeatherAdvisoryResponse>>builder()
                        .success(true)
                        .message("Task weather advisories retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
