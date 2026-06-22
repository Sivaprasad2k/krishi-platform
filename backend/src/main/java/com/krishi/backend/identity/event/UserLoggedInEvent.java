package com.krishi.backend.identity.event;

import com.krishi.backend.common.event.BaseDomainEvent;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserLoggedInEvent(

        UUID aggregateId,
        String phoneNumber,
        LocalDateTime occurredAt,
        UUID createdBy

) implements BaseDomainEvent {

    @Override
    public String getAggregateType() {
        return "USER";
    }

    @Override
    public String getEventType() {
        return "USER_LOGGED_IN";
    }
}
