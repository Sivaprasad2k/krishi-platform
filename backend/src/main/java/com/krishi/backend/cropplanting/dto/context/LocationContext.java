package com.krishi.backend.cropplanting.dto.context;

import com.krishi.backend.weather.dto.CoordinateSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationContext {
    private UUID farmId;
    private String farmName;
    private String locationAddress;
    private String state;
    private String district;
    private Double latitude;
    private Double longitude;
    private CoordinateSource coordinateSource;
    private String locationLabel;
    private UUID plotId;
    private String plotName;
}
