package com.krishi.backend.advisory.rule;

import com.krishi.backend.advisory.rule.impl.ChemicalSprayingWindRiskRule;
import com.krishi.backend.cropplanting.dto.context.*;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.weather.dto.DailyForecastItem;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Chemical Spraying Wind Risk Rule Unit Tests")
class ChemicalSprayingWindRiskRuleTest {

    private ChemicalSprayingWindRiskRule rule;
    private UUID farmId;
    private UUID plantingId;
    private UUID taskId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        rule = new ChemicalSprayingWindRiskRule();
        farmId = UUID.randomUUID();
        plantingId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        today = LocalDate.now();
    }

    @Test
    @DisplayName("Should generate HIGH severity advisory when sustained wind > 35 km/h or gust > 45 km/h")
    void testSevereWindHighSeverity() {
        AgriculturalContextResponse context = buildContext(
                "DISEASE_TREATMENT",
                "Spray Fungicide for Blast",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.FRESH,
                38.0,
                52.0
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(1, result.getProposals().size());

        AdvisoryProposal proposal = result.getProposals().get(0);
        assertEquals(ChemicalSprayingWindRiskRule.RULE_CODE, proposal.getRuleCode());
        assertEquals(AdvisorySeverity.HIGH, proposal.getSeverity());
        assertTrue(proposal.getMessage().contains("38.0 km/h"));
        assertTrue(proposal.getMessage().contains("52.0 km/h"));
        assertTrue(proposal.getProvenance().contains("Provisional"));
    }

    @Test
    @DisplayName("Should generate MEDIUM severity advisory when wind > 20 km/h but <= 35 km/h")
    void testModerateWindMediumSeverity() {
        AgriculturalContextResponse context = buildContext(
                "FERTILIZER_APPLICATION",
                "Foliar Spray Micronutrients",
                TaskStatus.IN_PROGRESS,
                today,
                WeatherFreshnessStatus.FRESH,
                24.0,
                28.0
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(1, result.getProposals().size());
        assertEquals(AdvisorySeverity.MEDIUM, result.getProposals().get(0).getSeverity());
    }

    @Test
    @DisplayName("Should return SUCCESS with no proposals when wind is calm (<= 20 km/h)")
    void testCalmWindSafe() {
        AgriculturalContextResponse context = buildContext(
                "PEST_CONTROL",
                "Neem Oil Application",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.FRESH,
                12.0,
                18.0
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertTrue(result.getProposals().isEmpty());
        assertTrue(result.getReason().contains("safe application thresholds"));
    }

    @Test
    @DisplayName("Should return INSUFFICIENT_CONTEXT when weather is STALE (never assumes safe)")
    void testStaleWeatherReturnsInsufficientContext() {
        AgriculturalContextResponse context = buildContext(
                "DISEASE_TREATMENT",
                "Spray Pesticide",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.STALE,
                5.0,
                8.0
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.INSUFFICIENT_CONTEXT, result.getOutcome());
        assertTrue(result.getReason().contains("stale"));
    }

    @Test
    @DisplayName("Should return NOT_APPLICABLE when no spraying tasks exist")
    void testNoSprayingTasks() {
        AgriculturalContextResponse context = buildContext(
                "HARVESTING",
                "Harvest Plot 1",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.FRESH,
                40.0,
                55.0
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_NOT_APPLICABLE, result.getOutcome());
        assertTrue(result.getProposals().isEmpty());
    }

    private AgriculturalContextResponse buildContext(
            String taskType,
            String taskTitle,
            TaskStatus status,
            LocalDate dueDate,
            WeatherFreshnessStatus freshness,
            Double windSpeed,
            Double windGust
    ) {
        OperationalTaskContext task = OperationalTaskContext.builder()
                .taskId(taskId)
                .title(taskTitle)
                .taskType(taskType)
                .status(status)
                .dueDate(dueDate)
                .build();

        DailyForecastItem forecastItem = DailyForecastItem.builder()
                .date(dueDate)
                .maxWindSpeedKmh(windSpeed)
                .maxWindGustsKmh(windGust)
                .build();

        WeatherContext weather = WeatherContext.builder()
                .freshness(freshness)
                .statusMessage(freshness == WeatherFreshnessStatus.FRESH ? "Fresh forecast" : "Error")
                .dailyForecast(freshness == WeatherFreshnessStatus.FRESH || freshness == WeatherFreshnessStatus.STALE
                        ? List.of(forecastItem) : Collections.emptyList())
                .build();

        return AgriculturalContextResponse.builder()
                .asOfDate(today)
                .crop(CropIdentityContext.builder().plantingId(plantingId).cropName("Tomato").build())
                .location(LocationContext.builder().farmId(farmId).build())
                .relevantTasks(List.of(task))
                .weather(weather)
                .build();
    }
}
