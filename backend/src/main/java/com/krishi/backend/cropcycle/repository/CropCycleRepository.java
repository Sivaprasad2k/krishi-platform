package com.krishi.backend.cropcycle.repository;

import com.krishi.backend.cropcycle.entity.CropCycle;
import com.krishi.backend.cropcycle.entity.CropCycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CropCycleRepository extends JpaRepository<CropCycle, UUID> {

    /**
     * Find all crop cycles associated with a specific farm.
     *
     * @param farmId the UUID of the farm
     * @return a list of crop cycles
     */
    List<CropCycle> findByFarmId(UUID farmId);

    long countByFarmOwnerIdAndStatus(UUID ownerId, CropCycleStatus status);
}
