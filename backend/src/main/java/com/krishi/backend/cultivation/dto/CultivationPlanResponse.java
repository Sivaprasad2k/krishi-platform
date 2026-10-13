package com.krishi.backend.cultivation.dto;

import com.krishi.backend.cultivation.entity.CultivationPlanStatus;
import com.krishi.backend.task.dto.TaskResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CultivationPlanResponse {
    private UUID id;
    private UUID cropPlantingId;
    private UUID templateId;
    private String templateCropName;
    private String templateCropVariety;
    private String templateVersion;
    private String templateSource;
    private Integer durationDays;
    private Integer planVersion;
    private CultivationPlanStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
    private List<CultivationStageResponse> stages;
    private List<TaskResponse> tasks;
}
