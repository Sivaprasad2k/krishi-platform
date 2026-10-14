package com.krishi.backend.advisory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisoryEvaluationSummaryResponse {
    private UUID cropPlantingId;
    private OffsetDateTime evaluatedAt;
    private LocalDate asOfDate;
    private int totalRulesEvaluated;
    private int activeAdvisoriesCount;
    private List<RuleEvaluationReportItem> ruleBreakdown;
    private List<AgriculturalAdvisoryResponse> activeAdvisories;
}
