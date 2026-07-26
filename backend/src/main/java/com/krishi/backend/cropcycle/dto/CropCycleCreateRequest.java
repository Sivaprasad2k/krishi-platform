package com.krishi.backend.cropcycle.dto;

import com.krishi.backend.cropcycle.entity.SeasonType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CropCycleCreateRequest {

    private String name;

    @NotNull(message = "Season type is required")
    private SeasonType seasonType;

    @NotNull(message = "Year is required")
    private Integer year;

    private String notes;

    @NotNull(message = "Farm ID is required")
    private UUID farmId;
}
