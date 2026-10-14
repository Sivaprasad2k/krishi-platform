package com.krishi.backend.cropplanting.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservedGrowthStageContext {
    private String stageCode;
    private String stageName;
    private LocalDate observationDate;
    private String observedBy;
    private String notes;
}
