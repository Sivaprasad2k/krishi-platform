package com.krishi.backend.advisory.service;

import com.krishi.backend.advisory.dto.AdvisoryEvaluationSummaryResponse;
import com.krishi.backend.advisory.dto.AgriculturalAdvisoryResponse;
import com.krishi.backend.advisory.entity.AdvisoryStatus;
import com.krishi.backend.advisory.entity.AgriculturalAdvisory;
import com.krishi.backend.advisory.event.AdvisoryAcknowledgedEvent;
import com.krishi.backend.advisory.event.AdvisoryCreatedEvent;
import com.krishi.backend.advisory.event.AdvisoryResolvedEvent;
import com.krishi.backend.advisory.mapper.AgriculturalAdvisoryMapper;
import com.krishi.backend.advisory.repository.AgriculturalAdvisoryRepository;
import com.krishi.backend.advisory.rule.AdvisoryProposal;
import com.krishi.backend.advisory.rule.RuleEvaluationOutcome;
import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropplanting.dto.context.AgriculturalContextResponse;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cropplanting.service.AgriculturalContextService;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.notification.entity.NotificationType;
import com.krishi.backend.notification.service.NotificationService;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service orchestrating agricultural advisory rule evaluation, deduplication,
 * lifecycle transitions, notifications, and domain event publication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgriculturalAdvisoryService {

    private final CropPlantingRepository cropPlantingRepository;
    private final TaskRepository taskRepository;
    private final AgriculturalAdvisoryRepository advisoryRepository;
    private final AgriculturalContextService agriculturalContextService;
    private final AgriculturalAdvisoryEngine advisoryEngine;
    private final AgriculturalAdvisoryMapper advisoryMapper;
    private final NotificationService notificationService;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Triggers deterministic rules evaluation for a given crop planting as of today.
     * Enforces farmer ownership, persists proposals idempotently, handles escalations,
     * and dispatches notifications without duplicates.
     */
    @Transactional
    public AdvisoryEvaluationSummaryResponse evaluateAdvisories(UUID plantingId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        Farm farm = planting.getCropCycle().getFarm();
        validateFarmOwnership(farm, userDetails.getUserId());

        LocalDate evaluationDate = LocalDate.now();
        AgriculturalContextResponse context = agriculturalContextService.getAgriculturalContext(plantingId, evaluationDate, userDetails);

        // Acquire pessimistic row-level lock on CropPlanting for evaluation and persistence.
        // Context resolution (including any external weather provider calls) has completed,
        // ensuring no row locks are held during external I/O.
        planting = cropPlantingRepository.findByIdForUpdate(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        // Run rules engine
        AgriculturalAdvisoryEngine.EngineExecutionResult execResult = advisoryEngine.evaluate(context);

        // Process candidate proposals idempotently
        List<AgriculturalAdvisory> activeAdvisories = new ArrayList<>();
        Set<String> processedFingerprints = new HashSet<>();

        for (AdvisoryProposal proposal : execResult.getProposals()) {
            processedFingerprints.add(proposal.getFingerprint());
            AgriculturalAdvisory advisory = processProposal(farm, planting, proposal, userDetails.getUserId());
            if (advisory != null && advisory.getStatus() == AdvisoryStatus.ACTIVE) {
                activeAdvisories.add(advisory);
            }
        }

        // Auto-resolve or expire advisories whose conditions no longer hold
        resolveAndExpireStaleAdvisories(planting.getId(), execResult, processedFingerprints, userDetails.getUserId());

        // Fetch all currently active advisories for this planting to return in summary
        List<AgriculturalAdvisory> currentActive = advisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(
                plantingId, AdvisoryStatus.ACTIVE
        );

        return AdvisoryEvaluationSummaryResponse.builder()
                .cropPlantingId(plantingId)
                .evaluatedAt(OffsetDateTime.now())
                .asOfDate(evaluationDate)
                .totalRulesEvaluated(execResult.getReportItems().size())
                .activeAdvisoriesCount(currentActive.size())
                .ruleBreakdown(execResult.getReportItems())
                .activeAdvisories(currentActive.stream().map(advisoryMapper::toResponse).collect(Collectors.toList()))
                .build();
    }

    /**
     * Lists advisories for a crop planting with optional status filter.
     */
    @Transactional(readOnly = true)
    public List<AgriculturalAdvisoryResponse> getAdvisories(UUID plantingId, AdvisoryStatus status, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        validateFarmOwnership(planting.getCropCycle().getFarm(), userDetails.getUserId());

        List<AgriculturalAdvisory> advisories;
        if (status != null) {
            advisories = advisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(plantingId, status);
        } else {
            advisories = advisoryRepository.findByCropPlantingIdOrderByCreatedAtDesc(plantingId);
        }

        return advisories.stream()
                .map(advisoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Acknowledges an active advisory.
     */
    @Transactional
    public AgriculturalAdvisoryResponse acknowledgeAdvisory(UUID advisoryId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        AgriculturalAdvisory advisory = advisoryRepository.findById(advisoryId)
                .orElseThrow(() -> new IllegalArgumentException("Agricultural advisory not found with id: " + advisoryId));

        validateFarmOwnership(advisory.getFarm(), userDetails.getUserId());

        if (advisory.getStatus() != AdvisoryStatus.ACTIVE) {
            throw new IllegalStateException("Cannot acknowledge advisory in status: " + advisory.getStatus());
        }

        advisory.setStatus(AdvisoryStatus.ACKNOWLEDGED);
        advisory.setAcknowledgedAt(OffsetDateTime.now());
        AgriculturalAdvisory updated = advisoryRepository.save(advisory);

        // Publish AdvisoryAcknowledgedEvent
        domainEventPublisher.publish(AdvisoryAcknowledgedEvent.builder()
                .advisoryId(updated.getId())
                .farmId(updated.getFarm().getId())
                .cropPlantingId(updated.getCropPlanting().getId())
                .ruleCode(updated.getRuleCode())
                .status(updated.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build());

        return advisoryMapper.toResponse(updated);
    }

    /**
     * Explicitly resolves an advisory.
     */
    @Transactional
    public AgriculturalAdvisoryResponse resolveAdvisory(UUID advisoryId, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        AgriculturalAdvisory advisory = advisoryRepository.findById(advisoryId)
                .orElseThrow(() -> new IllegalArgumentException("Agricultural advisory not found with id: " + advisoryId));

        validateFarmOwnership(advisory.getFarm(), userDetails.getUserId());

        if (advisory.getStatus() == AdvisoryStatus.RESOLVED) {
            return advisoryMapper.toResponse(advisory);
        }

        advisory.setStatus(AdvisoryStatus.RESOLVED);
        advisory.setResolvedAt(OffsetDateTime.now());
        AgriculturalAdvisory updated = advisoryRepository.save(advisory);

        // Publish AdvisoryResolvedEvent
        domainEventPublisher.publish(AdvisoryResolvedEvent.builder()
                .advisoryId(updated.getId())
                .farmId(updated.getFarm().getId())
                .cropPlantingId(updated.getCropPlanting().getId())
                .ruleCode(updated.getRuleCode())
                .status(updated.getStatus())
                .resolutionReason("Manually resolved by farmer")
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build());

        return advisoryMapper.toResponse(updated);
    }

    private AgriculturalAdvisory processProposal(Farm farm, CropPlanting planting, AdvisoryProposal proposal, UUID userId) {
        Optional<AgriculturalAdvisory> activeOpt = advisoryRepository.findByAdvisoryFingerprintAndStatus(
                proposal.getFingerprint(), AdvisoryStatus.ACTIVE
        );

        if (activeOpt.isPresent()) {
            AgriculturalAdvisory existing = activeOpt.get();

            // Check if severity escalated
            if (proposal.getSeverity().ordinal() > existing.getSeverity().ordinal()) {
                log.info("Agricultural advisory [{}] escalated from {} to {}. Superseding existing.",
                        proposal.getFingerprint(), existing.getSeverity(), proposal.getSeverity());

                existing.setStatus(AdvisoryStatus.SUPERSEDED);
                advisoryRepository.saveAndFlush(existing);

                AgriculturalAdvisory escalated = createAndSaveAdvisory(farm, planting, proposal, userId);

                // Dispatch escalation notification
                try {
                    notificationService.createNotification(
                            farm.getOwner().getId(),
                            "[ESCALATION] " + proposal.getTitle(),
                            proposal.getMessage(),
                            NotificationType.AGRICULTURAL_ADVISORY
                    );
                } catch (Exception notifEx) {
                    log.warn("Failed to dispatch notification for escalated advisory [{}]: {}",
                            proposal.getFingerprint(), notifEx.getMessage());
                }

                return escalated;
            } else {
                // Severity unchanged or lower: update lastEvaluatedAt without duplicate notification
                existing.setLastEvaluatedAt(OffsetDateTime.now());
                return advisoryRepository.save(existing);
            }
        }

        // Check if previously acknowledged with equal or higher severity
        Optional<AgriculturalAdvisory> latestOpt = advisoryRepository.findFirstByAdvisoryFingerprintOrderByCreatedAtDesc(
                proposal.getFingerprint()
        );

        if (latestOpt.isPresent()) {
            AgriculturalAdvisory latest = latestOpt.get();
            if (latest.getStatus() == AdvisoryStatus.ACKNOWLEDGED
                    && latest.getSeverity().ordinal() >= proposal.getSeverity().ordinal()) {
                // Farmer already acknowledged this warning; update lastEvaluatedAt without re-alerting
                latest.setLastEvaluatedAt(OffsetDateTime.now());
                return advisoryRepository.save(latest);
            }
        }

        // New active advisory
        AgriculturalAdvisory newAdvisory = createAndSaveAdvisory(farm, planting, proposal, userId);

        // Dispatch notification
        try {
            notificationService.createNotification(
                    farm.getOwner().getId(),
                    proposal.getTitle(),
                    proposal.getMessage(),
                    NotificationType.AGRICULTURAL_ADVISORY
            );
        } catch (Exception notifEx) {
            log.warn("Failed to dispatch notification for advisory [{}]: {}",
                    proposal.getFingerprint(), notifEx.getMessage());
        }

        return newAdvisory;
    }

    private AgriculturalAdvisory createAndSaveAdvisory(Farm farm, CropPlanting planting, AdvisoryProposal proposal, UUID userId) {
        Task task = null;
        if (proposal.getTaskId() != null) {
            task = taskRepository.findById(proposal.getTaskId()).orElse(null);
        }

        AgriculturalAdvisory advisory = AgriculturalAdvisory.builder()
                .id(UUID.randomUUID())
                .advisoryFingerprint(proposal.getFingerprint())
                .farm(farm)
                .cropPlanting(planting)
                .task(task)
                .ruleCode(proposal.getRuleCode())
                .severity(proposal.getSeverity())
                .title(proposal.getTitle())
                .message(proposal.getMessage())
                .recommendedAction(proposal.getRecommendedAction())
                .targetDate(proposal.getTargetDate())
                .status(AdvisoryStatus.ACTIVE)
                .provenance(proposal.getProvenance())
                .createdAt(OffsetDateTime.now())
                .lastEvaluatedAt(OffsetDateTime.now())
                .expiresAt(proposal.getExpiresAt())
                .build();

        AgriculturalAdvisory saved = advisoryRepository.saveAndFlush(advisory);

        // Publish AdvisoryCreatedEvent
        domainEventPublisher.publish(AdvisoryCreatedEvent.builder()
                .advisoryId(saved.getId())
                .farmId(farm.getId())
                .cropPlantingId(planting.getId())
                .taskId(task != null ? task.getId() : null)
                .ruleCode(saved.getRuleCode())
                .severity(saved.getSeverity())
                .title(saved.getTitle())
                .status(saved.getStatus())
                .occurredAt(LocalDateTime.now())
                .createdBy(userId)
                .build());

        return saved;
    }

    private void resolveAndExpireStaleAdvisories(
            UUID plantingId,
            AgriculturalAdvisoryEngine.EngineExecutionResult execResult,
            Set<String> processedFingerprints,
            UUID userId
    ) {
        List<AgriculturalAdvisory> activeAdvisories = advisoryRepository.findByCropPlantingIdAndStatusInOrderByCreatedAtDesc(
                plantingId, List.of(AdvisoryStatus.ACTIVE, AdvisoryStatus.ACKNOWLEDGED)
        );

        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = LocalDate.now();

        // Rules that successfully evaluated clean
        Set<String> successfulRuleCodes = execResult.getReportItems().stream()
                .filter(item -> item.getOutcome() == RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS)
                .map(item -> item.getRuleCode())
                .collect(Collectors.toSet());

        for (AgriculturalAdvisory adv : activeAdvisories) {
            // 1. Expire if expiresAt has elapsed
            if (adv.getExpiresAt() != null && adv.getExpiresAt().isBefore(now)) {
                adv.setStatus(AdvisoryStatus.EXPIRED);
                advisoryRepository.save(adv);
                continue;
            }

            // 2. Resolve if linked task was completed, cancelled, or skipped
            if (adv.getTask() != null) {
                TaskStatus taskStatus = adv.getTask().getStatus();
                if (taskStatus == TaskStatus.COMPLETED
                        || taskStatus == TaskStatus.CANCELLED
                        || taskStatus == TaskStatus.SKIPPED) {
                    adv.setStatus(AdvisoryStatus.RESOLVED);
                    adv.setResolvedAt(now);
                    AgriculturalAdvisory saved = advisoryRepository.save(adv);

                    String reason = String.format("Linked task '%s' was %s",
                            adv.getTask().getTitle() != null ? adv.getTask().getTitle() : adv.getTask().getId().toString(),
                            taskStatus.name().toLowerCase());

                    domainEventPublisher.publish(AdvisoryResolvedEvent.builder()
                            .advisoryId(saved.getId())
                            .farmId(saved.getFarm().getId())
                            .cropPlantingId(saved.getCropPlanting().getId())
                            .ruleCode(saved.getRuleCode())
                            .status(saved.getStatus())
                            .resolutionReason(reason)
                            .occurredAt(LocalDateTime.now())
                            .createdBy(userId)
                            .build());

                    continue;
                }
            }

            // 3. Auto-resolve if the rule evaluated successfully and the condition cleared
            if (successfulRuleCodes.contains(adv.getRuleCode())
                    && !processedFingerprints.contains(adv.getAdvisoryFingerprint())) {
                adv.setStatus(AdvisoryStatus.RESOLVED);
                adv.setResolvedAt(now);
                AgriculturalAdvisory saved = advisoryRepository.save(adv);

                String reason = "Agronomic risk conditions resolved clean during rules evaluation";

                domainEventPublisher.publish(AdvisoryResolvedEvent.builder()
                        .advisoryId(saved.getId())
                        .farmId(saved.getFarm().getId())
                        .cropPlantingId(saved.getCropPlanting().getId())
                        .ruleCode(saved.getRuleCode())
                        .status(saved.getStatus())
                        .resolutionReason(reason)
                        .occurredAt(LocalDateTime.now())
                        .createdBy(userId)
                        .build());
            }
        }
    }

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

    private void validateFarmOwnership(Farm farm, UUID userId) {
        if (farm.getOwner() == null || !farm.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this resource.");
        }
    }
}
