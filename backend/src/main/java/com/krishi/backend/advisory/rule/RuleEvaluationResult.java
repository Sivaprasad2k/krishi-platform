package com.krishi.backend.advisory.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Result of evaluating an agricultural rule against an agricultural context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvaluationResult {
    private String ruleCode;
    private RuleEvaluationOutcome outcome;
    private String reason;
    @Builder.Default
    private List<AdvisoryProposal> proposals = Collections.emptyList();

    public static RuleEvaluationResult success(String ruleCode, String reason, List<AdvisoryProposal> proposals) {
        return RuleEvaluationResult.builder()
                .ruleCode(ruleCode)
                .outcome(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS)
                .reason(reason)
                .proposals(proposals != null ? proposals : Collections.emptyList())
                .build();
    }

    public static RuleEvaluationResult success(String ruleCode, String reason) {
        return success(ruleCode, reason, Collections.emptyList());
    }

    public static RuleEvaluationResult notApplicable(String ruleCode, String reason) {
        return RuleEvaluationResult.builder()
                .ruleCode(ruleCode)
                .outcome(RuleEvaluationOutcome.RULE_NOT_APPLICABLE)
                .reason(reason)
                .proposals(Collections.emptyList())
                .build();
    }

    public static RuleEvaluationResult insufficientContext(String ruleCode, String reason) {
        return RuleEvaluationResult.builder()
                .ruleCode(ruleCode)
                .outcome(RuleEvaluationOutcome.INSUFFICIENT_CONTEXT)
                .reason(reason)
                .proposals(Collections.emptyList())
                .build();
    }

    public static RuleEvaluationResult failure(String ruleCode, String reason) {
        return RuleEvaluationResult.builder()
                .ruleCode(ruleCode)
                .outcome(RuleEvaluationOutcome.RULE_EVALUATION_FAILURE)
                .reason(reason)
                .proposals(Collections.emptyList())
                .build();
    }
}
