package com.krishi.backend.farm.mapper;

import com.krishi.backend.farm.dto.FarmResponse;
import com.krishi.backend.farm.entity.Farm;
import org.springframework.stereotype.Component;

@Component
public class FarmMapper {

    /**
     * Map a Farm entity to a FarmResponse DTO.
     *
     * @param farm the Farm entity
     * @return the mapped FarmResponse DTO
     */
    public FarmResponse toResponse(Farm farm) {
        if (farm == null) {
            return null;
        }

        return FarmResponse.builder()
                .id(farm.getId())
                .name(farm.getName())
                .sizeHectares(farm.getSizeHectares())
                .location(farm.getLocation())
                .state(farm.getState())
                .district(farm.getDistrict())
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .ownerId(farm.getOwner().getId())
                .status(farm.getStatus())
                .createdAt(farm.getCreatedAt())
                .updatedAt(farm.getUpdatedAt())
                .build();
    }
}
