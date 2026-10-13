package com.krishi.backend.cultivation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cultivation_stages", schema = "krishi")
public class CultivationStage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private CultivationTemplate template;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "start_day_offset", nullable = false)
    private Integer startDayOffset;

    @Column(name = "end_day_offset", nullable = false)
    private Integer endDayOffset;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
