package com.krishi.backend.common.event.serialization;

import com.krishi.backend.common.event.BaseDomainEvent;

/**
 * Strategy interface for serializing domain events into storage formats (e.g., JSON).
 */
public interface EventSerializer {

    /**
     * Serializes a domain event into its string representation.
     *
     * @param event the domain event to serialize
     * @return the serialized string payload of the event
     */
    String serialize(BaseDomainEvent event);
}
