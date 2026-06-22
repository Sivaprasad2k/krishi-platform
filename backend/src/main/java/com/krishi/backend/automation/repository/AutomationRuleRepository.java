package com.krishi.backend.automation.repository;

import com.krishi.backend.automation.entity.AutomationRule;
import com.krishi.backend.automation.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {

    /**
     * Finds all enabled automation rules for a specific event type.
     *
     * @param eventType the event type
     * @return list of enabled automation rules
     */
    List<AutomationRule> findByEventTypeAndEnabledTrue(EventType eventType);
}
