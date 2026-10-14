package com.krishi.backend.advisory.rule.impl;

import com.krishi.backend.advisory.rule.AdvisoryProposal;
import com.krishi.backend.advisory.rule.AgriculturalRule;
import com.krishi.backend.advisory.rule.RuleEvaluationResult;
import com.krishi.backend.cropplanting.dto.context.AgriculturalContextResponse;
import com.krishi.backend.cropplanting.dto.context.OperationalTaskContext;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import com.krishi.backend.cultivation.dto.GrowthStageStatus;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule evaluating crop-stage-dependent task relevance.
 * Uses estimated growth stage ONLY when its status is RESOLVED.
 * Never creates false growth-stage warnings when stage estimation is unresolved or insufficient.
 */
@Slf4j
@Component
public class StageTaskMisalignmentRule implements AgriculturalRule {

    public static final String RULE_CODE = "STAGE_TASK_MISALIGNMENT";
    public static final String RULE_NAME = "Crop-Stage Dependent Task Relevance";
    public static final String PROVENANCE = "Certified ICAR Cultivation Plan Phenological Stage Boundaries";

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleEvaluationResult evaluate(AgriculturalContextResponse context) {
        if (context == null) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Agricultural context is null");
        }

        if (context.getGrowthStage() == null || context.getGrowthStage().getEstimatedStage() == null) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Growth stage context is missing");
        }

        EstimatedGrowthStageResponse currentStage = context.getGrowthStage().getEstimatedStage();
        if (currentStage.getStatus() != GrowthStageStatus.RESOLVED) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE,
                    String.format("Estimated growth stage status is %s; stage-dependent task relevance cannot be evaluated unless growth stage is RESOLVED",
                            currentStage.getStatus()));
        }

        Integer currentStageOrder = currentStage.getStageOrder();
        if (currentStageOrder == null) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Stage order is missing from resolved growth stage");
        }

        List<OperationalTaskContext> relevantTasks = context.getRelevantTasks();
        if (relevantTasks == null || relevantTasks.isEmpty()) {
            return RuleEvaluationResult.notApplicable(RULE_CODE, "No operational tasks found for crop planting");
        }

        List<OperationalTaskContext> activeStageTasks = relevantTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS)
                .filter(task -> task.getStageOrder() != null)
                .collect(Collectors.toList());

        if (activeStageTasks.isEmpty()) {
            return RuleEvaluationResult.notApplicable(RULE_CODE, "No active tasks with configured cultivation stage links");
        }

        UUID farmId = context.getLocation() != null ? context.getLocation().getFarmId() : null;
        UUID plantingId = context.getCrop() != null ? context.getCrop().getPlantingId() : null;
        LocalDate evaluationDate = context.getAsOfDate() != null ? context.getAsOfDate() : LocalDate.now();

        List<AdvisoryProposal> proposals = new ArrayList<>();

        for (OperationalTaskContext task : activeStageTasks) {
            if (task.getStageOrder() < currentStageOrder) {
                String taskStageName = task.getStageName() != null ? task.getStageName() : "Stage " + task.getStageOrder();
                String currentStageName = currentStage.getStageName() != null ? currentStage.getStageName() : "Stage " + currentStageOrder;

                LocalDate targetDate = task.getDueDate() != null ? task.getDueDate() : evaluationDate;
                String fingerprint = buildFingerprint(farmId, plantingId, task.getTaskId(), RULE_CODE, targetDate);

                proposals.add(AdvisoryProposal.builder()
                        .fingerprint(fingerprint)
                        .ruleCode(RULE_CODE)
                        .severity(AdvisorySeverity.LOW)
                        .title("Stage Misalignment — Task from Prior Growth Stage")
                        .message(String.format("Task '%s' is assigned to stage '%s' (Stage %d), but the crop has advanced to '%s' (Stage %d). The optimal phenological window for this intervention may have passed.",
                                task.getTitle(), taskStageName, task.getStageOrder(), currentStageName, currentStageOrder))
                        .recommendedAction("Review task relevance with current crop development. If the intervention is no longer agronomically appropriate, mark the task as skipped with an explicit skip reason.")
                        .targetDate(targetDate)
                        .taskId(task.getTaskId())
                        .provenance(PROVENANCE)
                        .expiresAt(evaluationDate.plusDays(7).atStartOfDay().atOffset(ZoneOffset.UTC))
                        .build());
            }
        }

        if (!proposals.isEmpty()) {
            return RuleEvaluationResult.success(RULE_CODE,
                    String.format("Detected %d stage-misaligned pending task(s)", proposals.size()),
                    proposals);
        }

        return RuleEvaluationResult.success(RULE_CODE,
                String.format("All active stage-linked tasks are aligned with or ahead of the current growth stage ('%s')", currentStage.getStageName()));
    }

    private String buildFingerprint(UUID farmId, UUID plantingId, UUID taskId, String ruleCode, LocalDate targetDate) {
        String fId = farmId != null ? farmId.toString() : "GLOBAL";
        String pId = plantingId != null ? plantingId.toString() : "GLOBAL";
        String tId = taskId != null ? taskId.toString() : "GLOBAL";
        return fId + ":" + pId + ":" + tId + ":" + ruleCode + ":" + targetDate;
    }
}
