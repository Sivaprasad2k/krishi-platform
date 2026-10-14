package com.krishi.backend.advisory.entity;

/**
 * Lifecycle statuses for Agricultural Advisories.
 */
public enum AdvisoryStatus {
    /**
     * Active, actionable advisory requiring farmer attention.
     */
    ACTIVE,

    /**
     * Acknowledged by farmer; active monitoring continues without repeated alert notifications.
     */
    ACKNOWLEDGED,

    /**
     * Resolved because the underlying condition cleared, task was completed/skipped,
     * or the advisory was explicitly resolved.
     */
    RESOLVED,

    /**
     * Replaced by an escalated advisory for the same fingerprint with higher severity.
     */
    SUPERSEDED,

    /**
     * Expired because the target evaluation date or phenological window has passed.
     */
    EXPIRED
}
