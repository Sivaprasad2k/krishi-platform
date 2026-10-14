package com.krishi.backend.advisory.dto;

import com.krishi.backend.advisory.entity.AdvisoryStatus;
import com.krishi.backend.weather.entity.AdvisorySeverity;
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
public class AgriculturalAdvisoryResponse {
    private UUID id;
    private String advisoryFingerprint;
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
    private String provenance;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastEvaluatedAt;
    private OffsetDateTime acknowledgedAt;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime expiresAt;
}
