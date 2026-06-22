package com.krishi.backend.automation.listener;

import com.krishi.backend.common.event.BaseDomainEvent;
import com.krishi.backend.cropcycle.event.CropCycleStartedEvent;
import com.krishi.backend.diseaseincident.event.DiseaseIncidentReportedEvent;
import com.krishi.backend.task.event.TaskCompletedEvent;
import com.krishi.backend.task.event.TaskCreatedEvent;
import com.krishi.backend.treatment.event.TreatmentCompletedEvent;
import com.krishi.backend.automation.service.AutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutomationEventListener {

    private final AutomationService automationService;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleDiseaseIncidentReported(DiseaseIncidentReportedEvent event) {
        log.debug("Automation listener caught DiseaseIncidentReportedEvent for aggregate: {}", event.aggregateId());
        automationService.executeRulesForEvent(event);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTreatmentCompleted(TreatmentCompletedEvent event) {
        log.debug("Automation listener caught TreatmentCompletedEvent for aggregate: {}", event.aggregateId());
        automationService.executeRulesForEvent(event);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTaskCreated(TaskCreatedEvent event) {
        log.debug("Automation listener caught TaskCreatedEvent for aggregate: {}", event.aggregateId());
        automationService.executeRulesForEvent(event);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTaskCompleted(TaskCompletedEvent event) {
        log.debug("Automation listener caught TaskCompletedEvent for aggregate: {}", event.aggregateId());
        automationService.executeRulesForEvent(event);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleCropCycleStarted(CropCycleStartedEvent event) {
        log.debug("Automation listener caught CropCycleStartedEvent for aggregate: {}", event.aggregateId());
        automationService.executeRulesForEvent(event);
    }
}
