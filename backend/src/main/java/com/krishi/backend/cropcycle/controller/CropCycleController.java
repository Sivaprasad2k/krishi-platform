package com.krishi.backend.cropcycle.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.dto.CropCycleResponse;
import com.krishi.backend.cropcycle.service.CropCycleService;
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
public class CropCycleController {

    private final CropCycleService cropCycleService;

    @PostMapping("/api/v1/crop-cycles")
    public ResponseEntity<ApiResponse<CropCycleResponse>> createCropCycle(
            @Valid @RequestBody CropCycleCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropCycleResponse response = cropCycleService.createCropCycle(request, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropCycleResponse>builder()
                        .success(true)
                        .message("Crop cycle created in DRAFT status successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/crop-cycles/{id}")
    public ResponseEntity<ApiResponse<CropCycleResponse>> getCropCycleById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropCycleResponse response = cropCycleService.getCropCycleById(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropCycleResponse>builder()
                        .success(true)
                        .message("Crop cycle details retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/farms/{farmId}/crop-cycles")
    public ResponseEntity<ApiResponse<List<CropCycleResponse>>> getCropCyclesByFarm(
            @PathVariable UUID farmId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<CropCycleResponse> response = cropCycleService.getCropCyclesByFarm(farmId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<CropCycleResponse>>builder()
                        .success(true)
                        .message("Crop cycles listed successfully for the farm")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/crop-cycles/{id}/start")
    public ResponseEntity<ApiResponse<CropCycleResponse>> startCropCycle(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropCycleResponse response = cropCycleService.startCropCycle(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropCycleResponse>builder()
                        .success(true)
                        .message("Crop cycle started and moved to ACTIVE status")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/crop-cycles/{id}/complete")
    public ResponseEntity<ApiResponse<CropCycleResponse>> completeCropCycle(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropCycleResponse response = cropCycleService.completeCropCycle(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropCycleResponse>builder()
                        .success(true)
                        .message("Crop cycle completed successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/crop-cycles/{id}/cancel")
    public ResponseEntity<ApiResponse<CropCycleResponse>> cancelCropCycle(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CropCycleResponse response = cropCycleService.cancelCropCycle(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<CropCycleResponse>builder()
                        .success(true)
                        .message("Crop cycle cancelled successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
