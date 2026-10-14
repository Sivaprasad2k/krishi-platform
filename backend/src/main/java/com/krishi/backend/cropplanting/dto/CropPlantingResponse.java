package com.krishi.backend.cropplanting.dto;

import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CropPlantingResponse {
    private UUID id;
    private UUID cropCycleId;
    private String cropName;
    private String cropVariety;
    private LocalDateTime plantingDate;
    private LocalDateTime expectedHarvestDate;
    private EstimatedGrowthStageResponse estimatedGrowthStage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
