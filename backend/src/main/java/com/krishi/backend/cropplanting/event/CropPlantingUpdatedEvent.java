package com.krishi.backend.cropplanting.event;

import com.krishi.backend.common.event.BaseDomainEvent;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CropPlantingUpdatedEvent(
        UUID aggregateId,
        String cropName,
        String cropVariety,
        LocalDateTime plantingDate,
        LocalDateTime expectedHarvestDate,
        LocalDateTime occurredAt,
        UUID createdBy
) implements BaseDomainEvent {

    @Override
    public String getAggregateType() {
        return "CROP_PLANTING";
    }

    @Override
    public String getEventType() {
        return "CROP_PLANTING_UPDATED";
    }
}
