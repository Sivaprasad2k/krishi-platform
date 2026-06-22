package com.krishi.backend.common.event.repository;

import com.krishi.backend.common.event.entity.DomainEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DomainEventRepository
        extends JpaRepository<DomainEvent, UUID> {
}