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
 * Rule evaluating chemical drift and inhalation risk for scheduled spraying or treatment tasks.
 * Uses explicitly documented provisional thresholds.
 * Missing or stale weather data returns INSUFFICIENT_CONTEXT.
 */
@Slf4j
@Component
public class ChemicalSprayingWindRiskRule implements AgriculturalRule {

    public static final String RULE_CODE = "CHEMICAL_SPRAYING_WIND_RISK";
    public static final String RULE_NAME = "High-Wind Risk for Scheduled Chemical Spraying";

    public static final double SUSTAINED_WIND_TRIGGER_KMH = 20.0;
    public static final double GUST_TRIGGER_KMH = 30.0;
    public static final double HIGH_SEVERITY_WIND_KMH = 35.0;
    public static final double HIGH_SEVERITY_GUST_KMH = 45.0;

    public static final String PROVENANCE = "Provisional threshold based on FAO/CABI Agrochemical Spray Drift Guidelines (applications prohibited when wind > 20 km/h or gusts > 30 km/h)";

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

        List<OperationalTaskContext> activeSprayingTasks = getActiveSprayingTasks(context);
        if (activeSprayingTasks.isEmpty()) {
            return RuleEvaluationResult.notApplicable(RULE_CODE, "No active spraying, top-dressing, or chemical treatment tasks scheduled for crop planting");
        }

        WeatherContext weather = context.getWeather();
        if (weather == null) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Weather context is missing; cannot verify wind drift safety");
        }

        if (weather.getFreshness() == null
                || weather.getFreshness() == WeatherFreshnessStatus.UNAVAILABLE
                || weather.getFreshness() == WeatherFreshnessStatus.LOCATION_UNAVAILABLE) {
            String msg = weather.getStatusMessage() != null ? weather.getStatusMessage() : "Weather forecast is unavailable";
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Weather forecast is unavailable (" + msg + "); cannot verify wind drift safety");
        }

        if (weather.getFreshness() == WeatherFreshnessStatus.STALE) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE, "Weather forecast is stale (cached data expired); cannot safely evaluate wind drift risk");
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

        for (OperationalTaskContext task : activeSprayingTasks) {
            LocalDate targetDate = task.getDueDate() != null ? task.getDueDate() : context.getAsOfDate();
            if (targetDate == null) {
                targetDate = LocalDate.now();
            }

            DailyForecastItem forecast = forecastByDate.get(targetDate);
            if (forecast == null) {
                tasksBeyondHorizon++;
                continue;
            }

            double maxWind = forecast.getMaxWindSpeedKmh() != null ? forecast.getMaxWindSpeedKmh() : 0.0;
            double maxGust = forecast.getMaxWindGustsKmh() != null ? forecast.getMaxWindGustsKmh() : 0.0;

            if (maxWind > SUSTAINED_WIND_TRIGGER_KMH || maxGust > GUST_TRIGGER_KMH) {
                AdvisorySeverity severity = (maxWind > HIGH_SEVERITY_WIND_KMH || maxGust > HIGH_SEVERITY_GUST_KMH)
                        ? AdvisorySeverity.HIGH : AdvisorySeverity.MEDIUM;

                String fingerprint = buildFingerprint(farmId, plantingId, task.getTaskId(), RULE_CODE, targetDate);

                proposals.add(AdvisoryProposal.builder()
                        .fingerprint(fingerprint)
                        .ruleCode(RULE_CODE)
                        .severity(severity)
                        .title("High Wind Warning — Chemical Spraying Drift Risk")
                        .message(String.format("High winds (sustained %.1f km/h, gusts up to %.1f km/h) are expected on %s for %s. Chemical spraying in windy conditions causes severe off-target drift, wastage, and inhalation hazard.",
                                maxWind, maxGust, targetDate, cropName))
                        .recommendedAction("Postpone spraying or chemical application until wind speeds subside below 15 km/h, preferably during calm early morning hours.")
                        .targetDate(targetDate)
                        .taskId(task.getTaskId())
                        .provenance(PROVENANCE)
                        .expiresAt(targetDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))
                        .build());
            }
        }

        if (!proposals.isEmpty()) {
            return RuleEvaluationResult.success(RULE_CODE,
                    String.format("Generated %d chemical spraying wind drift advisory proposal(s)", proposals.size()),
                    proposals);
        }

        if (tasksBeyondHorizon == activeSprayingTasks.size()) {
            return RuleEvaluationResult.insufficientContext(RULE_CODE,
                    "All scheduled spraying tasks have due dates beyond the available weather forecast horizon");
        }

        return RuleEvaluationResult.success(RULE_CODE,
                String.format("Evaluated %d spraying task(s); forecast wind speed is within safe application thresholds", activeSprayingTasks.size() - tasksBeyondHorizon));
    }

    private List<OperationalTaskContext> getActiveSprayingTasks(AgriculturalContextResponse context) {
        if (context.getRelevantTasks() == null) {
            return Collections.emptyList();
        }
        return context.getRelevantTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS)
                .filter(this::isSprayingOrTreatmentTask)
                .collect(Collectors.toList());
    }

    private boolean isSprayingOrTreatmentTask(OperationalTaskContext task) {
        if (task.getTaskType() != null) {
            String type = task.getTaskType().toUpperCase();
            if (type.equals("FERTILIZER_APPLICATION") || type.equals("DISEASE_TREATMENT") || type.equals("PEST_CONTROL")) {
                return true;
            }
        }
        String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
        return title.contains("spray") || title.contains("top-dressing") || title.contains("fertilizer")
                || title.contains("fungicide") || title.contains("pesticide") || title.contains("chemical")
                || title.contains("dusting");
    }

    private String buildFingerprint(UUID farmId, UUID plantingId, UUID taskId, String ruleCode, LocalDate targetDate) {
        String fId = farmId != null ? farmId.toString() : "GLOBAL";
        String pId = plantingId != null ? plantingId.toString() : "GLOBAL";
        String tId = taskId != null ? taskId.toString() : "GLOBAL";
        return fId + ":" + pId + ":" + tId + ":" + ruleCode + ":" + targetDate;
    }
}
