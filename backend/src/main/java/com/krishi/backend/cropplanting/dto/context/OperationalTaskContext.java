package com.krishi.backend.cropplanting.dto.context;

import com.krishi.backend.task.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalTaskContext {
    private UUID taskId;
    private String title;
    private String taskType;
    private TaskStatus status;
    private LocalDate dueDate;
    private boolean overdue;
    private Integer daysOverdue;
    private String stageName;
    private Integer stageOrder;
}
