package com.krishi.backend.advisory.service;

import com.krishi.backend.advisory.dto.RuleEvaluationReportItem;
import com.krishi.backend.advisory.rule.AdvisoryProposal;
import com.krishi.backend.advisory.rule.AgriculturalRule;
import com.krishi.backend.advisory.rule.RuleEvaluationOutcome;
import com.krishi.backend.advisory.rule.RuleEvaluationResult;
import com.krishi.backend.cropplanting.dto.context.AgriculturalContextResponse;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic rules engine executing configured AgriculturalRules against
 * an established agricultural context snapshot.
 * Isolates rule failures to ensure one failing rule never aborts overall evaluation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgriculturalAdvisoryEngine {

    private final List<AgriculturalRule> rules;

    @Data
    @Builder
    public static class EngineExecutionResult {
        private List<RuleEvaluationReportItem> reportItems;
        private List<AdvisoryProposal> proposals;
    }

    /**
     * Evaluates all configured agricultural advisory rules against the provided agricultural context.
     *
     * @param context the resolved agricultural context
     * @return EngineExecutionResult containing individual rule reports and aggregated proposals
     */
    public EngineExecutionResult evaluate(AgriculturalContextResponse context) {
        List<RuleEvaluationReportItem> reportItems = new ArrayList<>();
        List<AdvisoryProposal> proposals = new ArrayList<>();

        if (rules == null || rules.isEmpty()) {
            log.warn("No AgriculturalRule beans found in application context.");
            return EngineExecutionResult.builder()
                    .reportItems(reportItems)
                    .proposals(proposals)
                    .build();
        }

        for (AgriculturalRule rule : rules) {
            try {
                RuleEvaluationResult result = rule.evaluate(context);
                if (result == null) {
                    result = RuleEvaluationResult.failure(rule.getRuleCode(), "Rule returned null result");
                }

                int proposalCount = result.getProposals() != null ? result.getProposals().size() : 0;
                reportItems.add(RuleEvaluationReportItem.builder()
                        .ruleCode(rule.getRuleCode())
                        .ruleName(rule.getRuleName())
                        .outcome(result.getOutcome())
                        .reason(result.getReason())
                        .proposalsCount(proposalCount)
                        .build());

                if (result.getOutcome() == RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS
                        && result.getProposals() != null) {
                    proposals.addAll(result.getProposals());
                }

            } catch (Exception ex) {
                log.error("Unhandled error evaluating rule [{}] - {}", rule.getRuleCode(), ex.getMessage(), ex);
                reportItems.add(RuleEvaluationReportItem.builder()
                        .ruleCode(rule.getRuleCode())
                        .ruleName(rule.getRuleName())
                        .outcome(RuleEvaluationOutcome.RULE_EVALUATION_FAILURE)
                        .reason("Rule evaluation failed unexpectedly: " + ex.getMessage())
                        .proposalsCount(0)
                        .build());
            }
        }

        return EngineExecutionResult.builder()
                .reportItems(reportItems)
                .proposals(proposals)
                .build();
    }
}
