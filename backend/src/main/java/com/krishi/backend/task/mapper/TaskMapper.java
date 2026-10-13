package com.krishi.backend.task.mapper;

import com.krishi.backend.task.dto.TaskResponse;
import com.krishi.backend.task.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    /**
     * Map a Task entity to a TaskResponse DTO safely.
     *
     * @param task the Task entity
     * @return the mapped TaskResponse DTO
     */
    public TaskResponse toResponse(Task task) {
        if (task == null) {
            return null;
        }

        String cropName = null;
        String cropVariety = null;
        java.util.UUID farmId = null;
        String farmName = null;
        if (task.getCropPlanting() != null) {
            cropName = task.getCropPlanting().getCropName();
            cropVariety = task.getCropPlanting().getCropVariety();
            if (task.getCropPlanting().getCropCycle() != null && task.getCropPlanting().getCropCycle().getFarm() != null) {
                farmId = task.getCropPlanting().getCropCycle().getFarm().getId();
                farmName = task.getCropPlanting().getCropCycle().getFarm().getName();
            }
        }

        return TaskResponse.builder()
                .id(task.getId())
                .cropPlantingId(task.getCropPlanting() != null ? task.getCropPlanting().getId() : null)
                .cropName(cropName)
                .cropVariety(cropVariety)
                .farmId(farmId)
                .farmName(farmName)
                .treatmentId(task.getTreatment() != null ? task.getTreatment().getId() : null)
                .cultivationPlanId(task.getCultivationPlan() != null ? task.getCultivationPlan().getId() : null)
                .templateTaskId(task.getTemplateTask() != null ? task.getTemplateTask().getId() : null)
                .taskType(task.getTaskType())
                .source(task.getSource())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .status(task.getStatus())
                .skipReason(task.getSkipReason())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
