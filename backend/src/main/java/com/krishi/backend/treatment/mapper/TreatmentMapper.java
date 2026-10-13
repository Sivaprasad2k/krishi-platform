package com.krishi.backend.treatment.mapper;

import com.krishi.backend.treatment.dto.TreatmentResponse;
import com.krishi.backend.treatment.entity.Treatment;
import org.springframework.stereotype.Component;

@Component
public class TreatmentMapper {

    /**
     * Map a Treatment entity to a TreatmentResponse DTO.
     *
     * @param treatment the Treatment entity
     * @return the mapped TreatmentResponse DTO
     */
    public TreatmentResponse toResponse(Treatment treatment) {
        if (treatment == null) {
            return null;
        }

        String diseaseName = null;
        java.util.UUID cropPlantingId = null;
        String cropName = null;
        String cropVariety = null;
        java.util.UUID farmId = null;
        String farmName = null;

        if (treatment.getDiseaseIncident() != null) {
            diseaseName = treatment.getDiseaseIncident().getDiseaseName();
            if (treatment.getDiseaseIncident().getCropPlanting() != null) {
                com.krishi.backend.cropplanting.entity.CropPlanting cp = treatment.getDiseaseIncident().getCropPlanting();
                cropPlantingId = cp.getId();
                cropName = cp.getCropName();
                cropVariety = cp.getCropVariety();
                if (cp.getCropCycle() != null && cp.getCropCycle().getFarm() != null) {
                    farmId = cp.getCropCycle().getFarm().getId();
                    farmName = cp.getCropCycle().getFarm().getName();
                }
            }
        }

        return TreatmentResponse.builder()
                .id(treatment.getId())
                .diseaseIncidentId(treatment.getDiseaseIncident() != null ? treatment.getDiseaseIncident().getId() : null)
                .diseaseName(diseaseName)
                .cropPlantingId(cropPlantingId)
                .cropName(cropName)
                .cropVariety(cropVariety)
                .farmId(farmId)
                .farmName(farmName)
                .treatmentName(treatment.getTreatmentName())
                .description(treatment.getDescription())
                .recommendedDosage(treatment.getRecommendedDosage())
                .applicationMethod(treatment.getApplicationMethod())
                .startDate(treatment.getStartDate())
                .completionDate(treatment.getCompletionDate())
                .status(treatment.getStatus())
                .createdAt(treatment.getCreatedAt())
                .updatedAt(treatment.getUpdatedAt())
                .build();
    }
}
