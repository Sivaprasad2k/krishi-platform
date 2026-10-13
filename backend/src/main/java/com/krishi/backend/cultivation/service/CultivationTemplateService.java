package com.krishi.backend.cultivation.service;

import com.krishi.backend.cultivation.dto.CultivationTemplateResponse;
import com.krishi.backend.cultivation.entity.CultivationTemplate;
import com.krishi.backend.cultivation.entity.TemplateStatus;
import com.krishi.backend.cultivation.mapper.CultivationMapper;
import com.krishi.backend.cultivation.repository.CultivationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CultivationTemplateService {

    private final CultivationTemplateRepository templateRepository;
    private final CultivationMapper cultivationMapper;

    /**
     * Lists published templates, optionally filtered by crop name.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('FARMER')")
    public List<CultivationTemplateResponse> getPublishedTemplates(String cropName) {
        List<CultivationTemplate> templates;
        if (cropName != null && !cropName.isBlank()) {
            templates = templateRepository.findByCropNameIgnoreCaseAndStatus(cropName.trim(), TemplateStatus.PUBLISHED);
        } else {
            templates = templateRepository.findByStatusOrderByCropNameAsc(TemplateStatus.PUBLISHED);
        }

        return templates.stream()
                .map(cultivationMapper::toTemplateResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves template details including stages and activities by ID.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('FARMER')")
    public CultivationTemplateResponse getTemplateById(UUID id) {
        CultivationTemplate template = templateRepository.findByIdAndStatus(id, TemplateStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Published template not found with id: " + id));

        return cultivationMapper.toTemplateResponse(template);
    }
}
