package com.krishi.backend.cultivation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Encapsulates the estimated phenological growth stage of a crop planting.
 * This represents a calculated estimation derived from elapsed calendar days
 * and certified cultivation templates, and must not be treated as a confirmed
 * farmer observation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimatedGrowthStageResponse {

    /**
     * Diagnostic outcome of the estimation process.
     */
    private GrowthStageStatus status;

    /**
     * Human-readable name of the resolved cultivation stage (e.g. "Tillering & Vegetative Phase").
     * Null if status is not RESOLVED.
     */
    private String stageName;

    /**
     * Unique identifier of the matching CultivationStage entity.
     */
    private UUID stageId;

    /**
     * 1-based sequence order of the stage within the cultivation template.
     */
    private Integer stageOrder;

    /**
     * Total number of growth stages configured in the applicable cultivation template.
     */
    private Integer totalStages;

    /**
     * Number of calendar days elapsed between the planting date and the evaluation date.
     * Calculated using ChronoUnit.DAYS.between(plantingDate, asOfDate).
     * Day 0 corresponds to the planting date itself. Negative values indicate future planting dates.
     */
    private Long elapsedDays;

    /**
     * Day offset marking the planned beginning of this growth stage (relative to plantingDate).
     */
    private Integer stageStartDay;

    /**
     * Day offset marking the planned conclusion of this growth stage (relative to plantingDate).
     */
    private Integer stageEndDay;

    /**
     * Progress percentage [0.0 - 100.0] within the current growth stage window.
     */
    private Double stageProgressPercentage;

    /**
     * Overall progress percentage [0.0 - 100.0] across the total planned cultivation cycle duration.
     */
    private Double overallProgressPercentage;

    /**
     * Name of the crop/template utilized for estimation (e.g. "Rice (Paddy)").
     */
    private String templateName;

    /**
     * Published version string of the template utilized (e.g. "2024.1").
     */
    private String templateVersion;

    /**
     * Agronomic or diagnostic message detailing the estimation basis or resolution failure.
     */
    private String message;
}
