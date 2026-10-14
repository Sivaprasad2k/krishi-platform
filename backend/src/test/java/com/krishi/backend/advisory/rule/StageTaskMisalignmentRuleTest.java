package com.krishi.backend.advisory.rule;

import com.krishi.backend.advisory.rule.impl.StageTaskMisalignmentRule;
import com.krishi.backend.cropplanting.dto.context.*;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import com.krishi.backend.cultivation.dto.GrowthStageStatus;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stage Task Misalignment Rule Unit Tests")
class StageTaskMisalignmentRuleTest {

    private StageTaskMisalignmentRule rule;
    private UUID farmId;
    private UUID plantingId;
    private UUID taskId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        rule = new StageTaskMisalignmentRule();
        farmId = UUID.randomUUID();
        plantingId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        today = LocalDate.now();
    }

    @Test
    @DisplayName("Should generate LOW severity advisory when active pending task belongs to an earlier stage than current RESOLVED stage")
    void testPriorStageTaskTriggersAdvisory() {
        // Current stage is Stage 3 (Panicle Initiation)
        EstimatedGrowthStageResponse currentStage = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.RESOLVED)
                .stageName("Panicle Initiation & Booting")
                .stageOrder(3)
                .totalStages(5)
                .build();

        // Pending task belongs to Stage 1 (Nursery & Seedbed Preparation)
        OperationalTaskContext priorStageTask = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Prepare Nursery Seedbed")
                .taskType("LAND_PREPARATION")
                .status(TaskStatus.PENDING)
                .stageName("Nursery & Seedbed Preparation")
                .stageOrder(1)
                .dueDate(today.minusDays(20))
                .build();

        AgriculturalContextResponse context = AgriculturalContextResponse.builder()
                .asOfDate(today)
                .crop(CropIdentityContext.builder().plantingId(plantingId).cropName("Rice - Uma").build())
                .location(LocationContext.builder().farmId(farmId).build())
                .growthStage(GrowthStageContext.builder().estimatedStage(currentStage).build())
                .relevantTasks(List.of(priorStageTask))
                .build();

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(1, result.getProposals().size());

        AdvisoryProposal proposal = result.getProposals().get(0);
        assertEquals(StageTaskMisalignmentRule.RULE_CODE, proposal.getRuleCode());
        assertEquals(AdvisorySeverity.LOW, proposal.getSeverity());
        assertTrue(proposal.getMessage().contains("Stage 1"));
        assertTrue(proposal.getMessage().contains("Stage 3"));
        assertTrue(proposal.getRecommendedAction().contains("skip reason"));
    }

    @Test
    @DisplayName("Should return SUCCESS with no proposals when active tasks are aligned with current stage or in future stages")
    void testAlignedTasksSafe() {
        EstimatedGrowthStageResponse currentStage = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.RESOLVED)
                .stageName("Tillering & Stem Elongation")
                .stageOrder(2)
                .totalStages(5)
                .build();

        OperationalTaskContext alignedTask = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Top Dressing Fertilizer")
                .taskType("FERTILIZER_APPLICATION")
                .status(TaskStatus.PENDING)
                .stageName("Tillering & Stem Elongation")
                .stageOrder(2)
                .dueDate(today.plusDays(2))
                .build();

        AgriculturalContextResponse context = AgriculturalContextResponse.builder()
                .asOfDate(today)
                .crop(CropIdentityContext.builder().plantingId(plantingId).cropName("Rice").build())
                .location(LocationContext.builder().farmId(farmId).build())
                .growthStage(GrowthStageContext.builder().estimatedStage(currentStage).build())
                .relevantTasks(List.of(alignedTask))
                .build();

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertTrue(result.getProposals().isEmpty());
    }

    @Test
    @DisplayName("Should return INSUFFICIENT_CONTEXT when growth stage status is NOT RESOLVED (e.g. PRE_PLANTING)")
    void testUnresolvedGrowthStageReturnsInsufficientContext() {
        EstimatedGrowthStageResponse currentStage = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.PRE_PLANTING)
                .stageName(null)
                .stageOrder(null)
                .build();

        OperationalTaskContext task = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Seed Soaking")
                .taskType("SEED_TREATMENT")
                .status(TaskStatus.PENDING)
                .stageOrder(1)
                .build();

        AgriculturalContextResponse context = AgriculturalContextResponse.builder()
                .asOfDate(today)
                .growthStage(GrowthStageContext.builder().estimatedStage(currentStage).build())
                .relevantTasks(List.of(task))
                .build();

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.INSUFFICIENT_CONTEXT, result.getOutcome());
        assertTrue(result.getProposals().isEmpty());
        assertTrue(result.getReason().contains("PRE_PLANTING"));
    }

    @Test
    @DisplayName("Should return INSUFFICIENT_CONTEXT when growth stage status is NO_ACTIVE_PLAN")
    void testNoActivePlanReturnsInsufficientContext() {
        EstimatedGrowthStageResponse currentStage = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.NO_ACTIVE_PLAN)
                .build();

        AgriculturalContextResponse context = AgriculturalContextResponse.builder()
                .asOfDate(today)
                .growthStage(GrowthStageContext.builder().estimatedStage(currentStage).build())
                .relevantTasks(Collections.emptyList())
                .build();

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.INSUFFICIENT_CONTEXT, result.getOutcome());
    }

    @Test
    @DisplayName("Should return NOT_APPLICABLE when no tasks have stage linkage")
    void testNoStageLinkedTasks() {
        EstimatedGrowthStageResponse currentStage = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.RESOLVED)
                .stageName("Flowering")
                .stageOrder(4)
                .build();

        OperationalTaskContext task = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("General Fence Repair")
                .taskType("OTHER")
                .status(TaskStatus.PENDING)
                .stageOrder(null)
                .build();

        AgriculturalContextResponse context = AgriculturalContextResponse.builder()
                .asOfDate(today)
                .growthStage(GrowthStageContext.builder().estimatedStage(currentStage).build())
                .relevantTasks(List.of(task))
                .build();

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_NOT_APPLICABLE, result.getOutcome());
    }
}
