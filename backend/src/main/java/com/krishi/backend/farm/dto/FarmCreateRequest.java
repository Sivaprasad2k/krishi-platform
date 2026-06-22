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
}
