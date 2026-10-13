package com.krishi.backend.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskRescheduleRequest {

    @NotNull(message = "New due date is required")
    private LocalDateTime dueDate;
}
