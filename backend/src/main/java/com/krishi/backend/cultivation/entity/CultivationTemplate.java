package com.krishi.backend.cultivation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cultivation_templates", schema = "krishi")
public class CultivationTemplate {

    @Id
    private UUID id;

    @Column(name = "crop_name", nullable = false)
    private String cropName;

    @Column(name = "crop_variety")
    private String cropVariety;

    @Column(name = "season")
    private String season;

    @Column(name = "region_state")
    private String regionState;

    @Column(name = "region_district")
    private String regionDistrict;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "version", nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TemplateStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    private List<CultivationStage> stages = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOffset ASC, taskOrder ASC")
    private List<CultivationTemplateTask> templateTasks = new ArrayList<>();
}
