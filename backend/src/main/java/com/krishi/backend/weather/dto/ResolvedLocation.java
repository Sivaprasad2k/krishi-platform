package com.krishi.backend.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedLocation {
    private Double latitude;
    private Double longitude;
    private CoordinateSource coordinateSource;
    private String locationLabel;
    private Double normalizedLatitude;
    private Double normalizedLongitude;
    private String gridKey;
}
