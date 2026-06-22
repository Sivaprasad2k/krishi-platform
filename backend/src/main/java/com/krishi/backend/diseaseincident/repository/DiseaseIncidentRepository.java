package com.krishi.backend.diseaseincident.repository;

import com.krishi.backend.diseaseincident.entity.DiseaseIncident;
import com.krishi.backend.diseaseincident.entity.DiseaseIncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiseaseIncidentRepository extends JpaRepository<DiseaseIncident, UUID> {

    /**
     * Find all disease incidents associated with a specific crop planting.
     *
     * @param cropPlantingId the UUID of the crop planting
     * @return a list of disease incidents
     */
    List<DiseaseIncident> findByCropPlantingId(UUID cropPlantingId);

    @Query("SELECT COUNT(di) FROM DiseaseIncident di WHERE di.cropPlanting.cropCycle.farm.owner.id = :ownerId AND di.status IN :statuses")
    long countOpenIncidentsByOwner(@Param("ownerId") UUID ownerId, @Param("statuses") List<DiseaseIncidentStatus> statuses);
}
