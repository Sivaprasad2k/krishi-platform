package com.krishi.backend.diseaseincident.mapper;

import com.krishi.backend.diseaseincident.dto.DiseaseIncidentResponse;
import com.krishi.backend.diseaseincident.entity.DiseaseIncident;
import org.springframework.stereotype.Component;

@Component
public class DiseaseIncidentMapper {

    /**
     * Map a DiseaseIncident entity to a DiseaseIncidentResponse DTO.
     *
     * @param incident the DiseaseIncident entity
     * @return the mapped DiseaseIncidentResponse DTO
     */
    public DiseaseIncidentResponse toResponse(DiseaseIncident incident) {
        if (incident == null) {
            return null;
        }

        return DiseaseIncidentResponse.builder()
                .id(incident.getId())
                .cropPlantingId(incident.getCropPlanting().getId())
                .diseaseName(incident.getDiseaseName())
                .description(incident.getDescription())
                .status(incident.getStatus())
                .reportedAt(incident.getReportedAt())
                .reviewedAt(incident.getReviewedAt())
                .treatedAt(incident.getTreatedAt())
                .closedAt(incident.getClosedAt())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
