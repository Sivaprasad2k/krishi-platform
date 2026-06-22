package com.krishi.backend.dashboard.service;

import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropcycle.entity.CropCycleStatus;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.dashboard.dto.DashboardSummary;
import com.krishi.backend.diseaseincident.entity.DiseaseIncidentStatus;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.notification.repository.NotificationRepository;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FarmRepository farmRepository;
    private final CropCycleRepository cropCycleRepository;
    private final TaskRepository taskRepository;
    private final DiseaseIncidentRepository diseaseIncidentRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Retrieves the consolidated dashboard summary counts for the authenticated user.
     * Enforces the ROLE_FARMER requirement.
     *
     * @param userDetails the authenticated user details
     * @return a DashboardSummary DTO containing counts
     */
    @Transactional(readOnly = true)
    public DashboardSummary getSummary(CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);
        UUID userId = userDetails.getUserId();

        long farmCount = farmRepository.countByOwnerId(userId);
        
        long activeCropCycles = cropCycleRepository.countByFarmOwnerIdAndStatus(userId, CropCycleStatus.ACTIVE);
        
        long pendingTasks = taskRepository.countPendingTasksByOwner(userId, List.of(
                TaskStatus.PENDING,
                TaskStatus.IN_PROGRESS
        ));
        
        long openProblems = diseaseIncidentRepository.countOpenIncidentsByOwner(userId, List.of(
                DiseaseIncidentStatus.REPORTED,
                DiseaseIncidentStatus.UNDER_REVIEW,
                DiseaseIncidentStatus.TREATED
        ));
        
        long unreadNotifications = notificationRepository.countByUserIdAndIsReadFalse(userId);

        return DashboardSummary.builder()
                .farmCount(farmCount)
                .activeCropCycles(activeCropCycles)
                .pendingTasks(pendingTasks)
                .openProblems(openProblems)
                .unreadNotifications(unreadNotifications)
                .build();
    }

    /**
     * Validates that the user has the FARMER role.
     *
     * @param userDetails the authenticated user details
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
}
