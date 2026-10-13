package com.krishi.backend.treatment.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.treatment.dto.TreatmentPlanRequest;
import com.krishi.backend.treatment.dto.TreatmentResponse;
import com.krishi.backend.treatment.service.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TreatmentController {

    private final TreatmentService treatmentService;

    @PostMapping("/api/v1/treatments")
    public ResponseEntity<ApiResponse<TreatmentResponse>> createTreatment(
            @Valid @RequestBody TreatmentPlanRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TreatmentResponse response = treatmentService.createTreatment(request, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TreatmentResponse>builder()
                        .success(true)
                        .message("Treatment plan created successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/treatments/{id}/start")
    public ResponseEntity<ApiResponse<TreatmentResponse>> startTreatment(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TreatmentResponse response = treatmentService.startTreatment(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TreatmentResponse>builder()
                        .success(true)
                        .message("Treatment marked as in progress")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/treatments/{id}/complete")
    public ResponseEntity<ApiResponse<TreatmentResponse>> completeTreatment(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TreatmentResponse response = treatmentService.completeTreatment(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TreatmentResponse>builder()
                        .success(true)
                        .message("Treatment completed successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/treatments/{id}")
    public ResponseEntity<ApiResponse<TreatmentResponse>> getTreatmentById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TreatmentResponse response = treatmentService.getTreatmentById(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TreatmentResponse>builder()
                        .success(true)
                        .message("Treatment details retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/disease-incidents/{incidentId}/treatments")
    public ResponseEntity<ApiResponse<List<TreatmentResponse>>> getTreatmentsByIncident(
            @PathVariable UUID incidentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<TreatmentResponse> response = treatmentService.getTreatmentsByIncident(incidentId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<TreatmentResponse>>builder()
                        .success(true)
                        .message("Treatments listed successfully for the disease incident")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/treatments")
    public ResponseEntity<ApiResponse<List<TreatmentResponse>>> getAllTreatments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<TreatmentResponse> response = treatmentService.getAllTreatmentsForUser(userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<TreatmentResponse>>builder()
                        .success(true)
                        .message("Treatments listed successfully for farmer")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
