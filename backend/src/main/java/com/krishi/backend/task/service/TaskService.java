package com.krishi.backend.task.service;

import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.task.dto.TaskCreateRequest;
import com.krishi.backend.task.dto.TaskResponse;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.event.TaskCancelledEvent;
import com.krishi.backend.task.event.TaskCompletedEvent;
import com.krishi.backend.task.event.TaskCreatedEvent;
import com.krishi.backend.task.event.TaskStartedEvent;
import com.krishi.backend.task.mapper.TaskMapper;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.entity.Treatment;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TreatmentRepository treatmentRepository;
    private final TaskMapper taskMapper;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Creates a new Task for a specific Treatment plan.
     */
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Treatment treatment = treatmentRepository.findById(request.getTreatmentId())
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found with id: " + request.getTreatmentId()));

        validateTreatmentOwnership(treatment, userDetails.getUserId());

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTreatment(treatment);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setCreatedBy(userDetails.getUserId());

        Task saved = taskRepository.save(task);

        // Publish TaskCreatedEvent
        TaskCreatedEvent event = TaskCreatedEvent.builder()
                .aggregateId(saved.getId())
                .treatmentId(treatment.getId())
                .title(saved.getTitle())
                .status(saved.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return taskMapper.toResponse(saved);
    }

    /**
     * Starts the Task, moving status from PENDING to IN_PROGRESS.
     */
    @Transactional
    public TaskResponse startTask(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateTreatmentOwnership(task.getTreatment(), userDetails.getUserId());

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Cannot start a task that is in status: " + task.getStatus());
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setUpdatedAt(LocalDateTime.now());
        task.setUpdatedBy(userDetails.getUserId());

        Task updated = taskRepository.save(task);

        // Publish TaskStartedEvent
        TaskStartedEvent event = TaskStartedEvent.builder()
                .aggregateId(updated.getId())
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return taskMapper.toResponse(updated);
    }

    /**
     * Completes the Task, moving status from IN_PROGRESS to COMPLETED.
     */
    @Transactional
    public TaskResponse completeTask(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateTreatmentOwnership(task.getTreatment(), userDetails.getUserId());

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete a task that is in status: " + task.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        task.setUpdatedBy(userDetails.getUserId());

        Task updated = taskRepository.save(task);

        // Publish TaskCompletedEvent
        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .aggregateId(updated.getId())
                .completedAt(now)
                .status(updated.getStatus())
                .occurredAt(now)
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return taskMapper.toResponse(updated);
    }

    /**
     * Cancels the Task, moving status from PENDING to CANCELLED.
     */
    @Transactional
    public TaskResponse cancelTask(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateTreatmentOwnership(task.getTreatment(), userDetails.getUserId());

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel a task that is in status: " + task.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.CANCELLED);
        task.setCompletedAt(now); // Recording actual cancellation timestamp
        task.setUpdatedAt(now);
        task.setUpdatedBy(userDetails.getUserId());

        Task updated = taskRepository.save(task);

        // Publish TaskCancelledEvent
        TaskCancelledEvent event = TaskCancelledEvent.builder()
                .aggregateId(updated.getId())
                .cancelledAt(now)
                .status(updated.getStatus())
                .occurredAt(now)
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return taskMapper.toResponse(updated);
    }

    /**
     * Retrieves details of a specific Task, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateTreatmentOwnership(task.getTreatment(), userDetails.getUserId());

        return taskMapper.toResponse(task);
    }

    /**
     * Lists all tasks associated with a specific treatment, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByTreatment(UUID treatmentId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found with id: " + treatmentId));

        validateTreatmentOwnership(treatment, userDetails.getUserId());

        List<Task> tasks = taskRepository.findByTreatmentId(treatmentId);
        return tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Enforces the ROLE_FARMER check.
     */
    private void validateFarmerRole(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Access denied. User is not authenticated.");
        }
        boolean isFarmer = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_FARMER"));
        if (!isFarmer) {
            throw new AccessDeniedException("Access denied. Only farmers are allowed to access this resource.");
        }
    }

    /**
     * Enforces ownership chain walk: Treatment -> DiseaseIncident -> CropPlanting -> CropCycle -> Farm -> Farmer (User)
     */
    private void validateTreatmentOwnership(Treatment treatment, UUID userId) {
        if (!treatment.getDiseaseIncident().getCropPlanting().getCropCycle().getFarm().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this treatment.");
        }
    }
}
