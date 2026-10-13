package com.krishi.backend.weather.repository;

import com.krishi.backend.weather.entity.AdvisoryStatus;
import com.krishi.backend.weather.entity.WeatherAdvisory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeatherAdvisoryRepository extends JpaRepository<WeatherAdvisory, UUID> {

    Optional<WeatherAdvisory> findByAdvisoryFingerprintAndStatus(String advisoryFingerprint, AdvisoryStatus status);

    Optional<WeatherAdvisory> findFirstByAdvisoryFingerprintOrderByCreatedAtDesc(String advisoryFingerprint);

    List<WeatherAdvisory> findByFarmIdAndStatus(UUID farmId, AdvisoryStatus status);

    List<WeatherAdvisory> findByFarmIdAndStatusInOrderByTargetDateAsc(UUID farmId, List<AdvisoryStatus> statuses);

    List<WeatherAdvisory> findByTaskIdAndStatus(UUID taskId, AdvisoryStatus status);

    List<WeatherAdvisory> findByFarmIdAndTargetDateBeforeAndStatus(UUID farmId, LocalDate cutoffDate, AdvisoryStatus status);
}
