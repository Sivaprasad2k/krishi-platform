package com.krishi.backend.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publishes a domain event to Spring's application event publisher.
     *
     * @param event the domain event to publish
     */
    public void publish(BaseDomainEvent event) {
        eventPublisher.publishEvent(event);
    }
}
