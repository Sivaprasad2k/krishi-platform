package com.krishi.backend.weather.dto;

import com.krishi.backend.weather.entity.AdvisorySeverity;
import com.krishi.backend.weather.entity.AdvisoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAdvisoryResponse {
    private UUID id;
    private UUID farmId;
    private UUID cropPlantingId;
    private String cropName;
    private UUID taskId;
    private String taskTitle;
    private String ruleCode;
    private AdvisorySeverity severity;
    private String title;
    private String message;
    private String recommendedAction;
    private LocalDate targetDate;
    private AdvisoryStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime acknowledgedAt;
}
