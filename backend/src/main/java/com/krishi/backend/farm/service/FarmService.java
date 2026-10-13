package com.krishi.backend.farm.service;

import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.dto.FarmResponse;
import com.krishi.backend.farm.dto.FarmUpdateRequest;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.farm.entity.FarmStatus;
import com.krishi.backend.farm.event.FarmCreatedEvent;
import com.krishi.backend.farm.mapper.FarmMapper;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.entity.User;
import com.krishi.backend.identity.repository.UserRepository;
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
public class FarmService {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final FarmMapper farmMapper;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Creates a new Farm for the authenticated user.
     * Only users with the FARMER role can perform this action.
     *
     * @param request     the request DTO
     * @param userDetails the authenticated user details
     * @return the created FarmResponse
     */
    @Transactional
    public FarmResponse createFarm(FarmCreateRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        User owner = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Farm farm = new Farm();
        farm.setId(UUID.randomUUID());
        farm.setName(request.getName());
        farm.setSizeHectares(request.getSizeHectares());
        farm.setLocation(request.getLocation());
        farm.setState(request.getState());
        farm.setDistrict(request.getDistrict());
        farm.setLatitude(request.getLatitude());
        farm.setLongitude(request.getLongitude());
        farm.setOwner(owner);
        farm.setStatus(FarmStatus.ACTIVE);
        farm.setCreatedAt(LocalDateTime.now());
        farm.setCreatedBy(userDetails.getUserId());

        Farm savedFarm = farmRepository.save(farm);

        // Publish FarmCreatedEvent
        FarmCreatedEvent event = FarmCreatedEvent.builder()
                .aggregateId(savedFarm.getId())
                .name(savedFarm.getName())
                .sizeHectares(savedFarm.getSizeHectares())
                .location(savedFarm.getLocation())
                .ownerId(owner.getId())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();
        domainEventPublisher.publish(event);

        return farmMapper.toResponse(savedFarm);
    }

    /**
     * Retrieves the details of a specific Farm, enforcing ownership and role checks.
     *
     * @param farmId      the UUID of the farm
     * @param userDetails the authenticated user details
     * @return the FarmResponse
     */
    @Transactional(readOnly = true)
    public FarmResponse getFarmById(UUID farmId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + farmId));

        validateOwnership(farm, userDetails.getUserId());

        return farmMapper.toResponse(farm);
    }

    /**
     * Lists all farms owned by the authenticated user.
     *
     * @param userDetails the authenticated user details
     * @return a list of FarmResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<FarmResponse> getMyFarms(CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        List<Farm> farms = farmRepository.findByOwnerId(userDetails.getUserId());
        return farms.stream()
                .map(farmMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing Farm, enforcing ownership and role checks.
     *
     * @param farmId      the UUID of the farm to update
     * @param request     the update request DTO
     * @param userDetails the authenticated user details
     * @return the updated FarmResponse
     */
    @Transactional
    public FarmResponse updateFarm(UUID farmId, FarmUpdateRequest request, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + farmId));

        validateOwnership(farm, userDetails.getUserId());

        farm.setName(request.getName());
        farm.setSizeHectares(request.getSizeHectares());
        farm.setLocation(request.getLocation());
        farm.setState(request.getState());
        farm.setDistrict(request.getDistrict());
        farm.setLatitude(request.getLatitude());
        farm.setLongitude(request.getLongitude());
        farm.setStatus(request.getStatus());
        farm.setUpdatedAt(LocalDateTime.now());
        farm.setUpdatedBy(userDetails.getUserId());

        Farm updatedFarm = farmRepository.save(farm);
        return farmMapper.toResponse(updatedFarm);
    }

    /**
     * Validates that the user has the FARMER role.
     *
     * @param userDetails the authenticated user details
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
     * Enforces ownership of the farm.
     *
     * @param farm   the farm entity
     * @param userId the UUID of the authenticated user
     */
    private void validateOwnership(Farm farm, UUID userId) {
        if (!farm.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own this farm.");
        }
    }
}
