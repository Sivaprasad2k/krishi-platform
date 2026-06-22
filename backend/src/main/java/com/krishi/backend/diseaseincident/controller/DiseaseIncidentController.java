package com.krishi.backend.diseaseincident.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentReportRequest;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentResponse;
import com.krishi.backend.diseaseincident.service.DiseaseIncidentService;
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
public class DiseaseIncidentController {

    private final DiseaseIncidentService diseaseIncidentService;

    @PostMapping("/api/v1/disease-incidents")
    public ResponseEntity<ApiResponse<DiseaseIncidentResponse>> reportIncident(
            @Valid @RequestBody DiseaseIncidentReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DiseaseIncidentResponse response = diseaseIncidentService.reportIncident(request, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<DiseaseIncidentResponse>builder()
                        .success(true)
                        .message("Crop disease incident reported successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/disease-incidents/{id}/review")
    public ResponseEntity<ApiResponse<DiseaseIncidentResponse>> reviewIncident(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DiseaseIncidentResponse response = diseaseIncidentService.reviewIncident(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<DiseaseIncidentResponse>builder()
                        .success(true)
                        .message("Disease incident marked under review")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/disease-incidents/{id}/treat")
    public ResponseEntity<ApiResponse<DiseaseIncidentResponse>> treatIncident(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DiseaseIncidentResponse response = diseaseIncidentService.treatIncident(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<DiseaseIncidentResponse>builder()
                        .success(true)
                        .message("Disease incident marked as treated")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/disease-incidents/{id}/close")
    public ResponseEntity<ApiResponse<DiseaseIncidentResponse>> closeIncident(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DiseaseIncidentResponse response = diseaseIncidentService.closeIncident(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<DiseaseIncidentResponse>builder()
                        .success(true)
                        .message("Disease incident closed successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/disease-incidents/{id}")
    public ResponseEntity<ApiResponse<DiseaseIncidentResponse>> getIncidentById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DiseaseIncidentResponse response = diseaseIncidentService.getIncidentById(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<DiseaseIncidentResponse>builder()
                        .success(true)
                        .message("Disease incident details retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/crop-plantings/{plantingId}/disease-incidents")
    public ResponseEntity<ApiResponse<List<DiseaseIncidentResponse>>> getIncidentsByPlanting(
            @PathVariable UUID plantingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<DiseaseIncidentResponse> response = diseaseIncidentService.getIncidentsByPlanting(plantingId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<DiseaseIncidentResponse>>builder()
                        .success(true)
                        .message("Disease incidents listed successfully for the crop planting")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
