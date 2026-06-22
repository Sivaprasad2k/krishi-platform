package com.krishi.backend.task.dto;

import com.krishi.backend.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TaskResponse {
    private UUID id;
    private UUID treatmentId;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
