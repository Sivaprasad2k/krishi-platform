package com.krishi.backend.cropcycle.event;

import com.krishi.backend.common.event.BaseDomainEvent;
import com.krishi.backend.cropcycle.entity.CropCycleStatus;
import com.krishi.backend.cropcycle.entity.SeasonType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CropCycleCreatedEvent(
        UUID aggregateId,
        UUID farmId,
        String name,
        SeasonType seasonType,
        Integer year,
        CropCycleStatus status,
        LocalDateTime occurredAt,
        UUID createdBy
) implements BaseDomainEvent {

    @Override
    public String getAggregateType() {
        return "CROP_CYCLE";
    }

    @Override
    public String getEventType() {
        return "CROP_CYCLE_CREATED";
    }
}
