package com.krishi.backend.dashboard.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.dashboard.dto.DashboardSummary;
import com.krishi.backend.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> getSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DashboardSummary summary = dashboardService.getSummary(userDetails);

        return ResponseEntity.ok(
                ApiResponse.<DashboardSummary>builder()
                        .success(true)
                        .message("Dashboard summary retrieved successfully")
                        .data(summary)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
