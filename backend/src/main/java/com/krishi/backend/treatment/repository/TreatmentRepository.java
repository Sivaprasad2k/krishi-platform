package com.krishi.backend.treatment.repository;

import com.krishi.backend.treatment.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {

    /**
     * Find all treatments associated with a specific disease incident.
     *
     * @param diseaseIncidentId the UUID of the disease incident
     * @return a list of treatments
     */
    List<Treatment> findByDiseaseIncidentId(UUID diseaseIncidentId);

    /**
     * Find all treatments associated with all disease incidents for a specific crop planting.
     *
     * @param cropPlantingId the UUID of the crop planting
     * @return a list of treatments
     */
    List<Treatment> findByDiseaseIncidentCropPlantingId(UUID cropPlantingId);
}
