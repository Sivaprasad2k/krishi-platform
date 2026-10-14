package com.krishi.backend.cropplanting.mapper;

import com.krishi.backend.cropplanting.dto.CropPlantingResponse;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import org.springframework.stereotype.Component;

@Component
public class CropPlantingMapper {

    /**
     * Map a CropPlanting entity to a CropPlantingResponse DTO without growth-stage estimate.
     *
     * @param planting the CropPlanting entity
     * @return the mapped CropPlantingResponse DTO
     */
    public CropPlantingResponse toResponse(CropPlanting planting) {
        return toResponse(planting, null);
    }

    /**
     * Map a CropPlanting entity and its calculated EstimatedGrowthStageResponse to a CropPlantingResponse DTO.
     *
     * @param planting             the CropPlanting entity
     * @param estimatedGrowthStage the resolved estimated growth stage
     * @return the mapped CropPlantingResponse DTO
     */
    public CropPlantingResponse toResponse(CropPlanting planting, EstimatedGrowthStageResponse estimatedGrowthStage) {
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
                .estimatedGrowthStage(estimatedGrowthStage)
                .createdAt(planting.getCreatedAt())
                .updatedAt(planting.getUpdatedAt())
                .build();
    }
}
