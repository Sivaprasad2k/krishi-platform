package com.krishi.backend.common.event.listener;

import com.krishi.backend.common.event.BaseDomainEvent;
import com.krishi.backend.common.event.entity.DomainEvent;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.common.event.serialization.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPersistenceListener {

    private final DomainEventRepository domainEventRepository;
    private final EventSerializer eventSerializer;

    /**
     * Listens for all domain events and persists them to the database.
     * This execution is synchronous and binds to the active transaction.
     *
     * @param event the domain event that was published
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleDomainEvent(BaseDomainEvent event) {
        log.info("Intercepted domain event [{}] for aggregate [{}]", event.getEventType(), event.aggregateId());

        String serializedData = eventSerializer.serialize(event);

        DomainEvent entity = DomainEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.aggregateId())
                .eventType(event.getEventType())
                .eventData(serializedData)
                .occurredAt(event.occurredAt())
                .createdBy(event.createdBy())
                .build();

        domainEventRepository.save(entity);
        log.debug("Successfully persisted domain event to database: {}", entity.getId());
    }
}
