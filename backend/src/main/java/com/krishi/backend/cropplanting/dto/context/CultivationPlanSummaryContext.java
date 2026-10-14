package com.krishi.backend.cropplanting.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CultivationPlanSummaryContext {
    private UUID planId;
    private Integer revisionNumber;
    private String status;
    private String cropName;
    private String cropVariety;
    private String packageSource;
    private String packageVersion;
    private Integer durationDays;
    private Integer totalStages;
    private Integer totalTasks;
}
