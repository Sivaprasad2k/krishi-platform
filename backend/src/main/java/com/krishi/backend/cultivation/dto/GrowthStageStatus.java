package com.krishi.backend.cultivation.dto;

/**
 * Represents the status of the agronomic growth-stage estimation.
 */
public enum GrowthStageStatus {
    /**
     * Estimated stage was successfully resolved from an active cultivation plan.
     */
    RESOLVED,

    /**
     * No active cultivation plan is associated with this crop planting.
     */
    NO_ACTIVE_PLAN,

    /**
     * Target date precedes the planting date and falls outside any pre-sowing stage window.
     */
    PRE_PLANTING,

    /**
     * Target date exceeds the planned duration or final stage window of the cultivation template.
     */
    POST_HARVEST,

    /**
     * The cultivation template has missing, overlapping, or gapped stage definitions.
     */
    STAGE_DEFINITION_ERROR,

    /**
     * The planting date on the crop planting record is null.
     */
    MISSING_PLANTING_DATE
}
