package com.krishi.backend.diseaseincident.service;

import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentReportRequest;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentResponse;
import com.krishi.backend.diseaseincident.entity.DiseaseIncident;
import com.krishi.backend.diseaseincident.entity.DiseaseIncidentStatus;
import com.krishi.backend.diseaseincident.event.DiseaseIncidentClosedEvent;
import com.krishi.backend.diseaseincident.event.DiseaseIncidentReportedEvent;
import com.krishi.backend.diseaseincident.event.DiseaseIncidentTreatedEvent;
import com.krishi.backend.diseaseincident.event.DiseaseIncidentUnderReviewEvent;
import com.krishi.backend.diseaseincident.mapper.DiseaseIncidentMapper;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiseaseIncidentService {

    private final DiseaseIncidentRepository diseaseIncidentRepository;
    private final CropPlantingRepository cropPlantingRepository;
    private final DiseaseIncidentMapper diseaseIncidentMapper;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Reports a new crop disease incident.
     */
    @Transactional
    public DiseaseIncidentResponse reportIncident(DiseaseIncidentReportRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(request.getCropPlantingId())
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + request.getCropPlantingId()));

        validatePlantingOwnership(planting, userDetails.getUserId());

        DiseaseIncident incident = new DiseaseIncident();
        incident.setId(UUID.randomUUID());
        incident.setCropPlanting(planting);
        incident.setDiseaseName(request.getDiseaseName());
        incident.setDescription(request.getDescription());
        incident.setStatus(DiseaseIncidentStatus.REPORTED);
        incident.setReportedAt(LocalDateTime.now());
        incident.setCreatedAt(LocalDateTime.now());
        incident.setCreatedBy(userDetails.getUserId());

        DiseaseIncident saved = diseaseIncidentRepository.save(incident);

        // Publish DiseaseIncidentReportedEvent
        DiseaseIncidentReportedEvent event = DiseaseIncidentReportedEvent.builder()
                .aggregateId(saved.getId())
                .cropPlantingId(planting.getId())
                .diseaseName(saved.getDiseaseName())
                .status(saved.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return diseaseIncidentMapper.toResponse(saved);
    }

    /**
     * Moves the incident state from REPORTED to UNDER_REVIEW.
     */
    @Transactional
    public DiseaseIncidentResponse reviewIncident(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        DiseaseIncident incident = diseaseIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found with id: " + id));

        validatePlantingOwnership(incident.getCropPlanting(), userDetails.getUserId());

        if (incident.getStatus() != DiseaseIncidentStatus.REPORTED) {
            throw new IllegalStateException("Cannot review a disease incident that is in status: " + incident.getStatus());
        }

        incident.setStatus(DiseaseIncidentStatus.UNDER_REVIEW);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setUpdatedAt(LocalDateTime.now());
        incident.setUpdatedBy(userDetails.getUserId());

        DiseaseIncident updated = diseaseIncidentRepository.save(incident);

        // Publish DiseaseIncidentUnderReviewEvent
        DiseaseIncidentUnderReviewEvent event = DiseaseIncidentUnderReviewEvent.builder()
                .aggregateId(updated.getId())
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return diseaseIncidentMapper.toResponse(updated);
    }

    /**
     * Moves the incident state from UNDER_REVIEW to TREATED.
     */
    @Transactional
    public DiseaseIncidentResponse treatIncident(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        DiseaseIncident incident = diseaseIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found with id: " + id));

        validatePlantingOwnership(incident.getCropPlanting(), userDetails.getUserId());

        if (incident.getStatus() != DiseaseIncidentStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Cannot treat a disease incident that is in status: " + incident.getStatus());
        }

        incident.setStatus(DiseaseIncidentStatus.TREATED);
        incident.setTreatedAt(LocalDateTime.now());
        incident.setUpdatedAt(LocalDateTime.now());
        incident.setUpdatedBy(userDetails.getUserId());

        DiseaseIncident updated = diseaseIncidentRepository.save(incident);

        // Publish DiseaseIncidentTreatedEvent
        DiseaseIncidentTreatedEvent event = DiseaseIncidentTreatedEvent.builder()
                .aggregateId(updated.getId())
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return diseaseIncidentMapper.toResponse(updated);
    }

    /**
     * Moves the incident state from TREATED to CLOSED.
     */
    @Transactional
    public DiseaseIncidentResponse closeIncident(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        DiseaseIncident incident = diseaseIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found with id: " + id));

        validatePlantingOwnership(incident.getCropPlanting(), userDetails.getUserId());

        if (incident.getStatus() != DiseaseIncidentStatus.TREATED) {
            throw new IllegalStateException("Cannot close a disease incident that is in status: " + incident.getStatus());
        }

        incident.setStatus(DiseaseIncidentStatus.CLOSED);
        incident.setClosedAt(LocalDateTime.now());
        incident.setUpdatedAt(LocalDateTime.now());
        incident.setUpdatedBy(userDetails.getUserId());

        DiseaseIncident updated = diseaseIncidentRepository.save(incident);

        // Publish DiseaseIncidentClosedEvent
        DiseaseIncidentClosedEvent event = DiseaseIncidentClosedEvent.builder()
                .aggregateId(updated.getId())
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return diseaseIncidentMapper.toResponse(updated);
    }

    /**
     * Retrieves details of a specific DiseaseIncident, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public DiseaseIncidentResponse getIncidentById(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        DiseaseIncident incident = diseaseIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found with id: " + id));

        validatePlantingOwnership(incident.getCropPlanting(), userDetails.getUserId());

        return diseaseIncidentMapper.toResponse(incident);
    }

    /**
     * Lists all disease incidents for a specific crop planting, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public List<DiseaseIncidentResponse> getIncidentsByPlanting(UUID plantingId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        validatePlantingOwnership(planting, userDetails.getUserId());

        List<DiseaseIncident> incidents = diseaseIncidentRepository.findByCropPlantingId(plantingId);
        return incidents.stream()
                .map(diseaseIncidentMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Enforces the ROLE_FARMER check.
     */
    private void validateFarmerRole(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Access denied. User is not authenticated.");
        }
        boolean isFarmer = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_FARMER"));
        if (!isFarmer) {
            throw new AccessDeniedException("Access denied. Only farmers are allowed to access this resource.");
        }
    }

    /**
     * Enforces ownership chain: CropPlanting -> CropCycle -> Farm -> Farmer (User)
     */
    private void validatePlantingOwnership(CropPlanting planting, UUID userId) {
        if (!planting.getCropCycle().getFarm().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this crop planting.");
        }
    }
}
