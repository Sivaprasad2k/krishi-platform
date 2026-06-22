package com.krishi.backend.diseaseincident.event;

import com.krishi.backend.common.event.BaseDomainEvent;
import com.krishi.backend.diseaseincident.entity.DiseaseIncidentStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DiseaseIncidentClosedEvent(
        UUID aggregateId,
        DiseaseIncidentStatus status,
        LocalDateTime occurredAt,
        UUID createdBy
) implements BaseDomainEvent {

    @Override
    public String getAggregateType() {
        return "DISEASE_INCIDENT";
    }

    @Override
    public String getEventType() {
        return "DISEASE_INCIDENT_CLOSED";
    }
}
