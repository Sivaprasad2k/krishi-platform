package com.krishi.backend.cultivation.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.cultivation.dto.CultivationTemplateResponse;
import com.krishi.backend.cultivation.service.CultivationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cultivation-templates")
@RequiredArgsConstructor
public class CultivationTemplateController {

    private final CultivationTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CultivationTemplateResponse>>> getPublishedTemplates(
            @RequestParam(required = false) String cropName) {

        List<CultivationTemplateResponse> templates = templateService.getPublishedTemplates(cropName);

        return ResponseEntity.ok(
                ApiResponse.<List<CultivationTemplateResponse>>builder()
                        .success(true)
                        .message("Published cultivation templates retrieved successfully")
                        .data(templates)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CultivationTemplateResponse>> getTemplateById(@PathVariable UUID id) {
        CultivationTemplateResponse template = templateService.getTemplateById(id);

        return ResponseEntity.ok(
                ApiResponse.<CultivationTemplateResponse>builder()
                        .success(true)
                        .message("Cultivation template details retrieved successfully")
                        .data(template)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
