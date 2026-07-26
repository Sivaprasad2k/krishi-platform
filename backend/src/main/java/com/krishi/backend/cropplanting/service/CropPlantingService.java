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
public class CropPlantingService {

    private final CropPlantingRepository cropPlantingRepository;
    private final CropCycleRepository cropCycleRepository;
    private final CropPlantingMapper cropPlantingMapper;
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
