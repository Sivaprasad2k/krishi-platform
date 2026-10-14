package com.krishi.backend.advisory.rule.impl;

import com.krishi.backend.advisory.rule.AdvisoryProposal;
import com.krishi.backend.advisory.rule.AgriculturalRule;
import com.krishi.backend.advisory.rule.RuleEvaluationResult;
import com.krishi.backend.cropplanting.dto.context.AgriculturalContextResponse;
import com.krishi.backend.cropplanting.dto.context.OperationalTaskContext;
import com.krishi.backend.cropplanting.dto.context.WeatherContext;
import com.krishi.backend.cropplanting.dto.context.WeatherFreshnessStatus;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.weather.dto.DailyForecastItem;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule evaluating rainfall risk for scheduled irrigation tasks.
 * Evaluates forecast freshness, precipitation amounts, and probabilities.
 * A missing or stale weather forecast is explicitly treated as INSUFFICIENT_CONTEXT,
 * never as evidence that conditions are safe.
 */
@Slf4j
@Component
public class IrrigationRainfallConflictRule implements AgriculturalRule {

    public static final String RULE_CODE = "IRRIGATION_RAINFALL_CONFLICT";
    public static final String RULE_NAME = "Rainfall Risk for Scheduled Irrigation";

    public static final double PRECIPITATION_TRIGGER_MM = 20.0;
    public static final int PRECIPITATION_PROBABILITY_TRIGGER = 80;
    public static final double HIGH_SEVERITY_PRECIPITATION_MM = 50.0;
    public static final String PROVENANCE = "Standard ICAR & Agricultural Water Management Guidelines (Irrigation withheld when precipitation >= 20mm or probability >= 80%)";

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

        List<OperationalTaskContext> activeIrrigationTasks = getActiveIrrigationTasks(context);
        if (activeIrrigationTasks.isEmpty()) {
            return RuleEvaluationResult.notApplicable(RULE_CODE, "No active or scheduled irrigation tasks for crop planting");
        }

        WeatherContext weather = context.getWeather();
        if (weather == null) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Weather context is missing; cannot verify rainfall safety");
        }

        if (weather.getFreshness() == null
                || weather.getFreshness() == WeatherFreshnessStatus.UNAVAILABLE
                || weather.getFreshness() == WeatherFreshnessStatus.LOCATION_UNAVAILABLE) {
            String msg = weather.getStatusMessage() != null ? weather.getStatusMessage() : "Weather forecast is unavailable";
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Weather forecast is unavailable (" + msg + "); cannot verify rainfall safety");
        }

        if (weather.getFreshness() == WeatherFreshnessStatus.STALE) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Weather forecast is stale (cached data expired); cannot safely evaluate rainfall risk");
        }

        List<DailyForecastItem> dailyForecast = weather.getDailyForecast();
        if (dailyForecast == null || dailyForecast.isEmpty()) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Daily weather forecast items are missing from context");
        }

        Map<LocalDate, DailyForecastItem> forecastByDate = dailyForecast.stream()
                .filter(d -> d.getDate() != null)
                .collect(Collectors.toMap(DailyForecastItem::getDate, d -> d, (a, b) -> a));

        UUID farmId = context.getLocation() != null ? context.getLocation().getFarmId() : null;
        UUID plantingId = context.getCrop() != null ? context.getCrop().getPlantingId() : null;
        String cropName = (context.getCrop() != null && context.getCrop().getCropName() != null)
                ? context.getCrop().getCropName() : "crop";

        List<AdvisoryProposal> proposals = new ArrayList<>();
        int tasksBeyondHorizon = 0;

        for (OperationalTaskContext task : activeIrrigationTasks) {
            LocalDate targetDate = task.getDueDate() != null ? task.getDueDate() : context.getAsOfDate();
            if (targetDate == null) {
                targetDate = LocalDate.now();
            }

            DailyForecastItem forecast = forecastByDate.get(targetDate);
            if (forecast == null) {
                tasksBeyondHorizon++;
                continue;
            }

            double precipMm = forecast.getPrecipitationMm() != null ? forecast.getPrecipitationMm() : 0.0;
            int precipProb = forecast.getPrecipitationProbabilityMax() != null ? forecast.getPrecipitationProbabilityMax() : 0;

            if (precipMm >= PRECIPITATION_TRIGGER_MM || precipProb >= PRECIPITATION_PROBABILITY_TRIGGER) {
                AdvisorySeverity severity = (precipMm >= HIGH_SEVERITY_PRECIPITATION_MM)
                        ? AdvisorySeverity.HIGH : AdvisorySeverity.MEDIUM;

                String fingerprint = buildFingerprint(farmId, plantingId, task.getTaskId(), RULE_CODE, targetDate);

                proposals.add(AdvisoryProposal.builder()
                        .fingerprint(fingerprint)
                        .ruleCode(RULE_CODE)
                        .severity(severity)
                        .title("Heavy Rain Forecast — Review Scheduled Irrigation")
                        .message(String.format("Heavy rainfall (%.1f mm, %d%% probability) is forecast on %s for %s. Suspend artificial irrigation to prevent waterlogging, nutrient leaching, and root hypoxia.",
                                precipMm, precipProb, targetDate, cropName))
                        .recommendedAction("Withhold scheduled artificial irrigation until soil moisture drains. Inspect drainage bunds to prevent ponding.")
                        .targetDate(targetDate)
                        .taskId(task.getTaskId())
                        .provenance(PROVENANCE)
                        .expiresAt(targetDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))
                        .build());
            }
        }

        if (!proposals.isEmpty()) {
            return RuleEvaluationResult.success(RULE_CODE,
                    String.format("Generated %d irrigation conflict advisory proposal(s)", proposals.size()),
                    proposals);
        }

        if (tasksBeyondHorizon == activeIrrigationTasks.size()) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE,
                    "All scheduled irrigation tasks have due dates beyond the available weather forecast horizon");
        }

        return RuleEvaluationResult.success(RULE_CODE,
                String.format("Evaluated %d irrigation task(s); forecast rainfall is below risk thresholds", activeIrrigationTasks.size() - tasksBeyondHorizon));
    }

    private List<OperationalTaskContext> getActiveIrrigationTasks(AgriculturalContextResponse context) {
        if (context.getRelevantTasks() == null) {
            return Collections.emptyList();
        }
        return context.getRelevantTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS)
                .filter(this::isIrrigationTask)
                .collect(Collectors.toList());
    }

    private boolean isIrrigationTask(OperationalTaskContext task) {
        if ("IRRIGATION".equalsIgnoreCase(task.getTaskType())) {
            return true;
        }
        String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
        return title.contains("irrigation") || title.contains("watering") || title.contains("submergence");
    }

    private String buildFingerprint(UUID farmId, UUID plantingId, UUID taskId, String ruleCode, LocalDate targetDate) {
        String fId = farmId != null ? farmId.toString() : "GLOBAL";
        String pId = plantingId != null ? plantingId.toString() : "GLOBAL";
        String tId = taskId != null ? taskId.toString() : "GLOBAL";
        return fId + ":" + pId + ":" + tId + ":" + ruleCode + ":" + targetDate;
    }
}
