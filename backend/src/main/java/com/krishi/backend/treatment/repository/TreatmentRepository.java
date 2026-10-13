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

    /**
     * Find all treatments owned by a farmer user, eager loading the hierarchy
     * to eliminate N+1 queries.
     *
     * @param ownerId the UUID of the farm owner (farmer user)
     * @return a list of treatments
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Treatment t " +
            "JOIN FETCH t.diseaseIncident di " +
            "JOIN FETCH di.cropPlanting cp " +
            "JOIN FETCH cp.cropCycle cc " +
            "JOIN FETCH cc.farm f " +
            "WHERE f.owner.id = :ownerId " +
            "ORDER BY t.createdAt DESC")
    List<Treatment> findByFarmerUserId(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId);
}
