package com.krishi.backend.cultivation.repository;

import com.krishi.backend.cultivation.entity.CultivationTemplate;
import com.krishi.backend.cultivation.entity.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CultivationTemplateRepository extends JpaRepository<CultivationTemplate, UUID> {

    List<CultivationTemplate> findByStatusOrderByCropNameAsc(TemplateStatus status);

    List<CultivationTemplate> findByCropNameIgnoreCaseAndStatus(String cropName, TemplateStatus status);

    Optional<CultivationTemplate> findByIdAndStatus(UUID id, TemplateStatus status);
}
