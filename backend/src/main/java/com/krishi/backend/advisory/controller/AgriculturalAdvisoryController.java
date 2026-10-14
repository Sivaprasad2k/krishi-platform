package com.krishi.backend.advisory.controller;

import com.krishi.backend.advisory.dto.AdvisoryEvaluationSummaryResponse;
import com.krishi.backend.advisory.dto.AgriculturalAdvisoryResponse;
import com.krishi.backend.advisory.entity.AdvisoryStatus;
import com.krishi.backend.advisory.service.AgriculturalAdvisoryService;
import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller exposing REST endpoints for the Agricultural Advisory Rules Engine.
 * All endpoints enforce ROLE_FARMER and verified farm ownership.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FARMER')")
public class AgriculturalAdvisoryController {

    private final AgriculturalAdvisoryService advisoryService;

    /**
     * Triggers deterministic rules evaluation for a crop planting and generates/updates advisories.
     */
    @PostMapping("/crop-plantings/{plantingId}/advisories/evaluate")
    public ResponseEntity<ApiResponse<AdvisoryEvaluationSummaryResponse>> evaluateAdvisories(
            @PathVariable UUID plantingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AdvisoryEvaluationSummaryResponse response = advisoryService.evaluateAdvisories(plantingId, userDetails);
        return ResponseEntity.ok(
                ApiResponse.<AdvisoryEvaluationSummaryResponse>builder()
                        .success(true)
                        .message("Agricultural advisories evaluated successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * Lists agricultural advisories for a crop planting, optionally filtered by status.
     */
    @GetMapping("/crop-plantings/{plantingId}/advisories")
    public ResponseEntity<ApiResponse<List<AgriculturalAdvisoryResponse>>> getAdvisories(
            @PathVariable UUID plantingId,
            @RequestParam(required = false) AdvisoryStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<AgriculturalAdvisoryResponse> advisories = advisoryService.getAdvisories(plantingId, status, userDetails);
        return ResponseEntity.ok(
                ApiResponse.<List<AgriculturalAdvisoryResponse>>builder()
                        .success(true)
                        .message("Agricultural advisories retrieved successfully")
                        .data(advisories)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * Acknowledges an active advisory.
     */
    @PatchMapping("/advisories/{id}/acknowledge")
    public ResponseEntity<ApiResponse<AgriculturalAdvisoryResponse>> acknowledgeAdvisory(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AgriculturalAdvisoryResponse response = advisoryService.acknowledgeAdvisory(id, userDetails);
        return ResponseEntity.ok(
                ApiResponse.<AgriculturalAdvisoryResponse>builder()
                        .success(true)
                        .message("Agricultural advisory acknowledged successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * Manually resolves an advisory.
     */
    @PatchMapping("/advisories/{id}/resolve")
    public ResponseEntity<ApiResponse<AgriculturalAdvisoryResponse>> resolveAdvisory(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AgriculturalAdvisoryResponse response = advisoryService.resolveAdvisory(id, userDetails);
        return ResponseEntity.ok(
                ApiResponse.<AgriculturalAdvisoryResponse>builder()
                        .success(true)
                        .message("Agricultural advisory resolved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
