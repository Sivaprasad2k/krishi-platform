package com.krishi.backend.advisory.event;

import com.krishi.backend.advisory.entity.AdvisoryStatus;
import com.krishi.backend.common.event.BaseDomainEvent;
import com.krishi.backend.weather.entity.AdvisorySeverity;
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
public class AdvisoryCreatedEvent implements BaseDomainEvent {

    private UUID advisoryId;
    private UUID farmId;
    private UUID cropPlantingId;
    private UUID taskId;
    private String ruleCode;
    private AdvisorySeverity severity;
    private String title;
    private AdvisoryStatus status;
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
        return "AGRICULTURAL_ADVISORY_CREATED";
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
