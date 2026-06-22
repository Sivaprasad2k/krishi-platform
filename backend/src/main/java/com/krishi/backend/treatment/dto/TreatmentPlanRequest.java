package com.krishi.backend.treatment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TreatmentPlanRequest {

    @NotNull(message = "Disease incident ID is required")
    private UUID diseaseIncidentId;

    @NotBlank(message = "Treatment name is required")
    private String treatmentName;

    private String description;

    private String recommendedDosage;

    private String applicationMethod;
}
