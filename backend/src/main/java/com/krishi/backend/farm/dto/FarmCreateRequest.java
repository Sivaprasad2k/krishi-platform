package com.krishi.backend.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FarmCreateRequest {

    @NotBlank(message = "Farm name is required")
    private String name;

    @NotNull(message = "Farm size is required")
    @Positive(message = "Farm size must be greater than zero")
    private Double sizeHectares;

    @NotBlank(message = "Farm location is required")
    private String location;

    private String state;

    private String district;

    @jakarta.validation.constraints.DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;
}
