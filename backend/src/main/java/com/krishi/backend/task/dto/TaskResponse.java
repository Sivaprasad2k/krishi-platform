package com.krishi.backend.task.dto;

import com.krishi.backend.task.entity.TaskSource;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.entity.TaskType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TaskResponse {
    private UUID id;
    private UUID cropPlantingId;
    private UUID treatmentId;
    private UUID cultivationPlanId;
    private UUID templateTaskId;
    private TaskType taskType;
    private TaskSource source;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private TaskStatus status;
    private String skipReason;
    private String cropName;
    private String cropVariety;
    private UUID farmId;
    private String farmName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
