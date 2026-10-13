package com.krishi.backend.weather.repository;

import com.krishi.backend.weather.entity.DistrictCoordinate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DistrictCoordinateRepository extends JpaRepository<DistrictCoordinate, UUID> {

    Optional<DistrictCoordinate> findByStateIgnoreCaseAndDistrictIgnoreCase(String state, String district);

    Optional<DistrictCoordinate> findFirstByDistrictIgnoreCase(String district);
}
