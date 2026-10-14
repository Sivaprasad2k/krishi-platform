package com.krishi.backend.advisory.rule;

/**
 * Deterministic outcome states for rule evaluation in Krishi.
 */
public enum RuleEvaluationOutcome {
    /**
     * Rule evaluated successfully; advisory generated or conditions confirmed benign.
     */
    RULE_EVALUATION_SUCCESS,

    /**
     * Rule does not apply to the current crop planting, growth stage, or task context.
     */
    RULE_NOT_APPLICABLE,

    /**
     * Required inputs (e.g., weather forecast, planting date, growth stage) are missing or stale.
     * Missing or stale context is explicitly never treated as evidence that conditions are safe.
     */
    INSUFFICIENT_CONTEXT,

    /**
     * An unexpected failure or exception occurred during rule evaluation.
     */
    RULE_EVALUATION_FAILURE
}
