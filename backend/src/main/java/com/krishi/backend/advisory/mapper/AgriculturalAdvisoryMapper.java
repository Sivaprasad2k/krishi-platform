package com.krishi.backend.advisory.mapper;

import com.krishi.backend.advisory.dto.AgriculturalAdvisoryResponse;
import com.krishi.backend.advisory.entity.AgriculturalAdvisory;
import org.springframework.stereotype.Component;

@Component
public class AgriculturalAdvisoryMapper {

    public AgriculturalAdvisoryResponse toResponse(AgriculturalAdvisory advisory) {
        if (advisory == null) {
            return null;
        }

        return AgriculturalAdvisoryResponse.builder()
                .id(advisory.getId())
                .advisoryFingerprint(advisory.getAdvisoryFingerprint())
                .farmId(advisory.getFarm() != null ? advisory.getFarm().getId() : null)
                .cropPlantingId(advisory.getCropPlanting() != null ? advisory.getCropPlanting().getId() : null)
                .cropName(advisory.getCropPlanting() != null ? advisory.getCropPlanting().getCropName() : null)
                .taskId(advisory.getTask() != null ? advisory.getTask().getId() : null)
                .taskTitle(advisory.getTask() != null ? advisory.getTask().getTitle() : null)
                .ruleCode(advisory.getRuleCode())
                .severity(advisory.getSeverity())
                .title(advisory.getTitle())
                .message(advisory.getMessage())
                .recommendedAction(advisory.getRecommendedAction())
                .targetDate(advisory.getTargetDate())
                .status(advisory.getStatus())
                .provenance(advisory.getProvenance())
                .createdAt(advisory.getCreatedAt())
                .lastEvaluatedAt(advisory.getLastEvaluatedAt())
                .acknowledgedAt(advisory.getAcknowledgedAt())
                .resolvedAt(advisory.getResolvedAt())
                .expiresAt(advisory.getExpiresAt())
                .build();
    }
}
