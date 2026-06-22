package com.krishi.backend.diseaseincident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DiseaseIncidentReportRequest {

    @NotNull(message = "Crop planting ID is required")
    private UUID cropPlantingId;

    @NotBlank(message = "Disease name is required")
    private String diseaseName;

    private String description;
}
