package com.krishi.backend.cropplanting.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.dto.CropPlantingResponse;
import com.krishi.backend.cropplanting.dto.CropPlantingUpdateRequest;
import com.krishi.backend.cropplanting.dto.CropWorkspaceResponse;
import com.krishi.backend.cropplanting.service.CropPlantingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.krishi.backend.cropplanting.dto.context.AgriculturalContextResponse;
import com.krishi.backend.cropplanting.service.AgriculturalContextService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CropPlantingController {

    private final CropPlantingService cropPlantingService;
    private final AgriculturalContextService agriculturalContextService;

    @PostMapping("/api/v1/crop-plantings")
    public ResponseEntity<ApiResponse<CropPlantingResponse>> createCropPlanting(
            @Valid @RequestBody CropPlantingCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropPlantingResponse response = cropPlantingService.createCropPlanting(request, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropPlantingResponse>builder()
                        .success(true)
                        .message("Crop planting recorded successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PutMapping("/api/v1/crop-plantings/{id}")
    public ResponseEntity<ApiResponse<CropPlantingResponse>> updateCropPlanting(
            @PathVariable UUID id,
            @Valid @RequestBody CropPlantingUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropPlantingResponse response = cropPlantingService.updateCropPlanting(id, request, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropPlantingResponse>builder()
                        .success(true)
                        .message("Crop planting updated successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/crop-plantings/{id}")
    public ResponseEntity<ApiResponse<CropPlantingResponse>> getCropPlantingById(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropPlantingResponse response = cropPlantingService.getCropPlantingById(id, asOfDate, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropPlantingResponse>builder()
                        .success(true)
                        .message("Crop planting details retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/crop-cycles/{cropCycleId}/plantings")
    public ResponseEntity<ApiResponse<List<CropPlantingResponse>>> getPlantingsByCropCycle(
            @PathVariable UUID cropCycleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<CropPlantingResponse> response = cropPlantingService.getPlantingsByCropCycle(cropCycleId, asOfDate, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<CropPlantingResponse>>builder()
                        .success(true)
                        .message("Crop plantings listed successfully for the crop cycle")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/crop-plantings/{plantingId}/workspace")
    public ResponseEntity<ApiResponse<CropWorkspaceResponse>> getCropWorkspace(
            @PathVariable UUID plantingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropWorkspaceResponse response = cropPlantingService.getCropWorkspace(plantingId, asOfDate, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropWorkspaceResponse>builder()
                        .success(true)
                        .message("Crop workspace details retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping({"/api/v1/crop-plantings/{plantingId}/context", "/api/v1/crop-plantings/{plantingId}/agricultural-context"})
    public ResponseEntity<ApiResponse<AgriculturalContextResponse>> getAgriculturalContext(
            @PathVariable UUID plantingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AgriculturalContextResponse response = agriculturalContextService.getAgriculturalContext(plantingId, asOfDate, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<AgriculturalContextResponse>builder()
                        .success(true)
                        .message("Agricultural context retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
