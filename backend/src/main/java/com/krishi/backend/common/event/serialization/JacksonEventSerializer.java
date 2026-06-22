package com.krishi.backend.common.event.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishi.backend.common.event.BaseDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Jackson-based implementation of EventSerializer for serializing events to JSON format.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JacksonEventSerializer implements EventSerializer {

    private final ObjectMapper objectMapper;

    @Override
    public String serialize(BaseDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event of type [{}]", event.getEventType(), e);
            throw new RuntimeException("Failed to serialize domain event to JSON", e);
        }
    }
}
