package com.krishi.backend.cropplanting.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgriculturalContextResponse {
    private LocalDate asOfDate;
    private OffsetDateTime evaluatedAt;
    private CropIdentityContext crop;
    private GrowthStageContext growthStage;
    private LocationContext location;
    private CultivationPlanSummaryContext activePlan;
    private List<OperationalTaskContext> relevantTasks;
    private WeatherContext weather;
}
