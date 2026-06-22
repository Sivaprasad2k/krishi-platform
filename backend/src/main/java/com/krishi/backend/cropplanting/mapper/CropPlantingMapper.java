package com.krishi.backend.cropplanting.mapper;

import com.krishi.backend.cropplanting.dto.CropPlantingResponse;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import org.springframework.stereotype.Component;

@Component
public class CropPlantingMapper {

    /**
     * Map a CropPlanting entity to a CropPlantingResponse DTO.
     *
     * @param planting the CropPlanting entity
     * @return the mapped CropPlantingResponse DTO
     */
    public CropPlantingResponse toResponse(CropPlanting planting) {
        if (planting == null) {
            return null;
        }

        return CropPlantingResponse.builder()
                .id(planting.getId())
                .cropCycleId(planting.getCropCycle().getId())
                .cropName(planting.getCropName())
                .cropVariety(planting.getCropVariety())
                .plantingDate(planting.getPlantingDate())
                .expectedHarvestDate(planting.getExpectedHarvestDate())
                .createdAt(planting.getCreatedAt())
                .updatedAt(planting.getUpdatedAt())
                .build();
    }
}
