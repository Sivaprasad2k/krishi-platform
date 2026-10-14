package com.krishi.backend.advisory.service;

import com.krishi.backend.advisory.rule.AdvisoryProposal;
import com.krishi.backend.advisory.rule.AgriculturalRule;
import com.krishi.backend.advisory.rule.RuleEvaluationOutcome;
import com.krishi.backend.advisory.rule.RuleEvaluationResult;
import com.krishi.backend.cropplanting.dto.context.AgriculturalContextResponse;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Agricultural Advisory Engine Unit Tests")
class AgriculturalAdvisoryEngineTest {

    @Test
    @DisplayName("Should evaluate all rules and collect proposals when all rules succeed")
    void testEngineAggregatesProposals() {
        AgriculturalRule rule1 = mock(AgriculturalRule.class);
        when(rule1.getRuleCode()).thenReturn("RULE_1");
        when(rule1.getRuleName()).thenReturn("Rule One");
        when(rule1.evaluate(any())).thenReturn(RuleEvaluationResult.success("RULE_1", "All clear"));

        AgriculturalRule rule2 = mock(AgriculturalRule.class);
        when(rule2.getRuleCode()).thenReturn("RULE_2");
        when(rule2.getRuleName()).thenReturn("Rule Two");
        AdvisoryProposal proposal = AdvisoryProposal.builder()
                .fingerprint("fp:2")
                .ruleCode("RULE_2")
                .severity(AdvisorySeverity.MEDIUM)
                .title("Advisory 2")
                .targetDate(LocalDate.now())
                .build();
        when(rule2.evaluate(any())).thenReturn(RuleEvaluationResult.success("RULE_2", "Triggered", List.of(proposal)));

        AgriculturalAdvisoryEngine engine = new AgriculturalAdvisoryEngine(List.of(rule1, rule2));

        AgriculturalContextResponse context = AgriculturalContextResponse.builder().build();
        AgriculturalAdvisoryEngine.EngineExecutionResult result = engine.evaluate(context);

        assertEquals(2, result.getReportItems().size());
        assertEquals(1, result.getProposals().size());
        assertEquals("RULE_2", result.getProposals().get(0).getRuleCode());
    }

    @Test
    @DisplayName("Should isolate rule failure so that throwing rule does not abort execution of other rules")
    void testIsolatedRuleFailureHandling() {
        AgriculturalRule failingRule = mock(AgriculturalRule.class);
        when(failingRule.getRuleCode()).thenReturn("FAILING_RULE");
        when(failingRule.getRuleName()).thenReturn("Failing Rule");
        when(failingRule.evaluate(any())).thenThrow(new RuntimeException("Simulated unexpected NullPointer"));

        AgriculturalRule healthyRule = mock(AgriculturalRule.class);
        when(healthyRule.getRuleCode()).thenReturn("HEALTHY_RULE");
        when(healthyRule.getRuleName()).thenReturn("Healthy Rule");
        AdvisoryProposal proposal = AdvisoryProposal.builder()
                .fingerprint("fp:healthy")
                .ruleCode("HEALTHY_RULE")
                .severity(AdvisorySeverity.HIGH)
                .title("Healthy Advisory")
                .targetDate(LocalDate.now())
                .build();
        when(healthyRule.evaluate(any())).thenReturn(RuleEvaluationResult.success("HEALTHY_RULE", "Triggered", List.of(proposal)));

        AgriculturalAdvisoryEngine engine = new AgriculturalAdvisoryEngine(List.of(failingRule, healthyRule));

        AgriculturalContextResponse context = AgriculturalContextResponse.builder().build();
        AgriculturalAdvisoryEngine.EngineExecutionResult result = engine.evaluate(context);

        assertEquals(2, result.getReportItems().size());

        // Failing rule report item
        assertEquals("FAILING_RULE", result.getReportItems().get(0).getRuleCode());
        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_FAILURE, result.getReportItems().get(0).getOutcome());
        assertTrue(result.getReportItems().get(0).getReason().contains("Simulated unexpected NullPointer"));

        // Healthy rule report item and proposal are preserved!
        assertEquals("HEALTHY_RULE", result.getReportItems().get(1).getRuleCode());
        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getReportItems().get(1).getOutcome());
        assertEquals(1, result.getProposals().size());
        assertEquals("HEALTHY_RULE", result.getProposals().get(0).getRuleCode());
    }
}
