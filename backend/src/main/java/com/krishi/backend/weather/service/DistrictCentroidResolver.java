package com.krishi.backend.weather.service;

import com.krishi.backend.common.exception.LocationUnavailableException;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.weather.dto.CoordinateSource;
import com.krishi.backend.weather.dto.ResolvedLocation;
import com.krishi.backend.weather.entity.DistrictCoordinate;
import com.krishi.backend.weather.repository.DistrictCoordinateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistrictCentroidResolver {

    private final DistrictCoordinateRepository districtCoordinateRepository;

    public ResolvedLocation resolveLocation(Farm farm) {
        if (farm.getLatitude() != null && farm.getLongitude() != null) {
            double lat = farm.getLatitude();
            double lng = farm.getLongitude();
            double normLat = roundToTwoDecimals(lat);
            double normLng = roundToTwoDecimals(lng);
            String label = buildLabel(farm.getDistrict(), farm.getState(), farm.getName());

            return ResolvedLocation.builder()
                    .latitude(lat)
                    .longitude(lng)
                    .coordinateSource(CoordinateSource.FARM_GPS)
                    .locationLabel(label)
                    .normalizedLatitude(normLat)
                    .normalizedLongitude(normLng)
                    .gridKey("OPEN_METEO:" + normLat + ":" + normLng)
                    .build();
        }

        // GPS is not set, try district fallback
        String state = farm.getState();
        String district = farm.getDistrict();

        if (district == null || district.trim().isEmpty()) {
            throw new LocationUnavailableException(
                    "Farm coordinates and district are not configured. Please set farm GPS coordinates or a recognized district."
            );
        }

        Optional<DistrictCoordinate> districtMatch = Optional.empty();
        if (state != null && !state.trim().isEmpty()) {
            districtMatch = districtCoordinateRepository.findByStateIgnoreCaseAndDistrictIgnoreCase(state.trim(), district.trim());
        }
        if (districtMatch.isEmpty()) {
            districtMatch = districtCoordinateRepository.findFirstByDistrictIgnoreCase(district.trim());
        }

        if (districtMatch.isEmpty()) {
            throw new LocationUnavailableException(
                    "No verified coordinates available for district '" + district + "'"
                            + (state != null ? " in state '" + state + "'" : "")
                            + ". Please specify farm GPS coordinates."
            );
        }

        DistrictCoordinate dc = districtMatch.get();
        double normLat = roundToTwoDecimals(dc.getLatitude());
        double normLng = roundToTwoDecimals(dc.getLongitude());
        String label = dc.getDistrict() + ", " + dc.getState();

        return ResolvedLocation.builder()
                .latitude(dc.getLatitude())
                .longitude(dc.getLongitude())
                .coordinateSource(CoordinateSource.DISTRICT_CENTROID)
                .locationLabel(label)
                .normalizedLatitude(normLat)
                .normalizedLongitude(normLng)
                .gridKey("OPEN_METEO:" + normLat + ":" + normLng)
                .build();
    }

    public double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String buildLabel(String district, String state, String farmName) {
        if (district != null && !district.trim().isEmpty() && state != null && !state.trim().isEmpty()) {
            return district.trim() + ", " + state.trim();
        } else if (district != null && !district.trim().isEmpty()) {
            return district.trim();
        } else if (farmName != null && !farmName.trim().isEmpty()) {
            return farmName.trim();
        }
        return "Farm Location";
    }
}
