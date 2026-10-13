package com.krishi.backend.cropplanting.repository;

import com.krishi.backend.cropplanting.entity.CropPlanting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CropPlantingRepository extends JpaRepository<CropPlanting, UUID> {

    /**
     * Find all crop plantings associated with a specific crop cycle.
     *
     * @param cropCycleId the UUID of the crop cycle
     * @return a list of crop plantings
     */
    List<CropPlanting> findByCropCycleId(UUID cropCycleId);

    List<CropPlanting> findByCropCycleFarmIdAndCropCycleStatus(UUID farmId, com.krishi.backend.cropcycle.entity.CropCycleStatus status);
}
