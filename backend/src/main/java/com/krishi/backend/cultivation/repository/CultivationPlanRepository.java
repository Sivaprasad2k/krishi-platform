package com.krishi.backend.cultivation.repository;

import com.krishi.backend.cultivation.entity.CultivationPlan;
import com.krishi.backend.cultivation.entity.CultivationPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CultivationPlanRepository extends JpaRepository<CultivationPlan, UUID> {

    Optional<CultivationPlan> findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(UUID cropPlantingId, CultivationPlanStatus status);

    List<CultivationPlan> findByCropPlantingIdOrderByPlanVersionDesc(UUID cropPlantingId);

    List<CultivationPlan> findByCropPlantingIdAndStatus(UUID cropPlantingId, CultivationPlanStatus status);
}
