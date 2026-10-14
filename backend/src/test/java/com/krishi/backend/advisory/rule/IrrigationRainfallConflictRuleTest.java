package com.krishi.backend.advisory.rule;

import com.krishi.backend.advisory.rule.impl.IrrigationRainfallConflictRule;
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

@DisplayName("Irrigation Rainfall Conflict Rule Unit Tests")
class IrrigationRainfallConflictRuleTest {

    private IrrigationRainfallConflictRule rule;
    private UUID farmId;
    private UUID plantingId;
    private UUID taskId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        rule = new IrrigationRainfallConflictRule();
        farmId = UUID.randomUUID();
        plantingId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        today = LocalDate.now();
    }

    @Test
    @DisplayName("Should generate HIGH severity advisory when forecast precipitation >= 50mm")
    void testHeavyPrecipitationHighSeverity() {
        AgriculturalContextResponse context = buildContext(
                "IRRIGATION",
                "Flood Irrigation Field A",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.FRESH,
                55.0,
                95
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(1, result.getProposals().size());

        AdvisoryProposal proposal = result.getProposals().get(0);
        assertEquals(IrrigationRainfallConflictRule.RULE_CODE, proposal.getRuleCode());
        assertEquals(AdvisorySeverity.HIGH, proposal.getSeverity());
        assertEquals(today, proposal.getTargetDate());
        assertEquals(taskId, proposal.getTaskId());
        assertTrue(proposal.getMessage().contains("55.0 mm"));
        assertTrue(proposal.getMessage().contains("95%"));
        assertNotNull(proposal.getFingerprint());
    }

    @Test
    @DisplayName("Should generate MEDIUM severity advisory when precipitation >= 20mm but < 50mm")
    void testModeratePrecipitationMediumSeverity() {
        AgriculturalContextResponse context = buildContext(
                "IRRIGATION",
                "Scheduled Drip Watering",
                TaskStatus.IN_PROGRESS,
                today,
                WeatherFreshnessStatus.FRESH,
                25.0,
                70
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(1, result.getProposals().size());

        AdvisoryProposal proposal = result.getProposals().get(0);
        assertEquals(AdvisorySeverity.MEDIUM, proposal.getSeverity());
    }

    @Test
    @DisplayName("Should generate MEDIUM severity advisory when probability >= 80% even with low precipitation mm")
    void testHighProbabilityTrigger() {
        AgriculturalContextResponse context = buildContext(
                "IRRIGATION",
                "Scheduled Watering",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.FRESH,
                15.0,
                85
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertEquals(1, result.getProposals().size());
        assertEquals(AdvisorySeverity.MEDIUM, result.getProposals().get(0).getSeverity());
    }

    @Test
    @DisplayName("Should return SUCCESS with no proposals when rainfall is below threshold")
    void testRainfallBelowThreshold() {
        AgriculturalContextResponse context = buildContext(
                "IRRIGATION",
                "Submergence Watering",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.FRESH,
                5.0,
                20
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS, result.getOutcome());
        assertTrue(result.getProposals().isEmpty());
        assertTrue(result.getReason().contains("below risk thresholds"));
    }

    @Test
    @DisplayName("Should return INSUFFICIENT_CONTEXT when weather is STALE (never assumes safe)")
    void testStaleWeatherReturnsInsufficientContext() {
        AgriculturalContextResponse context = buildContext(
                "IRRIGATION",
                "Irrigation",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.STALE,
                0.0,
                0
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.INSUFFICIENT_CONTEXT, result.getOutcome());
        assertTrue(result.getProposals().isEmpty());
        assertTrue(result.getReason().contains("stale"));
    }

    @Test
    @DisplayName("Should return INSUFFICIENT_CONTEXT when weather is UNAVAILABLE")
    void testUnavailableWeatherReturnsInsufficientContext() {
        AgriculturalContextResponse context = buildContext(
                "IRRIGATION",
                "Irrigation",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.UNAVAILABLE,
                0.0,
                0
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.INSUFFICIENT_CONTEXT, result.getOutcome());
        assertTrue(result.getReason().contains("unavailable"));
    }

    @Test
    @DisplayName("Should return NOT_APPLICABLE when no irrigation tasks exist")
    void testNoIrrigationTasks() {
        AgriculturalContextResponse context = buildContext(
                "WEEDING",
                "Manual Weeding Row 1",
                TaskStatus.PENDING,
                today,
                WeatherFreshnessStatus.FRESH,
                100.0,
                100
        );

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_NOT_APPLICABLE, result.getOutcome());
        assertTrue(result.getProposals().isEmpty());
    }

    @Test
    @DisplayName("Should ignore completed or cancelled irrigation tasks")
    void testCompletedOrCancelledTasksIgnored() {
        OperationalTaskContext completedTask = OperationalTaskContext.builder()
                .taskId(taskId)
                .title("Completed Irrigation")
                .taskType("IRRIGATION")
                .status(TaskStatus.COMPLETED)
                .dueDate(today)
                .build();

        AgriculturalContextResponse context = AgriculturalContextResponse.builder()
                .asOfDate(today)
                .crop(CropIdentityContext.builder().plantingId(plantingId).cropName("Paddy").build())
                .location(LocationContext.builder().farmId(farmId).build())
                .relevantTasks(List.of(completedTask))
                .weather(WeatherContext.builder()
                        .freshness(WeatherFreshnessStatus.FRESH)
                        .dailyForecast(List.of(DailyForecastItem.builder()
                                .date(today)
                                .precipitationMm(80.0)
                                .precipitationProbabilityMax(99)
                                .build()))
                        .build())
                .build();

        RuleEvaluationResult result = rule.evaluate(context);

        assertEquals(RuleEvaluationOutcome.RULE_NOT_APPLICABLE, result.getOutcome());
    }

    private AgriculturalContextResponse buildContext(
            String taskType,
            String taskTitle,
            TaskStatus status,
            LocalDate dueDate,
            WeatherFreshnessStatus freshness,
            Double precipMm,
            Integer precipProb
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
                .precipitationMm(precipMm)
                .precipitationProbabilityMax(precipProb)
                .build();

        WeatherContext weather = WeatherContext.builder()
                .freshness(freshness)
                .statusMessage(freshness == WeatherFreshnessStatus.FRESH ? "Fresh forecast" : "Error")
                .dailyForecast(freshness == WeatherFreshnessStatus.FRESH || freshness == WeatherFreshnessStatus.STALE
                        ? List.of(forecastItem) : Collections.emptyList())
                .build();

        return AgriculturalContextResponse.builder()
                .asOfDate(today)
                .crop(CropIdentityContext.builder().plantingId(plantingId).cropName("Rice - Jyothi").build())
                .location(LocationContext.builder().farmId(farmId).build())
                .relevantTasks(List.of(task))
                .weather(weather)
                .build();
    }
}
