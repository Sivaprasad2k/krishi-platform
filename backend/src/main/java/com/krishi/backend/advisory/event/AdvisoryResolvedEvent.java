package com.krishi.backend.advisory.event;

import com.krishi.backend.advisory.entity.AdvisoryStatus;
import com.krishi.backend.common.event.BaseDomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisoryResolvedEvent implements BaseDomainEvent {

    private UUID advisoryId;
    private UUID farmId;
    private UUID cropPlantingId;
    private String ruleCode;
    private AdvisoryStatus status;
    private String resolutionReason;
    private LocalDateTime occurredAt;
    private UUID createdBy;

    @Override
    public UUID aggregateId() {
        return advisoryId;
    }

    @Override
    public String getAggregateType() {
        return "AgriculturalAdvisory";
    }

    @Override
    public String getEventType() {
        return "AGRICULTURAL_ADVISORY_RESOLVED";
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    @Override
    public UUID createdBy() {
        return createdBy;
    }
}
