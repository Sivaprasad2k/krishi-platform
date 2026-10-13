package com.krishi.backend.weather.entity;

import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.task.entity.Task;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "weather_advisories", schema = "krishi")
public class WeatherAdvisory {

    @Id
    private UUID id;

    @Column(name = "advisory_fingerprint", nullable = false, length = 160)
    private String advisoryFingerprint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_planting_id")
    private CropPlanting cropPlanting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvisorySeverity severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "recommended_action", nullable = false, columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvisoryStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_evaluated_at", nullable = false)
    private OffsetDateTime lastEvaluatedAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;
}
