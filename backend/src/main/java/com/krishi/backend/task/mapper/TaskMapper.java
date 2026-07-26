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

        return TaskResponse.builder()
                .id(task.getId())
                .cropPlantingId(task.getCropPlanting() != null ? task.getCropPlanting().getId() : null)
                .treatmentId(task.getTreatment() != null ? task.getTreatment().getId() : null)
                .taskType(task.getTaskType())
                .source(task.getSource())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
