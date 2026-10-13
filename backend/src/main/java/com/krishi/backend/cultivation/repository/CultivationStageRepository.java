package com.krishi.backend.cultivation.repository;

import com.krishi.backend.cultivation.entity.CultivationStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CultivationStageRepository extends JpaRepository<CultivationStage, UUID> {

    List<CultivationStage> findByTemplateIdOrderByStageOrderAsc(UUID templateId);
}
