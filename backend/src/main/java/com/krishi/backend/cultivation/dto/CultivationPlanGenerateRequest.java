package com.krishi.backend.cultivation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CultivationPlanGenerateRequest {

    @NotNull(message = "Template ID is required")
    private UUID templateId;
}
