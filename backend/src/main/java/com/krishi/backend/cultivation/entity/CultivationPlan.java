package com.krishi.backend.cultivation.entity;

import com.krishi.backend.cropplanting.entity.CropPlanting;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cultivation_plans", schema = "krishi")
public class CultivationPlan {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_planting_id", nullable = false)
    private CropPlanting cropPlanting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private CultivationTemplate template;

    @Column(name = "template_version", nullable = false)
    private String templateVersion;

    @Column(name = "plan_version", nullable = false)
    private Integer planVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CultivationPlanStatus status;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
