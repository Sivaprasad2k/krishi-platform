package com.krishi.backend.treatment.entity;

import com.krishi.backend.diseaseincident.entity.DiseaseIncident;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "treatments", schema = "krishi")
public class Treatment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_incident_id", nullable = false)
    private DiseaseIncident diseaseIncident;

    @Column(name = "treatment_name", nullable = false)
    private String treatmentName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommended_dosage")
    private String recommendedDosage;

    @Column(name = "application_method")
    private String applicationMethod;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TreatmentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
