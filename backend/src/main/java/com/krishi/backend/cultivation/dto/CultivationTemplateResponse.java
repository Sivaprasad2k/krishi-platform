package com.krishi.backend.cultivation.dto;

import com.krishi.backend.cultivation.entity.TemplateStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CultivationTemplateResponse {
    private UUID id;
    private String cropName;
    private String cropVariety;
    private String season;
    private String regionState;
    private String regionDistrict;
    private Integer durationDays;
    private String source;
    private String version;
    private TemplateStatus status;
    private String description;
    private LocalDateTime createdAt;
    private List<CultivationStageResponse> stages;
    private List<CultivationTemplateTaskResponse> templateTasks;
}
