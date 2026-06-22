package com.krishi.backend.cropplanting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CropPlantingUpdateRequest {

    @NotBlank(message = "Crop name is required")
    private String cropName;

    @NotBlank(message = "Crop variety is required")
    private String cropVariety;

    @NotNull(message = "Planting date is required")
    private LocalDateTime plantingDate;

    private LocalDateTime expectedHarvestDate;
}
