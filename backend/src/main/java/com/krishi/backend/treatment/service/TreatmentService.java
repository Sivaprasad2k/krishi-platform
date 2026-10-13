package com.krishi.backend.treatment.service;

import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.diseaseincident.entity.DiseaseIncident;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.treatment.dto.TreatmentPlanRequest;
import com.krishi.backend.treatment.dto.TreatmentResponse;
import com.krishi.backend.treatment.entity.Treatment;
import com.krishi.backend.treatment.entity.TreatmentStatus;
import com.krishi.backend.treatment.event.TreatmentCompletedEvent;
import com.krishi.backend.treatment.event.TreatmentPlannedEvent;
import com.krishi.backend.treatment.event.TreatmentStartedEvent;
import com.krishi.backend.treatment.mapper.TreatmentMapper;
import com.krishi.backend.treatment.repository.TreatmentRepository;
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
public class TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final DiseaseIncidentRepository diseaseIncidentRepository;
    private final TreatmentMapper treatmentMapper;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Creates a new Treatment plan for a specific DiseaseIncident.
     */
    @Transactional
    public TreatmentResponse createTreatment(TreatmentPlanRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        DiseaseIncident incident = diseaseIncidentRepository.findById(request.getDiseaseIncidentId())
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found with id: " + request.getDiseaseIncidentId()));

        validateIncidentOwnership(incident, userDetails.getUserId());

        Treatment treatment = new Treatment();
        treatment.setId(UUID.randomUUID());
        treatment.setDiseaseIncident(incident);
        treatment.setTreatmentName(request.getTreatmentName());
        treatment.setDescription(request.getDescription());
        treatment.setRecommendedDosage(request.getRecommendedDosage());
        treatment.setApplicationMethod(request.getApplicationMethod());
        treatment.setStatus(TreatmentStatus.PLANNED);
        treatment.setCreatedAt(LocalDateTime.now());
        treatment.setCreatedBy(userDetails.getUserId());

        Treatment saved = treatmentRepository.save(treatment);

        // Publish TreatmentPlannedEvent
        TreatmentPlannedEvent event = TreatmentPlannedEvent.builder()
                .aggregateId(saved.getId())
                .diseaseIncidentId(incident.getId())
                .treatmentName(saved.getTreatmentName())
                .status(saved.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return treatmentMapper.toResponse(saved);
    }

    /**
     * Starts the Treatment plan, moving status from PLANNED to IN_PROGRESS.
     */
    @Transactional
    public TreatmentResponse startTreatment(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found with id: " + id));

        validateIncidentOwnership(treatment.getDiseaseIncident(), userDetails.getUserId());

        if (treatment.getStatus() != TreatmentStatus.PLANNED) {
            throw new IllegalStateException("Cannot start a treatment that is in status: " + treatment.getStatus());
        }

        LocalDateTime startDate = LocalDateTime.now();
        treatment.setStatus(TreatmentStatus.IN_PROGRESS);
        treatment.setStartDate(startDate);
        treatment.setUpdatedAt(LocalDateTime.now());
        treatment.setUpdatedBy(userDetails.getUserId());

        Treatment updated = treatmentRepository.save(treatment);

        // Publish TreatmentStartedEvent
        TreatmentStartedEvent event = TreatmentStartedEvent.builder()
                .aggregateId(updated.getId())
                .startDate(startDate)
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return treatmentMapper.toResponse(updated);
    }

    /**
     * Completes the Treatment plan, moving status from IN_PROGRESS to COMPLETED.
     */
    @Transactional
    public TreatmentResponse completeTreatment(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found with id: " + id));

        validateIncidentOwnership(treatment.getDiseaseIncident(), userDetails.getUserId());

        if (treatment.getStatus() != TreatmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete a treatment that is in status: " + treatment.getStatus());
        }

        LocalDateTime completionDate = LocalDateTime.now();
        treatment.setStatus(TreatmentStatus.COMPLETED);
        treatment.setCompletionDate(completionDate);
        treatment.setUpdatedAt(LocalDateTime.now());
        treatment.setUpdatedBy(userDetails.getUserId());

        Treatment updated = treatmentRepository.save(treatment);

        // Publish TreatmentCompletedEvent
        TreatmentCompletedEvent event = TreatmentCompletedEvent.builder()
                .aggregateId(updated.getId())
                .completionDate(completionDate)
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return treatmentMapper.toResponse(updated);
    }

    /**
     * Retrieves details of a specific Treatment, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public TreatmentResponse getTreatmentById(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found with id: " + id));

        validateIncidentOwnership(treatment.getDiseaseIncident(), userDetails.getUserId());

        return treatmentMapper.toResponse(treatment);
    }

    /**
     * Lists all treatments associated with a specific disease incident, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public List<TreatmentResponse> getTreatmentsByIncident(UUID incidentId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        DiseaseIncident incident = diseaseIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Disease incident not found with id: " + incidentId));

        validateIncidentOwnership(incident, userDetails.getUserId());

        List<Treatment> treatments = treatmentRepository.findByDiseaseIncidentId(incidentId);
        return treatments.stream()
                .map(treatmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lists all treatments across all disease incidents for the authenticated farmer, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public List<TreatmentResponse> getAllTreatmentsForUser(CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        List<Treatment> treatments = treatmentRepository.findByFarmerUserId(userDetails.getUserId());
        return treatments.stream()
                .map(treatmentMapper::toResponse)
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
     * Enforces ownership chain walk: DiseaseIncident -> CropPlanting -> CropCycle -> Farm -> Farmer (User)
     */
    private void validateIncidentOwnership(DiseaseIncident incident, UUID userId) {
        if (!incident.getCropPlanting().getCropCycle().getFarm().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this disease incident.");
        }
    }
}
