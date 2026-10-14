package com.krishi.backend.notification.service;

import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.notification.dto.NotificationResponse;
import com.krishi.backend.notification.entity.Notification;
import com.krishi.backend.notification.entity.NotificationType;
import com.krishi.backend.notification.mapper.NotificationMapper;
import com.krishi.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    /**
     * Lists all notifications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userDetails.getUserId());
        return list.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lists all unread notifications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);
        List<Notification> list = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userDetails.getUserId());
        return list.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves details of a specific notification, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));

        validateNotificationOwnership(notification, userDetails.getUserId());

        return notificationMapper.toResponse(notification);
    }

    /**
     * Marks a specific notification as read.
     */
    @Transactional
    public NotificationResponse markAsRead(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));

        validateNotificationOwnership(notification, userDetails.getUserId());

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return notificationMapper.toResponse(notification);
    }

    /**
     * Marks all unread notifications for the authenticated user as read.
     */
    @Transactional
    public void markAllAsRead(CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userDetails.getUserId());
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setRead(true);
            n.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
    }

    /**
     * Internal programmatic API used by Event Listeners to persist notifications.
     * This operates as a system activity and does not perform user security checks.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotification(UUID userId, String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    /**
     * Enforces the ROLE_FARMER check.
     */
    private void validateFarmerRole(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Access denied. User is not authenticated.");
        }
        boolean isFarmer = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_FARMER"));
        if (!isFarmer) {
            throw new AccessDeniedException("Access denied. Only farmers are allowed to access this resource.");
        }
    }

    /**
     * Enforces user ownership of notifications.
     */
    private void validateNotificationOwnership(Notification notification, UUID userId) {
        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own this notification.");
        }
    }
}
