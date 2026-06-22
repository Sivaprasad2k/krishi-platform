package com.krishi.backend.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishi.backend.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private NotificationType notificationType;
    
    @JsonProperty("isRead")
    private boolean isRead;
    
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
