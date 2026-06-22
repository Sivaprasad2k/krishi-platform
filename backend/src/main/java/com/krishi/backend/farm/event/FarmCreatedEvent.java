package com.krishi.backend.farm.event;

import com.krishi.backend.common.event.BaseDomainEvent;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FarmCreatedEvent(
        UUID aggregateId,
        String name,
        Double sizeHectares,
        String location,
        UUID ownerId,
        LocalDateTime occurredAt,
        UUID createdBy
) implements BaseDomainEvent {

    @Override
    public String getAggregateType() {
        return "FARM";
    }

    @Override
    public String getEventType() {
        return "FARM_CREATED";
    }
}
