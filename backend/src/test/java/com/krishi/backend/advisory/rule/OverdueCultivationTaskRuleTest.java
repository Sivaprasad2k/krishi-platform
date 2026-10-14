package com.krishi.backend.advisory.rule;

import com.krishi.backend.advisory.rule.impl.OverdueCultivationTaskRule;
import com.krishi.backend.cropplanting.dto.context.*;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import com.krishi.backend.cultivation.dto.GrowthStageStatus;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Overdue Cultivation Task Rule Unit Tests")
class OverdueCultivationTaskRuleTest {

    private OverdueCultivationTaskRule rule;
    private UUID farmId;
    private UUID plantingId;
    private UUID taskId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        rule = new OverdueCultivationTaskRule();
        farmId = UUID.randomUUID();
        plantingId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        today = LocalDate.now();
    }

    @Test
    @DisplayName("Should generate HIGH severity advisory when task is overdue by >= 5 days")
    void testOverdueByFiveDaysHighSeverity() {
        OperationalTaskContext task = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Weeding Row 3")
                .taskType("WEEDING")
                .status(TaskStatus.PENDING)
                .dueDate(today.minusDays(6))
                .overdue(true)
                .daysOverdue(6)
                .build();

        AgriculturalContextResponse context = buildContext(List.of(task), "Vegetative Tillering", GrowthStageStatus.RESOLVED);

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(1, result.getProposals().size());

        AdvisoryProposal proposal = result.getProposals().get(0);
        assertEquals(OverdueCultivationTaskRule.RULE_CODE, proposal.getRuleCode());
        assertEquals(AdvisorySeverity.HIGH, proposal.getSeverity());
        assertTrue(proposal.getMessage().contains("overdue by 6 day(s)"));
        assertTrue(proposal.getMessage().contains("Vegetative Tillering"));
    }

    @Test
    @DisplayName("Should generate HIGH severity advisory for critical tasks (irrigation) even if overdue by < 5 days")
    void testCriticalTaskOverdueHighSeverity() {
        OperationalTaskContext task = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Urgent Nursery Irrigation")
                .taskType("IRRIGATION")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(today.minusDays(2))
                .overdue(true)
                .daysOverdue(2)
                .build();

        AgriculturalContextResponse context = buildContext(List.of(task), "Germination", GrowthStageStatus.RESOLVED);

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(1, result.getProposals().size());
        assertEquals(AdvisorySeverity.HIGH, result.getProposals().get(0).getSeverity());
    }

    @Test
    @DisplayName("Should generate MEDIUM severity advisory for regular tasks overdue by 1-4 days")
    void testRegularTaskModerateDelayMediumSeverity() {
        OperationalTaskContext task = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Light Mulching")
                .taskType("MULCHING")
                .status(TaskStatus.PENDING)
                .dueDate(today.minusDays(2))
                .overdue(true)
                .daysOverdue(2)
                .build();

        AgriculturalContextResponse context = buildContext(List.of(task), "Vegetative", GrowthStageStatus.RESOLVED);

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(AdvisorySeverity.MEDIUM, result.getProposals().get(0).getSeverity());
    }

    @Test
    @DisplayName("Should return NOT_APPLICABLE when no tasks are overdue")
    void testNoOverdueTasks() {
        OperationalTaskContext task = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Future Harvest")
                .taskType("HARVESTING")
                .status(TaskStatus.PENDING)
                .dueDate(today.plusDays(10))
                .overdue(false)
                .daysOverdue(null)
                .build();

        AgriculturalContextResponse context = buildContext(List.of(task), "Maturation", GrowthStageStatus.RESOLVED);

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_NOT_APPLICABLE, result.getOutcome());
        assertTrue(result.getProposals().isEmpty());
    }

    @Test
    @DisplayName("Should gracefully handle unresolved growth stage without crashing")
    void testUnresolvedGrowthStageFallback() {
        OperationalTaskContext task = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Late Task")
                .taskType("OTHER")
                .status(TaskStatus.OVERDUE)
                .dueDate(today.minusDays(3))
                .overdue(true)
                .daysOverdue(3)
                .build();

        AgriculturalContextResponse context = buildContext(List.of(task), null, GrowthStageStatus.NO_ACTIVE_PLAN);

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertTrue(result.getProposals().get(0).getMessage().contains("current cultivation cycle"));
    }

    private AgriculturalContextResponse buildContext(
            List<OperationalTaskContext> tasks,
            String stageName,
            GrowthStageStatus stageStatus
    ) {
        EstimatedGrowthStageResponse stage = EstimatedGrowthStageResponse.builder()
                .status(stageStatus)
                .stageName(stageName)
                .stageOrder(1)
                .build();

        return AgriculturalContextResponse.builder()
                .asOfDate(today)
                .crop(CropIdentityContext.builder().plantingId(plantingId).cropName("Paddy").build())
                .location(LocationContext.builder().farmId(farmId).build())
                .relevantTasks(tasks)
                .growthStage(GrowthStageContext.builder().estimatedStage(stage).build())
                .build();
    }
}
