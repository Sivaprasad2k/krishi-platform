package com.krishi.backend.advisory.dto;

import com.krishi.backend.advisory.rule.RuleEvaluationOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvaluationReportItem {
    private String ruleCode;
    private String ruleName;
    private RuleEvaluationOutcome outcome;
    private String reason;
    private int proposalsCount;
}
