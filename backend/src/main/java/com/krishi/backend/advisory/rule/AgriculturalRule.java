package com.krishi.backend.advisory.rule;

import com.krishi.backend.cropplanting.dto.context.AgriculturalContextResponse;

/**
 * Contract for deterministic, explainable agricultural advisory rules.
 */
public interface AgriculturalRule {

    /**
     * Unique identifier for the rule (e.g. "IRRIGATION_RAINFALL_CONFLICT").
     */
    String getRuleCode();

    /**
     * Human-readable name of the rule.
     */
    String getRuleName();

    /**
     * Evaluates the rule against the provided agricultural context.
     * Implementations must never interpret missing or stale context as safe conditions.
     *
     * @param context the resolved agricultural context
     * @return the deterministic evaluation result
     */
    RuleEvaluationResult evaluate(AgriculturalContextResponse context);
}
