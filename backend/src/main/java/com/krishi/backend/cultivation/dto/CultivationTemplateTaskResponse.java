package com.krishi.backend.cultivation.dto;

import com.krishi.backend.task.entity.TaskType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CultivationTemplateTaskResponse {
    private UUID id;
    private UUID stageId;
    private String stageName;
    private TaskType taskType;
    private String title;
    private String description;
    private Integer dayOffset;
    private Integer windowDaysBefore;
    private Integer windowDaysAfter;
    private Integer taskOrder;
    private Boolean mandatory;
}
