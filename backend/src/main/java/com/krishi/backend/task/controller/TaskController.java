package com.krishi.backend.task.controller;

import com.krishi.backend.common.api.ApiResponse;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.task.dto.TaskCreateRequest;
import com.krishi.backend.task.dto.TaskResponse;
import com.krishi.backend.task.service.TaskService;
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
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/api/v1/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TaskResponse response = taskService.createTask(request, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TaskResponse>builder()
                        .success(true)
                        .message("Task created successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAllTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<TaskResponse> response = taskService.getAllTasksForUser(userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<TaskResponse>>builder()
                        .success(true)
                        .message("Tasks listed successfully for farmer")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/tasks/{id}/start")
    public ResponseEntity<ApiResponse<TaskResponse>> startTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TaskResponse response = taskService.startTask(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TaskResponse>builder()
                        .success(true)
                        .message("Task started successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/tasks/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TaskResponse response = taskService.completeTask(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TaskResponse>builder()
                        .success(true)
                        .message("Task completed successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/api/v1/tasks/{id}/cancel")
    public ResponseEntity<ApiResponse<TaskResponse>> cancelTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TaskResponse response = taskService.cancelTask(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TaskResponse>builder()
                        .success(true)
                        .message("Task cancelled successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/tasks/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TaskResponse response = taskService.getTaskById(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<TaskResponse>builder()
                        .success(true)
                        .message("Task details retrieved successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/crop-plantings/{cropPlantingId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByCropPlanting(
            @PathVariable UUID cropPlantingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<TaskResponse> response = taskService.getTasksByCropPlanting(cropPlantingId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<TaskResponse>>builder()
                        .success(true)
                        .message("Tasks listed successfully for crop planting")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/api/v1/treatments/{treatmentId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByTreatment(
            @PathVariable UUID treatmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<TaskResponse> response = taskService.getTasksByTreatment(treatmentId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.<List<TaskResponse>>builder()
                        .success(true)
                        .message("Tasks listed successfully for the treatment")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
