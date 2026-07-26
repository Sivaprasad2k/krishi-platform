package com.krishi.backend.automation.service;

import com.krishi.backend.automation.entity.ActionType;
import com.krishi.backend.automation.entity.AutomationRule;
import com.krishi.backend.automation.entity.EventType;
import com.krishi.backend.automation.repository.AutomationRuleRepository;
import com.krishi.backend.common.event.BaseDomainEvent;
import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.diseaseincident.entity.DiseaseIncident;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.notification.entity.NotificationType;
import com.krishi.backend.notification.service.NotificationService;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.event.TaskCreatedEvent;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.entity.Treatment;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationService {

    private final AutomationRuleRepository automationRuleRepository;
    private final NotificationService notificationService;
    private final DiseaseIncidentRepository diseaseIncidentRepository;
    private final TreatmentRepository treatmentRepository;
    private final TaskRepository taskRepository;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Executes all active automation rules matching the given domain event.
     * This is run synchronously and shares the current transaction.
     *
     * @param event the triggered domain event
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void executeRulesForEvent(BaseDomainEvent event) {
        EventType eventType = resolveEventType(event.getEventType());
        if (eventType == null) {
            log.debug("Event type [{}] does not trigger any automation rules.", event.getEventType());
            return;
        }

        List<AutomationRule> rules = automationRuleRepository.findByEventTypeAndEnabledTrue(eventType);
        log.debug("Found {} active automation rules for event type [{}]", rules.size(), eventType);

        for (AutomationRule rule : rules) {
            try {
                executeRule(rule, event);
            } catch (Exception e) {
                log.error("Failed to execute automation rule [{}]: {}", rule.getRuleName(), e.getMessage(), e);
                // We let exceptions propagate to ensure transactional consistency
                throw e;
            }
        }
    }

    private void executeRule(AutomationRule rule, BaseDomainEvent event) {
        log.info("Executing automation rule [{}] of type [{}]", rule.getRuleName(), rule.getActionType());

        if (rule.getActionType() == ActionType.CREATE_NOTIFICATION) {
            handleCreateNotification(event);
        } else if (rule.getActionType() == ActionType.CREATE_TASK) {
            handleCreateTask(event);
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void handleCreateNotification(BaseDomainEvent event) {
        // NotificationEventListener already creates notifications for domain events unconditionally.
        // Skipping duplicate notification creation in automation service to prevent duplicate alerts.
        log.debug("Notification for event [{}] is managed by NotificationEventListener; skipping duplicate creation in AutomationService.", event.getEventType());
    }

    private void handleCreateTask(BaseDomainEvent event) {
        if ("TREATMENT_COMPLETED".equals(event.getEventType())) {
            Treatment treatment = treatmentRepository.findById(event.aggregateId())
                    .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + event.aggregateId()));
            UUID userId = treatment.getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId();

            Task task = new Task();
            task.setId(UUID.randomUUID());
            task.setCropPlanting(treatment.getDiseaseIncident().getCropPlanting());
            task.setTreatment(treatment);
            task.setTaskType(com.krishi.backend.task.entity.TaskType.CROP_INSPECTION);
            task.setSource(com.krishi.backend.task.entity.TaskSource.AUTOMATION);
            task.setTitle("INSPECT_CROP_RECOVERY");
            task.setDescription(String.format("{\"treatmentName\":\"%s\"}", escapeJson(treatment.getTreatmentName())));
            task.setDueDate(LocalDateTime.now().plusDays(7));
            task.setStatus(TaskStatus.PENDING);
            task.setCreatedAt(LocalDateTime.now());
            task.setCreatedBy(userId);

            Task saved = taskRepository.save(task);
            log.info("Automation rule created follow-up Task [{}] for treatment [{}]", saved.getId(), treatment.getId());

            // Publish TaskCreatedEvent to align with event-driven system lifecycle
            TaskCreatedEvent taskCreatedEvent = TaskCreatedEvent.builder()
                    .aggregateId(saved.getId())
                    .treatmentId(treatment.getId())
                    .title(saved.getTitle())
                    .status(saved.getStatus())
                    .occurredAt(LocalDateTime.now())
                    .createdBy(userId)
                    .build();
            domainEventPublisher.publish(taskCreatedEvent);
        }
    }

    private EventType resolveEventType(String eventTypeStr) {
        if ("DISEASE_INCIDENT_REPORTED".equals(eventTypeStr)) {
            return EventType.DISEASE_REPORTED;
        }
        try {
            return EventType.valueOf(eventTypeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
