package com.krishi.backend.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

public interface BaseDomainEvent {

    UUID aggregateId();

    String getAggregateType();

    String getEventType();

    LocalDateTime occurredAt();

    UUID createdBy();
}