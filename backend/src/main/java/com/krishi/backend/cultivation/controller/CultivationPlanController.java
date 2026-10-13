package com.krishi.backend.cultivation.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cultivation.dto.CultivationPlanGenerateRequest;
import com.krishi.backend.cultivation.dto.CultivationPlanResponse;
import com.krishi.backend.cultivation.service.CultivationPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/crop-plantings/{plantingId}/cultivation-plan")
@RequiredArgsConstructor
public class CultivationPlanController {

    private final CultivationPlanService cultivationPlanService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<CultivationPlanResponse>> generatePlan(
            @PathVariable UUID plantingId,
            @Valid @RequestBody CultivationPlanGenerateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CultivationPlanResponse response = cultivationPlanService.generateCultivationPlan(
                plantingId, request.getTemplateId(), userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CultivationPlanResponse>builder()
                        .success(true)
                        .message("Cultivation plan generated successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CultivationPlanResponse>> getActivePlan(
            @PathVariable UUID plantingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CultivationPlanResponse response = cultivationPlanService.getActivePlan(plantingId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CultivationPlanResponse>builder()
                        .success(true)
                        .message("Active cultivation plan retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
