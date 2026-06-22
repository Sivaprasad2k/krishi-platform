package com.krishi.backend.common.event.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "domain_events", schema = "krishi")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String eventData;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private UUID createdBy;
}