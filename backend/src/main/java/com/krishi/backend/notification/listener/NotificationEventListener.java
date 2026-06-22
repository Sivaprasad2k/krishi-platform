package com.krishi.backend.notification.listener;

import com.krishi.backend.cropcycle.entity.CropCycle;
import com.krishi.backend.cropcycle.event.CropCycleCompletedEvent;
import com.krishi.backend.cropcycle.event.CropCycleStartedEvent;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.diseaseincident.entity.DiseaseIncident;
import com.krishi.backend.diseaseincident.event.*;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.notification.entity.NotificationType;
import com.krishi.backend.notification.service.NotificationService;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.event.*;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.entity.Treatment;
import com.krishi.backend.treatment.event.*;
import com.krishi.backend.treatment.repository.TreatmentRepository;
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
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final CropCycleRepository cropCycleRepository;
    private final DiseaseIncidentRepository diseaseIncidentRepository;
    private final TreatmentRepository treatmentRepository;
    private final TaskRepository taskRepository;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleCropCycleStarted(CropCycleStartedEvent event) {
        log.debug("Notification handler caught CropCycleStartedEvent for aggregate [{}]", event.aggregateId());
        CropCycle cycle = cropCycleRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Crop cycle not found: " + event.aggregateId()));
        UUID userId = cycle.getFarm().getOwner().getId();
        String title = "Crop Cycle Started";
        String message = "Crop cycle " + cycle.getName() + " has started.";
        notificationService.createNotification(userId, title, message, NotificationType.CROP_CYCLE_STARTED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleCropCycleCompleted(CropCycleCompletedEvent event) {
        log.debug("Notification handler caught CropCycleCompletedEvent for aggregate [{}]", event.aggregateId());
        CropCycle cycle = cropCycleRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Crop cycle not found: " + event.aggregateId()));
        UUID userId = cycle.getFarm().getOwner().getId();
        String title = "Crop Cycle Completed";
        String message = "Crop cycle " + cycle.getName() + " has completed successfully.";
        notificationService.createNotification(userId, title, message, NotificationType.CROP_CYCLE_COMPLETED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleDiseaseIncidentReported(DiseaseIncidentReportedEvent event) {
        log.debug("Notification handler caught DiseaseIncidentReportedEvent for aggregate [{}]", event.aggregateId());
        DiseaseIncident incident = diseaseIncidentRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found: " + event.aggregateId()));
        UUID userId = incident.getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Disease Incident Reported";
        String message = incident.getDiseaseName() + " reported for " + incident.getCropPlanting().getCropName() + " (" + incident.getCropPlanting().getCropVariety() + ")";
        notificationService.createNotification(userId, title, message, NotificationType.DISEASE_REPORTED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleDiseaseIncidentUnderReview(DiseaseIncidentUnderReviewEvent event) {
        log.debug("Notification handler caught DiseaseIncidentUnderReviewEvent for aggregate [{}]", event.aggregateId());
        DiseaseIncident incident = diseaseIncidentRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found: " + event.aggregateId()));
        UUID userId = incident.getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Disease Incident Under Review";
        String message = incident.getDiseaseName() + " is now under review.";
        notificationService.createNotification(userId, title, message, NotificationType.DISEASE_UNDER_REVIEW);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleDiseaseIncidentTreated(DiseaseIncidentTreatedEvent event) {
        log.debug("Notification handler caught DiseaseIncidentTreatedEvent for aggregate [{}]", event.aggregateId());
        DiseaseIncident incident = diseaseIncidentRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found: " + event.aggregateId()));
        UUID userId = incident.getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Disease Incident Treated";
        String message = incident.getDiseaseName() + " has been marked as treated.";
        notificationService.createNotification(userId, title, message, NotificationType.DISEASE_TREATED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleDiseaseIncidentClosed(DiseaseIncidentClosedEvent event) {
        log.debug("Notification handler caught DiseaseIncidentClosedEvent for aggregate [{}]", event.aggregateId());
        DiseaseIncident incident = diseaseIncidentRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found: " + event.aggregateId()));
        UUID userId = incident.getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Disease Incident Closed";
        String message = incident.getDiseaseName() + " incident has been closed.";
        notificationService.createNotification(userId, title, message, NotificationType.DISEASE_CLOSED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTreatmentPlanned(TreatmentPlannedEvent event) {
        log.debug("Notification handler caught TreatmentPlannedEvent for aggregate [{}]", event.aggregateId());
        Treatment treatment = treatmentRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + event.aggregateId()));
        UUID userId = treatment.getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Treatment Planned";
        String message = treatment.getTreatmentName() + " treatment plan created.";
        notificationService.createNotification(userId, title, message, NotificationType.TREATMENT_PLANNED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTreatmentStarted(TreatmentStartedEvent event) {
        log.debug("Notification handler caught TreatmentStartedEvent for aggregate [{}]", event.aggregateId());
        Treatment treatment = treatmentRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + event.aggregateId()));
        UUID userId = treatment.getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Treatment Started";
        String message = treatment.getTreatmentName() + " treatment started";
        notificationService.createNotification(userId, title, message, NotificationType.TREATMENT_STARTED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTreatmentCompleted(TreatmentCompletedEvent event) {
        log.debug("Notification handler caught TreatmentCompletedEvent for aggregate [{}]", event.aggregateId());
        Treatment treatment = treatmentRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + event.aggregateId()));
        UUID userId = treatment.getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Treatment Completed";
        String message = treatment.getTreatmentName() + " treatment completed successfully";
        notificationService.createNotification(userId, title, message, NotificationType.TREATMENT_COMPLETED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTaskCreated(TaskCreatedEvent event) {
        log.debug("Notification handler caught TaskCreatedEvent for aggregate [{}]", event.aggregateId());
        Task task = taskRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + event.aggregateId()));
        UUID userId = task.getTreatment().getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Task Created";
        String message = task.getTitle() + (task.getDueDate() != null ? " before " + task.getDueDate().toLocalDate() : "");
        notificationService.createNotification(userId, title, message, NotificationType.TASK_CREATED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTaskStarted(TaskStartedEvent event) {
        log.debug("Notification handler caught TaskStartedEvent for aggregate [{}]", event.aggregateId());
        Task task = taskRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + event.aggregateId()));
        UUID userId = task.getTreatment().getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Task Started";
        String message = "Task " + task.getTitle() + " is now in progress.";
        notificationService.createNotification(userId, title, message, NotificationType.TASK_STARTED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTaskCompleted(TaskCompletedEvent event) {
        log.debug("Notification handler caught TaskCompletedEvent for aggregate [{}]", event.aggregateId());
        Task task = taskRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + event.aggregateId()));
        UUID userId = task.getTreatment().getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Task Completed";
        String message = "Task completed successfully";
        notificationService.createNotification(userId, title, message, NotificationType.TASK_COMPLETED);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleTaskCancelled(TaskCancelledEvent event) {
        log.debug("Notification handler caught TaskCancelledEvent for aggregate [{}]", event.aggregateId());
        Task task = taskRepository.findById(event.aggregateId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + event.aggregateId()));
        UUID userId = task.getTreatment().getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId();
        String title = "Task Cancelled";
        String message = "Task " + task.getTitle() + " has been cancelled.";
        notificationService.createNotification(userId, title, message, NotificationType.TASK_CANCELLED);
    }
}
