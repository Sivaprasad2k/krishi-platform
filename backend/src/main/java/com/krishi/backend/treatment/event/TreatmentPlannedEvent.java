package com.krishi.backend.treatment.event;

import com.krishi.backend.common.event.BaseDomainEvent;
import com.krishi.backend.treatment.entity.TreatmentStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TreatmentPlannedEvent(
        UUID aggregateId,
        UUID diseaseIncidentId,
        String treatmentName,
        TreatmentStatus status,
        LocalDateTime occurredAt,
        UUID createdBy
) implements BaseDomainEvent {

    @Override
    public String getAggregateType() {
        return "TREATMENT";
    }

    @Override
    public String getEventType() {
        return "TREATMENT_PLANNED";
    }
}
