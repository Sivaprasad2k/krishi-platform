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
 * Rule identifying overdue cultivation tasks and explaining the agronomic impact
 * relative to the current crop growth stage.
 * Does not create duplicate tasks or duplicate notifications.
 */
@Slf4j
@Component
public class OverdueCultivationTaskRule implements AgriculturalRule {

    public static final String RULE_CODE = "OVERDUE_CULTIVATION_TASK";
    public static final String RULE_NAME = "Overdue Cultivation Task Operational Alert";
    public static final String PROVENANCE = "Krishi Cultivation Operations Standard (Operational tasks overdue >= 1 day)";

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

        LocalDate evaluationDate = context.getAsOfDate() != null ? context.getAsOfDate() : LocalDate.now();
        List<OperationalTaskContext> overdueTasks = getOverdueTasks(context, evaluationDate);

        if (overdueTasks.isEmpty()) {
            return RuleEvaluationResult.notApplicable(RULE_CODE, "No overdue cultivation tasks found for crop planting");
        }

        UUID farmId = context.getLocation() != null ? context.getLocation().getFarmId() : null;
        UUID plantingId = context.getCrop() != null ? context.getCrop().getPlantingId() : null;
        String cropName = (context.getCrop() != null && context.getCrop().getCropName() != null)
                ? context.getCrop().getCropName() : "crop";

        String stageContextStr = "during the current cultivation cycle.";
        if (context.getGrowthStage() != null && context.getGrowthStage().getEstimatedStage() != null) {
            EstimatedGrowthStageResponse stage = context.getGrowthStage().getEstimatedStage();
            if (stage.getStatus() == GrowthStageStatus.RESOLVED && stage.getStageName() != null) {
                stageContextStr = String.format("during the '%s' growth stage.", stage.getStageName());
            }
        }

        List<AdvisoryProposal> proposals = new ArrayList<>();

        for (OperationalTaskContext task : overdueTasks) {
            int daysOverdue = task.getDaysOverdue() != null ? task.getDaysOverdue() : 1;
            boolean isCritical = isCriticalTask(task);

            AdvisorySeverity severity = (daysOverdue >= 5 || isCritical)
                    ? AdvisorySeverity.HIGH : AdvisorySeverity.MEDIUM;

            LocalDate targetDate = task.getDueDate() != null ? task.getDueDate() : evaluationDate;
            String fingerprint = buildFingerprint(farmId, plantingId, task.getTaskId(), RULE_CODE, targetDate);

            proposals.add(AdvisoryProposal.builder()
                    .fingerprint(fingerprint)
                    .ruleCode(RULE_CODE)
                    .severity(severity)
                    .title("Overdue Task Alert — " + task.getTitle())
                    .message(String.format("Task '%s' for %s is overdue by %d day(s) %s Operational delays compromise crop vigor and final yield potential.",
                            task.getTitle(), cropName, daysOverdue, stageContextStr))
                    .recommendedAction("Complete this task promptly, reschedule with an updated due date, or skip the task with an explicit reason if the operational window has passed.")
                    .targetDate(targetDate)
                    .taskId(task.getTaskId())
                    .provenance(PROVENANCE)
                    .expiresAt(evaluationDate.plusDays(3).atStartOfDay().atOffset(ZoneOffset.UTC))
                    .build());
        }

        return RuleEvaluationResult.success(RULE_CODE,
                String.format("Generated %d overdue task advisory proposal(s)", proposals.size()),
                proposals);
    }

    private List<OperationalTaskContext> getOverdueTasks(AgriculturalContextResponse context, LocalDate evaluationDate) {
        if (context.getRelevantTasks() == null) {
            return Collections.emptyList();
        }
        return context.getRelevantTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING
                        || task.getStatus() == TaskStatus.IN_PROGRESS
                        || task.getStatus() == TaskStatus.OVERDUE)
                .filter(task -> task.isOverdue()
                        || task.getStatus() == TaskStatus.OVERDUE
                        || (task.getDueDate() != null && task.getDueDate().isBefore(evaluationDate)))
                .collect(Collectors.toList());
    }

    private boolean isCriticalTask(OperationalTaskContext task) {
        if (task.getTaskType() != null) {
            String type = task.getTaskType().toUpperCase();
            return type.equals("IRRIGATION") || type.equals("DISEASE_TREATMENT") || type.equals("PEST_CONTROL");
        }
        String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
        return title.contains("irrigation") || title.contains("treatment") || title.contains("spray")
                || title.contains("disease");
    }

    private String buildFingerprint(UUID farmId, UUID plantingId, UUID taskId, String ruleCode, LocalDate targetDate) {
        String fId = farmId != null ? farmId.toString() : "GLOBAL";
        String pId = plantingId != null ? plantingId.toString() : "GLOBAL";
        String tId = taskId != null ? taskId.toString() : "GLOBAL";
        return fId + ":" + pId + ":" + tId + ":" + ruleCode + ":" + targetDate;
    }
}
