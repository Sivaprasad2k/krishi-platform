package com.krishi.backend.cropcycle.mapper;

import com.krishi.backend.cropcycle.dto.CropCycleResponse;
import com.krishi.backend.cropcycle.entity.CropCycle;
import org.springframework.stereotype.Component;

@Component
public class CropCycleMapper {

    /**
     * Map a CropCycle entity to a CropCycleResponse DTO.
     *
     * @param cropCycle the CropCycle entity
     * @return the mapped CropCycleResponse DTO
     */
    public CropCycleResponse toResponse(CropCycle cropCycle) {
        if (cropCycle == null) {
            return null;
        }

        return CropCycleResponse.builder()
                .id(cropCycle.getId())
                .name(cropCycle.getName())
                .cropName(cropCycle.getCropName())
                .status(cropCycle.getStatus())
                .startDate(cropCycle.getStartDate())
                .endDate(cropCycle.getEndDate())
                .farmId(cropCycle.getFarm().getId())
                .createdAt(cropCycle.getCreatedAt())
                .updatedAt(cropCycle.getUpdatedAt())
                .build();
    }
}
