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

        return TreatmentResponse.builder()
                .id(treatment.getId())
                .diseaseIncidentId(treatment.getDiseaseIncident().getId())
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
