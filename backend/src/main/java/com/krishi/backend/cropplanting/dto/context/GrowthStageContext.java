package com.krishi.backend.cropplanting.dto.context;

import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthStageContext {
    private EstimatedGrowthStageResponse estimatedStage;
    private ObservedGrowthStageContext observedStage;
    private String agronomicBasis;
    private String stageDiscrepancyNotice;
}
