package com.krishi.backend.task.event;

import com.krishi.backend.common.event.BaseDomainEvent;
import com.krishi.backend.task.entity.TaskStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TaskCreatedEvent(
        UUID aggregateId,
        UUID treatmentId,
        String title,
        TaskStatus status,
        LocalDateTime occurredAt,
        UUID createdBy
) implements BaseDomainEvent {

    @Override
    public String getAggregateType() {
        return "TASK";
    }

    @Override
    public String getEventType() {
        return "TASK_CREATED";
    }
}
