package com.krishi.backend.cultivation.mapper;

import com.krishi.backend.cultivation.dto.CultivationPlanResponse;
import com.krishi.backend.cultivation.dto.CultivationStageResponse;
import com.krishi.backend.cultivation.dto.CultivationTemplateResponse;
import com.krishi.backend.cultivation.dto.CultivationTemplateTaskResponse;
import com.krishi.backend.cultivation.entity.CultivationPlan;
import com.krishi.backend.cultivation.entity.CultivationStage;
import com.krishi.backend.cultivation.entity.CultivationTemplate;
import com.krishi.backend.cultivation.entity.CultivationTemplateTask;
import com.krishi.backend.task.dto.TaskResponse;
import com.krishi.backend.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CultivationMapper {

    private final TaskMapper taskMapper;

    public CultivationStageResponse toStageResponse(CultivationStage stage) {
        if (stage == null) {
            return null;
        }
        return CultivationStageResponse.builder()
                .id(stage.getId())
                .stageName(stage.getStageName())
                .stageOrder(stage.getStageOrder())
                .startDayOffset(stage.getStartDayOffset())
                .endDayOffset(stage.getEndDayOffset())
                .description(stage.getDescription())
                .build();
    }

    public CultivationTemplateTaskResponse toTemplateTaskResponse(CultivationTemplateTask task) {
        if (task == null) {
            return null;
        }
        return CultivationTemplateTaskResponse.builder()
                .id(task.getId())
                .stageId(task.getStage() != null ? task.getStage().getId() : null)
                .stageName(task.getStage() != null ? task.getStage().getStageName() : null)
                .taskType(task.getTaskType())
                .title(task.getTitle())
                .description(task.getDescription())
                .dayOffset(task.getDayOffset())
                .windowDaysBefore(task.getWindowDaysBefore())
                .windowDaysAfter(task.getWindowDaysAfter())
                .taskOrder(task.getTaskOrder())
                .mandatory(task.getMandatory())
                .build();
    }

    public CultivationTemplateResponse toTemplateResponse(CultivationTemplate template) {
        if (template == null) {
            return null;
        }
        List<CultivationStageResponse> stages = template.getStages() != null
                ? template.getStages().stream().map(this::toStageResponse).collect(Collectors.toList())
                : Collections.emptyList();

        List<CultivationTemplateTaskResponse> tasks = template.getTemplateTasks() != null
                ? template.getTemplateTasks().stream().map(this::toTemplateTaskResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return CultivationTemplateResponse.builder()
                .id(template.getId())
                .cropName(template.getCropName())
                .cropVariety(template.getCropVariety())
                .season(template.getSeason())
                .regionState(template.getRegionState())
                .regionDistrict(template.getRegionDistrict())
                .durationDays(template.getDurationDays())
                .source(template.getSource())
                .version(template.getVersion())
                .status(template.getStatus())
                .description(template.getDescription())
                .createdAt(template.getCreatedAt())
                .stages(stages)
                .templateTasks(tasks)
                .build();
    }

    public CultivationPlanResponse toPlanResponse(CultivationPlan plan, List<com.krishi.backend.task.entity.Task> tasks) {
        if (plan == null) {
            return null;
        }
        CultivationTemplate template = plan.getTemplate();
        List<CultivationStageResponse> stages = template != null && template.getStages() != null
                ? template.getStages().stream().map(this::toStageResponse).collect(Collectors.toList())
                : Collections.emptyList();

        List<TaskResponse> taskResponses = tasks != null
                ? tasks.stream().map(taskMapper::toResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return CultivationPlanResponse.builder()
                .id(plan.getId())
                .cropPlantingId(plan.getCropPlanting().getId())
                .templateId(template != null ? template.getId() : null)
                .templateCropName(template != null ? template.getCropName() : null)
                .templateCropVariety(template != null ? template.getCropVariety() : null)
                .templateVersion(plan.getTemplateVersion())
                .templateSource(template != null ? template.getSource() : null)
                .durationDays(template != null ? template.getDurationDays() : null)
                .planVersion(plan.getPlanVersion())
                .status(plan.getStatus())
                .generatedAt(plan.getGeneratedAt())
                .createdAt(plan.getCreatedAt())
                .stages(stages)
                .tasks(taskResponses)
                .build();
    }
}
