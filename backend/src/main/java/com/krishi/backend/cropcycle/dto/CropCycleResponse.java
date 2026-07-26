package com.krishi.backend.cropcycle.dto;

import com.krishi.backend.cropcycle.entity.CropCycleStatus;
import com.krishi.backend.cropcycle.entity.SeasonType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CropCycleResponse {
    private UUID id;
    private String name;
    private SeasonType seasonType;
    private Integer year;
    private String notes;
    private CropCycleStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UUID farmId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
