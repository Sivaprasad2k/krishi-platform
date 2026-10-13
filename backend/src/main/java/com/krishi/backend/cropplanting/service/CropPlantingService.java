package com.krishi.backend.cropplanting.service;

import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropcycle.entity.CropCycle;
import com.krishi.backend.cropcycle.entity.CropCycleStatus;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.dto.CropPlantingResponse;
import com.krishi.backend.cropplanting.dto.CropPlantingUpdateRequest;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.event.CropPlantingCreatedEvent;
import com.krishi.backend.cropplanting.event.CropPlantingUpdatedEvent;
import com.krishi.backend.cropplanting.mapper.CropPlantingMapper;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cropcycle.mapper.CropCycleMapper;
import com.krishi.backend.cropplanting.dto.CropWorkspaceResponse;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentResponse;
import com.krishi.backend.diseaseincident.entity.DiseaseIncident;
import com.krishi.backend.diseaseincident.entity.DiseaseIncidentStatus;
import com.krishi.backend.diseaseincident.mapper.DiseaseIncidentMapper;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.farm.mapper.FarmMapper;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.mapper.TaskMapper;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.entity.Treatment;
import com.krishi.backend.treatment.mapper.TreatmentMapper;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CropPlantingService {

    private final CropPlantingRepository cropPlantingRepository;
    private final CropCycleRepository cropCycleRepository;
    private final CropPlantingMapper cropPlantingMapper;
    private final CropCycleMapper cropCycleMapper;
    private final FarmMapper farmMapper;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final DiseaseIncidentRepository diseaseIncidentRepository;
    private final DiseaseIncidentMapper diseaseIncidentMapper;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper treatmentMapper;
    private final com.krishi.backend.cultivation.service.CultivationPlanService cultivationPlanService;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Creates a new CropPlanting on a specific CropCycle.
     * Validates that the CropCycle is in DRAFT or ACTIVE status and that the user owns the parent farm.
     */
    @Transactional
    public CropPlantingResponse createCropPlanting(CropPlantingCreateRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropCycle cropCycle = cropCycleRepository.findById(request.getCropCycleId())
                .orElseThrow(() -> new IllegalArgumentException("Crop cycle not found with id: " + request.getCropCycleId()));

        validateCycleOwnership(cropCycle, userDetails.getUserId());

        // FSM status guard
        if (cropCycle.getStatus() == CropCycleStatus.COMPLETED || cropCycle.getStatus() == CropCycleStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create a crop planting on a crop cycle that is completed or cancelled. Current status: " + cropCycle.getStatus());
        }

        if (request.getPlantingDate() != null && request.getExpectedHarvestDate() != null) {
            if (request.getExpectedHarvestDate().isBefore(request.getPlantingDate())) {
                throw new IllegalArgumentException("Expected harvest date cannot be before planting date.");
            }
        }

        CropPlanting planting = new CropPlanting();
        planting.setId(UUID.randomUUID());
        planting.setCropCycle(cropCycle);
        planting.setCropName(request.getCropName());
        planting.setCropVariety(request.getCropVariety());
        planting.setPlantingDate(request.getPlantingDate());
        planting.setExpectedHarvestDate(request.getExpectedHarvestDate());
        planting.setCreatedAt(LocalDateTime.now());
        planting.setCreatedBy(userDetails.getUserId());

        CropPlanting saved = cropPlantingRepository.save(planting);

        if (request.getTemplateId() != null) {
            cultivationPlanService.generateCultivationPlan(saved.getId(), request.getTemplateId(), userDetails);
        }

        // Publish CropPlantingCreatedEvent
        CropPlantingCreatedEvent event = CropPlantingCreatedEvent.builder()
                .aggregateId(saved.getId())
                .cropCycleId(cropCycle.getId())
                .cropName(saved.getCropName())
                .cropVariety(saved.getCropVariety())
                .plantingDate(saved.getPlantingDate())
                .expectedHarvestDate(saved.getExpectedHarvestDate())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return cropPlantingMapper.toResponse(saved);
    }

    /**
     * Updates an existing CropPlanting, enforcing ownership validation.
     */
    @Transactional
    public CropPlantingResponse updateCropPlanting(UUID plantingId, CropPlantingUpdateRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        validateCycleOwnership(planting.getCropCycle(), userDetails.getUserId());

        // FSM status guard
        CropCycle cropCycle = planting.getCropCycle();
        if (cropCycle.getStatus() == CropCycleStatus.COMPLETED || cropCycle.getStatus() == CropCycleStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a crop planting on a crop cycle that is completed or cancelled. Current status: " + cropCycle.getStatus());
        }

        if (request.getPlantingDate() != null && request.getExpectedHarvestDate() != null) {
            if (request.getExpectedHarvestDate().isBefore(request.getPlantingDate())) {
                throw new IllegalArgumentException("Expected harvest date cannot be before planting date.");
            }
        }

        planting.setCropName(request.getCropName());
        planting.setCropVariety(request.getCropVariety());
        planting.setPlantingDate(request.getPlantingDate());
        planting.setExpectedHarvestDate(request.getExpectedHarvestDate());
        planting.setUpdatedAt(LocalDateTime.now());
        planting.setUpdatedBy(userDetails.getUserId());

        CropPlanting updated = cropPlantingRepository.save(planting);

        // Publish CropPlantingUpdatedEvent
        CropPlantingUpdatedEvent event = CropPlantingUpdatedEvent.builder()
                .aggregateId(updated.getId())
                .cropName(updated.getCropName())
                .cropVariety(updated.getCropVariety())
                .plantingDate(updated.getPlantingDate())
                .expectedHarvestDate(updated.getExpectedHarvestDate())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return cropPlantingMapper.toResponse(updated);
    }

    /**
     * Retrieves the details of a specific CropPlanting, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public CropPlantingResponse getCropPlantingById(UUID plantingId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        validateCycleOwnership(planting.getCropCycle(), userDetails.getUserId());

        return cropPlantingMapper.toResponse(planting);
    }

    /**
     * Lists all crop plantings for a specific crop cycle, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public List<CropPlantingResponse> getPlantingsByCropCycle(UUID cropCycleId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropCycle cropCycle = cropCycleRepository.findById(cropCycleId)
                .orElseThrow(() -> new IllegalArgumentException("Crop cycle not found with id: " + cropCycleId));

        validateCycleOwnership(cropCycle, userDetails.getUserId());

        List<CropPlanting> plantings = cropPlantingRepository.findByCropCycleId(cropCycleId);
        return plantings.stream()
                .map(cropPlantingMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves unified crop workspace details containing crop, season, farm,
     * tasks, disease incidents, and treatments in a single call.
     * Enforces role and ownership verification.
     */
    @PreAuthorize("hasRole('FARMER')")
    @Transactional(readOnly = true)
    public CropWorkspaceResponse getCropWorkspace(UUID plantingId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        validateCycleOwnership(planting.getCropCycle(), userDetails.getUserId());

        CropCycle cycle = planting.getCropCycle();
        Farm farm = cycle.getFarm();

        List<Task> tasks = taskRepository.findByCropPlantingId(plantingId);
        List<DiseaseIncident> incidents = diseaseIncidentRepository.findByCropPlantingId(plantingId);
        List<Treatment> treatments = treatmentRepository.findByDiseaseIncidentCropPlantingId(plantingId);

        List<DiseaseIncidentResponse> incidentResponses = incidents.stream()
                .map(diseaseIncidentMapper::toResponse)
                .collect(Collectors.toList());

        List<DiseaseIncidentResponse> openIncidentResponses = incidentResponses.stream()
                .filter(inc -> inc.getStatus() != DiseaseIncidentStatus.CLOSED)
                .collect(Collectors.toList());

        com.krishi.backend.cultivation.dto.CultivationPlanResponse planResponse =
                cultivationPlanService.getActivePlan(plantingId, userDetails);

        return CropWorkspaceResponse.builder()
                .crop(cropPlantingMapper.toResponse(planting))
                .season(cropCycleMapper.toResponse(cycle))
                .farm(farmMapper.toResponse(farm))
                .farmName(farm.getName())
                .farmId(farm.getId())
                .tasks(tasks.stream().map(taskMapper::toResponse).collect(Collectors.toList()))
                .incidents(incidentResponses)
                .openIncidents(openIncidentResponses)
                .treatments(treatments.stream().map(treatmentMapper::toResponse).collect(Collectors.toList()))
                .cultivationPlan(planResponse)
                .build();
    }

    /**
     * Validates that the user has the FARMER role.
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
     * Walk the ownership chain: CropPlanting -> CropCycle -> Farm -> Farmer
     */
    private void validateCycleOwnership(CropCycle cropCycle, UUID userId) {
        if (!cropCycle.getFarm().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this crop cycle.");
        }
    }
}
