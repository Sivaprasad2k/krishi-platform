package com.krishi.backend.task.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class TaskRescheduleRequest {

    @NotNull(message = "New due date is required")
    private LocalDateTime dueDate;

    @JsonSetter("dueDate")
    public void setDueDateFromJson(Object value) {
        if (value == null) {
            this.dueDate = null;
        } else if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.length() == 10) {
                this.dueDate = LocalDate.parse(trimmed).atTime(8, 0, 0);
            } else {
                this.dueDate = LocalDateTime.parse(trimmed);
            }
        } else if (value instanceof LocalDateTime ldt) {
            this.dueDate = ldt;
        }
    }
}
