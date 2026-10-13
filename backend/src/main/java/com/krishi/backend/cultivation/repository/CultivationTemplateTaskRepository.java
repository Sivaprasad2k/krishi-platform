package com.krishi.backend.cultivation.repository;

import com.krishi.backend.cultivation.entity.CultivationTemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CultivationTemplateTaskRepository extends JpaRepository<CultivationTemplateTask, UUID> {

    List<CultivationTemplateTask> findByTemplateIdOrderByDayOffsetAscTaskOrderAsc(UUID templateId);
}
