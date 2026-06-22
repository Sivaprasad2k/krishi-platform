package com.krishi.backend.notification.mapper;

import com.krishi.backend.notification.dto.NotificationResponse;
import com.krishi.backend.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    /**
     * Map a Notification entity to a NotificationResponse DTO.
     *
     * @param notification the Notification entity
     * @return the mapped NotificationResponse DTO
     */
    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
