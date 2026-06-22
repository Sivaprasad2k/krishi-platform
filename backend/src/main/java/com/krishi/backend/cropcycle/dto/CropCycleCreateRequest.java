package com.krishi.backend.cropcycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CropCycleCreateRequest {

    @NotBlank(message = "Crop cycle name is required")
    private String name;

    @NotBlank(message = "Crop name is required")
    private String cropName;

    @NotNull(message = "Farm ID is required")
    private UUID farmId;
}
