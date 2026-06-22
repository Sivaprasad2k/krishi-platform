package com.krishi.backend.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardSummary {
    private long farmCount;
    private long activeCropCycles;
    private long pendingTasks;
    private long openProblems;
    private long unreadNotifications;
}
