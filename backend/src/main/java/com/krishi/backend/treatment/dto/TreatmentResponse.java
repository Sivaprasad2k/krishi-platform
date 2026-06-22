package com.krishi.backend.treatment.dto;

import com.krishi.backend.treatment.entity.TreatmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TreatmentResponse {
    private UUID id;
    private UUID diseaseIncidentId;
    private String treatmentName;
    private String description;
    private String recommendedDosage;
    private String applicationMethod;
    private LocalDateTime startDate;
    private LocalDateTime completionDate;
    private TreatmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
