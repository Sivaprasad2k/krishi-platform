package com.krishi.backend.cropplanting.entity;

import com.krishi.backend.cropcycle.entity.CropCycle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "crop_plantings", schema = "krishi")
public class CropPlanting {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_cycle_id", nullable = false)
    private CropCycle cropCycle;

    @Column(name = "crop_name", nullable = false)
    private String cropName;

    @Column(name = "crop_variety")
    private String cropVariety;

    @Column(name = "planting_date", nullable = false)
    private LocalDateTime plantingDate;

    @Column(name = "expected_harvest_date")
    private LocalDateTime expectedHarvestDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
