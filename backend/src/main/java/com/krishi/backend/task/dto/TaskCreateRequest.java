package com.krishi.backend.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TaskCreateRequest {

    @NotNull(message = "Treatment ID is required")
    private UUID treatmentId;

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    private LocalDateTime dueDate;
}
