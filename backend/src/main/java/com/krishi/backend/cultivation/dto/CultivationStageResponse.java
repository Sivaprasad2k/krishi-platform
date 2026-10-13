package com.krishi.backend.cultivation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CultivationStageResponse {
    private UUID id;
    private String stageName;
    private Integer stageOrder;
    private Integer startDayOffset;
    private Integer endDayOffset;
    private String description;
}
