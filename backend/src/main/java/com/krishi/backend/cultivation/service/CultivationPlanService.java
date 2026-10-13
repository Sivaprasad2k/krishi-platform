package com.krishi.backend.cultivation.service;

import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cultivation.dto.CultivationPlanResponse;
import com.krishi.backend.cultivation.entity.*;
import com.krishi.backend.cultivation.mapper.CultivationMapper;
import com.krishi.backend.cultivation.repository.CultivationPlanRepository;
import com.krishi.backend.cultivation.repository.CultivationTemplateRepository;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskSource;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CultivationPlanService {

    private final CropPlantingRepository cropPlantingRepository;
    private final CultivationTemplateRepository templateRepository;
    private final CultivationPlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final CultivationMapper cultivationMapper;

    /**
     * Generates or idempotently returns a cultivation plan for a crop planting.
     */
    @Transactional
    @PreAuthorize("hasRole('FARMER')")
    public CultivationPlanResponse generateCultivationPlan(UUID cropPlantingId, UUID templateId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting cropPlanting = cropPlantingRepository.findById(cropPlantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + cropPlantingId));

        validatePlantingOwnership(cropPlanting, userDetails.getUserId());

        if (cropPlanting.getPlantingDate() == null) {
            throw new IllegalArgumentException("Planting date is required to generate a cultivation plan.");
        }

        CultivationTemplate template = templateRepository.findByIdAndStatus(templateId, TemplateStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Template not found or not published with id: " + templateId));

        // Check for existing active plan
        Optional<CultivationPlan> existingActiveOpt = planRepository
                .findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(cropPlantingId, CultivationPlanStatus.ACTIVE);

        if (existingActiveOpt.isPresent()) {
            CultivationPlan existingPlan = existingActiveOpt.get();

            // IDEMPOTENCY CHECK: Same template -> return existing plan and tasks without generating duplicates
            if (existingPlan.getTemplate().getId().equals(templateId)) {
                List<Task> existingTasks = taskRepository.findByCultivationPlanId(existingPlan.getId());
                if (!existingTasks.isEmpty()) {
                    return cultivationMapper.toPlanResponse(existingPlan, existingTasks);
                }
            }

            // PLAN REVISION: Supersede existing plan while preserving historical completed / in-progress tasks
            existingPlan.setStatus(CultivationPlanStatus.SUPERSEDED);
            existingPlan.setUpdatedAt(LocalDateTime.now());
            existingPlan.setUpdatedBy(userDetails.getUserId());
            planRepository.saveAndFlush(existingPlan);

            // For existing tasks belonging to superseded plan: cancel PENDING, preserve COMPLETED and IN_PROGRESS
            List<Task> previousPlanTasks = taskRepository.findByCultivationPlanId(existingPlan.getId());
            for (Task task : previousPlanTasks) {
                if (task.getStatus() == TaskStatus.PENDING) {
                    task.setStatus(TaskStatus.CANCELLED);
                    task.setUpdatedAt(LocalDateTime.now());
                    task.setUpdatedBy(userDetails.getUserId());
                    taskRepository.save(task);
                }
                // COMPLETED, IN_PROGRESS, SKIPPED remain preserved as history
            }
        }

        int nextVersion = existingActiveOpt.map(p -> p.getPlanVersion() + 1).orElse(1);
        LocalDateTime now = LocalDateTime.now();

        CultivationPlan plan = new CultivationPlan();
        plan.setId(UUID.randomUUID());
        plan.setCropPlanting(cropPlanting);
        plan.setTemplate(template);
        plan.setTemplateVersion(template.getVersion());
        plan.setPlanVersion(nextVersion);
        plan.setStatus(CultivationPlanStatus.ACTIVE);
        plan.setGeneratedAt(now);
        plan.setCreatedAt(now);
        plan.setCreatedBy(userDetails.getUserId());

        CultivationPlan savedPlan = planRepository.save(plan);

        // Update crop planting expected harvest date if not present
        if (cropPlanting.getExpectedHarvestDate() == null && template.getDurationDays() != null) {
            cropPlanting.setExpectedHarvestDate(cropPlanting.getPlantingDate().plusDays(template.getDurationDays()));
            cropPlanting.setUpdatedAt(now);
            cropPlanting.setUpdatedBy(userDetails.getUserId());
            cropPlantingRepository.save(cropPlanting);
        }

        // Generate scheduled tasks based on template activities
        List<Task> generatedTasks = new ArrayList<>();
        if (template.getTemplateTasks() != null) {
            for (CultivationTemplateTask templateTask : template.getTemplateTasks()) {
                LocalDateTime dueDate = cropPlanting.getPlantingDate().plusDays(templateTask.getDayOffset());

                Task task = new Task();
                task.setId(UUID.randomUUID());
                task.setCropPlanting(cropPlanting);
                task.setTreatment(null);
                task.setCultivationPlan(savedPlan);
                task.setTemplateTask(templateTask);
                task.setTaskType(templateTask.getTaskType());
                task.setSource(TaskSource.PLAN);
                task.setTitle(templateTask.getTitle());
                task.setDescription(templateTask.getDescription());
                task.setDueDate(dueDate);
                task.setStatus(TaskStatus.PENDING);
                task.setCreatedAt(now);
                task.setCreatedBy(userDetails.getUserId());

                generatedTasks.add(taskRepository.save(task));
            }
        }

        return cultivationMapper.toPlanResponse(savedPlan, generatedTasks);
    }

    /**
     * Retrieves the active cultivation plan for a specific crop planting.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('FARMER')")
    public CultivationPlanResponse getActivePlan(UUID cropPlantingId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting cropPlanting = cropPlantingRepository.findById(cropPlantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + cropPlantingId));

        validatePlantingOwnership(cropPlanting, userDetails.getUserId());

        Optional<CultivationPlan> activePlanOpt = planRepository
                .findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(cropPlantingId, CultivationPlanStatus.ACTIVE);

        if (activePlanOpt.isEmpty()) {
            return null;
        }

        CultivationPlan activePlan = activePlanOpt.get();
        List<Task> planTasks = taskRepository.findByCultivationPlanId(activePlan.getId());

        return cultivationMapper.toPlanResponse(activePlan, planTasks);
    }

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

    private void validatePlantingOwnership(CropPlanting cropPlanting, UUID userId) {
        if (!cropPlanting.getCropCycle().getFarm().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this crop planting.");
        }
    }
}
