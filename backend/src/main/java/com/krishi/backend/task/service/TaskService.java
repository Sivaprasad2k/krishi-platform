package com.krishi.backend.task.service;

import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.task.dto.TaskCreateRequest;
import com.krishi.backend.task.dto.TaskResponse;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskSource;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.entity.TaskType;
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
    private final CropPlantingRepository cropPlantingRepository;
    private final TreatmentRepository treatmentRepository;
    private final TaskMapper taskMapper;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Creates a new Task for a CropPlanting (with optional Treatment link).
     */
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting cropPlanting = cropPlantingRepository.findById(request.getCropPlantingId())
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + request.getCropPlantingId()));

        validateCropPlantingOwnership(cropPlanting, userDetails.getUserId());

        Treatment treatment = null;
        if (request.getTreatmentId() != null) {
            treatment = treatmentRepository.findById(request.getTreatmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Treatment not found with id: " + request.getTreatmentId()));

            // Domain Invariant: Treatment must belong to the same CropPlanting
            UUID treatmentPlantingId = treatment.getDiseaseIncident().getCropPlanting().getId();
            if (!treatmentPlantingId.equals(cropPlanting.getId())) {
                throw new IllegalArgumentException("Cross-entity mismatch: Treatment does not belong to the specified crop planting.");
            }
        }

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setCropPlanting(cropPlanting);
        task.setTreatment(treatment);
        task.setTaskType(request.getTaskType() != null ? request.getTaskType() : TaskType.OTHER);
        task.setSource(TaskSource.MANUAL); // Provenance set by backend
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
                .treatmentId(treatment != null ? treatment.getId() : null)
                .title(saved.getTitle())
                .status(saved.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return taskMapper.toResponse(saved);
    }

    /**
     * Starts the Task, moving status from PENDING or OVERDUE to IN_PROGRESS.
     */
    @Transactional
    public TaskResponse startTask(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateCropPlantingOwnership(task.getCropPlanting(), userDetails.getUserId());

        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.OVERDUE) {
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
     * Completes the Task, moving status from IN_PROGRESS, PENDING, or OVERDUE to COMPLETED.
     */
    @Transactional
    public TaskResponse completeTask(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateCropPlantingOwnership(task.getCropPlanting(), userDetails.getUserId());

        if (task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.OVERDUE) {
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
     * Cancels the Task, moving status from PENDING, IN_PROGRESS, or OVERDUE to CANCELLED.
     */
    @Transactional
    public TaskResponse cancelTask(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateCropPlantingOwnership(task.getCropPlanting(), userDetails.getUserId());

        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.OVERDUE) {
            throw new IllegalStateException("Cannot cancel a task that is in status: " + task.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.CANCELLED);
        task.setCompletedAt(now);
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
     * Skips the Task, moving status from PENDING, IN_PROGRESS, or OVERDUE to SKIPPED.
     */
    @Transactional
    public TaskResponse skipTask(UUID id, com.krishi.backend.task.dto.TaskSkipRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateCropPlantingOwnership(task.getCropPlanting(), userDetails.getUserId());

        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.OVERDUE) {
            throw new IllegalStateException("Cannot skip a task that is in status: " + task.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.SKIPPED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        task.setUpdatedBy(userDetails.getUserId());

        if (request != null && request.getReason() != null && !request.getReason().trim().isEmpty()) {
            task.setSkipReason(request.getReason().trim());
        }

        Task updated = taskRepository.save(task);
        return taskMapper.toResponse(updated);
    }

    @Transactional
    public TaskResponse skipTask(UUID id, CustomUserDetails userDetails) {
        return skipTask(id, null, userDetails);
    }

    /**
     * Reschedules the Task, updating due date. If currently OVERDUE and new date is in the future, reverts to PENDING.
     */
    @Transactional
    public TaskResponse rescheduleTask(UUID id, com.krishi.backend.task.dto.TaskRescheduleRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));

        validateCropPlantingOwnership(task.getCropPlanting(), userDetails.getUserId());

        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.OVERDUE) {
            throw new IllegalStateException("Cannot reschedule a task that is in status: " + task.getStatus());
        }

        task.setDueDate(request.getDueDate());
        if (task.getStatus() == TaskStatus.OVERDUE && request.getDueDate() != null && request.getDueDate().isAfter(LocalDateTime.now())) {
            task.setStatus(TaskStatus.PENDING);
        }
        task.setUpdatedAt(LocalDateTime.now());
        task.setUpdatedBy(userDetails.getUserId());

        Task updated = taskRepository.save(task);
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

        validateCropPlantingOwnership(task.getCropPlanting(), userDetails.getUserId());

        return taskMapper.toResponse(task);
    }

    /**
     * Lists all tasks for a specific CropPlanting, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByCropPlanting(UUID cropPlantingId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting cropPlanting = cropPlantingRepository.findById(cropPlantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + cropPlantingId));

        validateCropPlantingOwnership(cropPlanting, userDetails.getUserId());

        List<Task> tasks = taskRepository.findByCropPlantingId(cropPlantingId);
        return tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lists all tasks associated with a specific treatment, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByTreatment(UUID treatmentId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found with id: " + treatmentId));

        validateCropPlantingOwnership(treatment.getDiseaseIncident().getCropPlanting(), userDetails.getUserId());

        List<Task> tasks = taskRepository.findByTreatmentId(treatmentId);
        return tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lists all tasks across all crop plantings for the authenticated farmer.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasksForUser(CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        List<Task> tasks = taskRepository.findByCropPlantingCropCycleFarmOwnerId(userDetails.getUserId());
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
     * Enforces ownership chain: CropPlanting -> CropCycle -> Farm -> Farmer (User)
     */
    private void validateCropPlantingOwnership(CropPlanting cropPlanting, UUID userId) {
        if (!cropPlanting.getCropCycle().getFarm().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this crop planting.");
        }
    }
}
