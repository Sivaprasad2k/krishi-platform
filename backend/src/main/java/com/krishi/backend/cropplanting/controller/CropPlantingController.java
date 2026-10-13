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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CropPlantingController {

    private final CropPlantingService cropPlantingService;

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
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropPlantingResponse response = cropPlantingService.getCropPlantingById(id, userDetails);

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
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<CropPlantingResponse> response = cropPlantingService.getPlantingsByCropCycle(cropCycleId, userDetails);

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
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropWorkspaceResponse response = cropPlantingService.getCropWorkspace(plantingId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropWorkspaceResponse>builder()
                        .success(true)
                        .message("Crop workspace details retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
