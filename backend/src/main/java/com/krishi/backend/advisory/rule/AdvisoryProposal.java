package com.krishi.backend.advisory.rule;

import com.krishi.backend.weather.entity.AdvisorySeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable candidate advisory proposal emitted by a rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisoryProposal {
    private String fingerprint;
    private String ruleCode;
    private AdvisorySeverity severity;
    private String title;
    private String message;
    private String recommendedAction;
    private LocalDate targetDate;
    private UUID taskId;
    private String provenance;
    private OffsetDateTime expiresAt;
}
