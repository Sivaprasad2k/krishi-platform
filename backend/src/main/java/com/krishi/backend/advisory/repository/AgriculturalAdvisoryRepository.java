package com.krishi.backend.advisory.repository;

import com.krishi.backend.advisory.entity.AdvisoryStatus;
import com.krishi.backend.advisory.entity.AgriculturalAdvisory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgriculturalAdvisoryRepository extends JpaRepository<AgriculturalAdvisory, UUID> {

    Optional<AgriculturalAdvisory> findByAdvisoryFingerprintAndStatus(String fingerprint, AdvisoryStatus status);

    Optional<AgriculturalAdvisory> findFirstByAdvisoryFingerprintOrderByCreatedAtDesc(String fingerprint);

    List<AgriculturalAdvisory> findByCropPlantingIdOrderByCreatedAtDesc(UUID cropPlantingId);

    List<AgriculturalAdvisory> findByCropPlantingIdAndStatusOrderByCreatedAtDesc(UUID cropPlantingId, AdvisoryStatus status);

    List<AgriculturalAdvisory> findByCropPlantingIdAndStatusInOrderByCreatedAtDesc(UUID cropPlantingId, Collection<AdvisoryStatus> statuses);

    List<AgriculturalAdvisory> findByFarmIdAndStatus(UUID farmId, AdvisoryStatus status);

    List<AgriculturalAdvisory> findByFarmIdAndExpiresAtBeforeAndStatus(UUID farmId, OffsetDateTime cutoff, AdvisoryStatus status);

    List<AgriculturalAdvisory> findByTaskIdAndStatusIn(UUID taskId, Collection<AdvisoryStatus> statuses);
}
