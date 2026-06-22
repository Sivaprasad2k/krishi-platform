package com.krishi.backend.farm.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.dto.FarmResponse;
import com.krishi.backend.farm.dto.FarmUpdateRequest;
import com.krishi.backend.farm.service.FarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;

    @PostMapping
    public ResponseEntity<ApiResponse<FarmResponse>> createFarm(
            @Valid @RequestBody FarmCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        FarmResponse response = farmService.createFarm(request, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<FarmResponse>builder()
                        .success(true)
                        .message("Farm created successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/{farmId}")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarmById(
            @PathVariable UUID farmId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        FarmResponse response = farmService.getFarmById(farmId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<FarmResponse>builder()
                        .success(true)
                        .message("Farm details retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getMyFarms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<FarmResponse> response = farmService.getMyFarms(userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<FarmResponse>>builder()
                        .success(true)
                        .message("Farms listed successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PutMapping("/{farmId}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarm(
            @PathVariable UUID farmId,
            @Valid @RequestBody FarmUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        FarmResponse response = farmService.updateFarm(farmId, request, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<FarmResponse>builder()
                        .success(true)
                        .message("Farm updated successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
