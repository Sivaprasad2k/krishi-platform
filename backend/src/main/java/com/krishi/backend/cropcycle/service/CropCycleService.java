package com.krishi.backend.cropcycle.service;

import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.dto.CropCycleResponse;
import com.krishi.backend.cropcycle.entity.CropCycle;
import com.krishi.backend.cropcycle.entity.CropCycleStatus;
import com.krishi.backend.cropcycle.event.CropCycleCancelledEvent;
import com.krishi.backend.cropcycle.event.CropCycleCompletedEvent;
import com.krishi.backend.cropcycle.event.CropCycleCreatedEvent;
import com.krishi.backend.cropcycle.event.CropCycleStartedEvent;
import com.krishi.backend.cropcycle.mapper.CropCycleMapper;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.farm.repository.FarmRepository;
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
public class CropCycleService {

    private final CropCycleRepository cropCycleRepository;
    private final FarmRepository farmRepository;
    private final CropCycleMapper cropCycleMapper;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Creates a new CropCycle in DRAFT status on a user's farm.
     * Enforces ownership of the parent farm.
     */
    @Transactional
    public CropCycleResponse createCropCycle(CropCycleCreateRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + request.getFarmId()));

        validateFarmOwnership(farm, userDetails.getUserId());

        CropCycle cropCycle = new CropCycle();
        cropCycle.setId(UUID.randomUUID());
        cropCycle.setName(request.getName());
        cropCycle.setSeasonType(request.getSeasonType());
        cropCycle.setYear(request.getYear());
        cropCycle.setNotes(request.getNotes());
        cropCycle.setStatus(CropCycleStatus.DRAFT);
        cropCycle.setFarm(farm);
        cropCycle.setCreatedAt(LocalDateTime.now());
        cropCycle.setCreatedBy(userDetails.getUserId());

        CropCycle saved = cropCycleRepository.save(cropCycle);

        // Publish CropCycleCreatedEvent
        CropCycleCreatedEvent event = CropCycleCreatedEvent.builder()
                .aggregateId(saved.getId())
                .farmId(farm.getId())
                .name(saved.getName())
                .seasonType(saved.getSeasonType())
                .year(saved.getYear())
                .status(saved.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return cropCycleMapper.toResponse(saved);
    }

    /**
     * Retrieves the details of a specific CropCycle, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public CropCycleResponse getCropCycleById(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropCycle cropCycle = cropCycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Crop cycle not found with id: " + id));

        validateFarmOwnership(cropCycle.getFarm(), userDetails.getUserId());

        return cropCycleMapper.toResponse(cropCycle);
    }

    /**
     * Retrieves all CropCycles associated with a specific farm, enforcing ownership of the farm.
     */
    @Transactional(readOnly = true)
    public List<CropCycleResponse> getCropCyclesByFarm(UUID farmId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + farmId));

        validateFarmOwnership(farm, userDetails.getUserId());

        List<CropCycle> cycles = cropCycleRepository.findByFarmId(farmId);
        return cycles.stream()
                .map(cropCycleMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Transitions a CropCycle status from DRAFT to ACTIVE and sets the start date.
     */
    @Transactional
    public CropCycleResponse startCropCycle(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropCycle cropCycle = cropCycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Crop cycle not found with id: " + id));

        validateFarmOwnership(cropCycle.getFarm(), userDetails.getUserId());

        if (cropCycle.getStatus() != CropCycleStatus.DRAFT) {
            throw new IllegalStateException("Cannot start a crop cycle that is in status: " + cropCycle.getStatus());
        }

        LocalDateTime startDate = LocalDateTime.now();
        cropCycle.setStatus(CropCycleStatus.ACTIVE);
        cropCycle.setStartDate(startDate);
        cropCycle.setUpdatedAt(LocalDateTime.now());
        cropCycle.setUpdatedBy(userDetails.getUserId());

        CropCycle updated = cropCycleRepository.save(cropCycle);

        // Publish CropCycleStartedEvent
        CropCycleStartedEvent event = CropCycleStartedEvent.builder()
                .aggregateId(updated.getId())
                .startDate(startDate)
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return cropCycleMapper.toResponse(updated);
    }

    /**
     * Transitions a CropCycle status from ACTIVE to COMPLETED and sets the end date.
     */
    @Transactional
    public CropCycleResponse completeCropCycle(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropCycle cropCycle = cropCycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Crop cycle not found with id: " + id));

        validateFarmOwnership(cropCycle.getFarm(), userDetails.getUserId());

        if (cropCycle.getStatus() != CropCycleStatus.ACTIVE) {
            throw new IllegalStateException("Cannot complete a crop cycle that is in status: " + cropCycle.getStatus());
        }

        LocalDateTime endDate = LocalDateTime.now();
        cropCycle.setStatus(CropCycleStatus.COMPLETED);
        cropCycle.setEndDate(endDate);
        cropCycle.setUpdatedAt(LocalDateTime.now());
        cropCycle.setUpdatedBy(userDetails.getUserId());

        CropCycle updated = cropCycleRepository.save(cropCycle);

        // Publish CropCycleCompletedEvent
        CropCycleCompletedEvent event = CropCycleCompletedEvent.builder()
                .aggregateId(updated.getId())
                .endDate(endDate)
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return cropCycleMapper.toResponse(updated);
    }

    /**
     * Transitions a CropCycle status from ACTIVE to CANCELLED and sets the end date.
     */
    @Transactional
    public CropCycleResponse cancelCropCycle(UUID id, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropCycle cropCycle = cropCycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Crop cycle not found with id: " + id));

        validateFarmOwnership(cropCycle.getFarm(), userDetails.getUserId());

        if (cropCycle.getStatus() != CropCycleStatus.ACTIVE) {
            throw new IllegalStateException("Cannot cancel a crop cycle that is in status: " + cropCycle.getStatus());
        }

        LocalDateTime endDate = LocalDateTime.now();
        cropCycle.setStatus(CropCycleStatus.CANCELLED);
        cropCycle.setEndDate(endDate);
        cropCycle.setUpdatedAt(LocalDateTime.now());
        cropCycle.setUpdatedBy(userDetails.getUserId());

        CropCycle updated = cropCycleRepository.save(cropCycle);

        // Publish CropCycleCancelledEvent
        CropCycleCancelledEvent event = CropCycleCancelledEvent.builder()
                .aggregateId(updated.getId())
                .endDate(endDate)
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return cropCycleMapper.toResponse(updated);
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
     * Enforces ownership of the parent farm.
     */
    private void validateFarmOwnership(Farm farm, UUID userId) {
        if (!farm.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own this farm.");
        }
    }
}
