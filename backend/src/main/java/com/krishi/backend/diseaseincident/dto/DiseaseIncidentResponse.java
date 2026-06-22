package com.krishi.backend.diseaseincident.dto;

import com.krishi.backend.diseaseincident.entity.DiseaseIncidentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DiseaseIncidentResponse {
    private UUID id;
    private UUID cropPlantingId;
    private String diseaseName;
    private String description;
    private DiseaseIncidentStatus status;
    private LocalDateTime reportedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime treatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
