package com.krishi.backend.cultivation.service;

import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import com.krishi.backend.cultivation.dto.GrowthStageStatus;
import com.krishi.backend.cultivation.entity.CultivationPlan;
import com.krishi.backend.cultivation.entity.CultivationPlanStatus;
import com.krishi.backend.cultivation.entity.CultivationStage;
import com.krishi.backend.cultivation.entity.CultivationTemplate;
import com.krishi.backend.cultivation.repository.CultivationPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deterministic agronomic service responsible for estimating the phenological
 * growth stage of a crop planting based on certified cultivation catalog templates.
 *
 * <p>Day-Count Convention:
 * <ul>
 *   <li>The evaluation baseline is calendar-day difference: {@code ChronoUnit.DAYS.between(plantingDate.toLocalDate(), asOfDate)}.</li>
 *   <li>Day 0 corresponds to the calendar date on which the crop was sown or transplanted into the field.</li>
 *   <li>Elapsed days increment by 1 for each subsequent calendar day.</li>
 *   <li>Negative elapsed days correspond to pre-planting dates (e.g. seedbed nursery preparation).</li>
 * </ul>
 *
 * <p>Boundary & Overlap Rules:
 * <ul>
 *   <li>Stage windows are evaluated as closed integer intervals {@code [startDayOffset, endDayOffset]}.</li>
 *   <li>Transitions between sequential disjoint stages (e.g. Stage A ending at Day 25, Stage B beginning at Day 26)
 *       are strictly deterministic.</li>
 *   <li>Where a pre-planting stage ends at Day 0 and a field establishment stage begins at Day 0, Day 0
 *       is deterministically attributed to the establishment stage because field planting has occurred.</li>
 *   <li>Any other ambiguous or corrupt overlapping stage boundaries in the catalog are explicitly flagged
 *       with {@link GrowthStageStatus#STAGE_DEFINITION_ERROR} without silently selecting an arbitrary stage.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgronomicStageResolverService {

    private final CultivationPlanRepository cultivationPlanRepository;
    private final CropPlantingRepository cropPlantingRepository;

    /**
     * Resolves the estimated growth stage for an in-memory CropPlanting as of today's date.
     *
     * @param planting the crop planting entity
     * @return the estimated growth stage response
     */
    @Transactional(readOnly = true)
    public EstimatedGrowthStageResponse resolveGrowthStage(CropPlanting planting) {
        return resolveGrowthStage(planting, LocalDate.now());
    }

    /**
     * Resolves the estimated growth stage for an in-memory CropPlanting as of a specified evaluation date.
     *
     * @param planting the crop planting entity
     * @param asOfDate the evaluation date (defaults to LocalDate.now() if null)
     * @return the estimated growth stage response
     */
    @Transactional(readOnly = true)
    public EstimatedGrowthStageResponse resolveGrowthStage(CropPlanting planting, LocalDate asOfDate) {
        if (planting == null) {
            throw new IllegalArgumentException("Crop planting must not be null.");
        }

        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        if (planting.getPlantingDate() == null) {
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.MISSING_PLANTING_DATE)
                    .message("Planting date is missing for this crop planting.")
                    .build();
        }

        LocalDate plantingLocalDate = planting.getPlantingDate().toLocalDate();
        long elapsedDays = ChronoUnit.DAYS.between(plantingLocalDate, targetDate);

        Optional<CultivationPlan> activePlanOpt = cultivationPlanRepository
                .findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(planting.getId(), CultivationPlanStatus.ACTIVE);

        if (activePlanOpt.isEmpty()) {
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.NO_ACTIVE_PLAN)
                    .elapsedDays(elapsedDays)
                    .message("No active cultivation plan found for crop planting.")
                    .build();
        }

        CultivationPlan activePlan = activePlanOpt.get();
        CultivationTemplate template = activePlan.getTemplate();

        if (template == null) {
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.STAGE_DEFINITION_ERROR)
                    .elapsedDays(elapsedDays)
                    .templateVersion(activePlan.getTemplateVersion())
                    .message("Active cultivation plan is not linked to a valid cultivation template.")
                    .build();
        }

        List<CultivationStage> rawStages = template.getStages();
        if (rawStages == null || rawStages.isEmpty()) {
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.STAGE_DEFINITION_ERROR)
                    .elapsedDays(elapsedDays)
                    .templateName(template.getCropName())
                    .templateVersion(activePlan.getTemplateVersion())
                    .message("Cultivation template has no defined growth stages.")
                    .build();
        }

        List<CultivationStage> stages = rawStages.stream()
                .sorted(Comparator.comparingInt(CultivationStage::getStageOrder))
                .collect(Collectors.toList());

        // Validate internal boundary integrity of individual stages
        for (CultivationStage s : stages) {
            if (s.getStartDayOffset() == null || s.getEndDayOffset() == null) {
                return EstimatedGrowthStageResponse.builder()
                        .status(GrowthStageStatus.STAGE_DEFINITION_ERROR)
                        .elapsedDays(elapsedDays)
                        .templateName(template.getCropName())
                        .templateVersion(activePlan.getTemplateVersion())
                        .message(String.format("Cultivation stage '%s' contains null boundary offsets.", s.getStageName()))
                        .build();
            }
            if (s.getStartDayOffset() > s.getEndDayOffset()) {
                return EstimatedGrowthStageResponse.builder()
                        .status(GrowthStageStatus.STAGE_DEFINITION_ERROR)
                        .elapsedDays(elapsedDays)
                        .templateName(template.getCropName())
                        .templateVersion(activePlan.getTemplateVersion())
                        .message(String.format("Cultivation stage '%s' has startDayOffset (%d) greater than endDayOffset (%d).",
                                s.getStageName(), s.getStartDayOffset(), s.getEndDayOffset()))
                        .build();
            }
        }

        int minDayOffset = stages.stream().mapToInt(CultivationStage::getStartDayOffset).min().orElse(0);
        int maxStageEndOffset = stages.stream().mapToInt(CultivationStage::getEndDayOffset).max().orElse(0);
        int cycleDuration = template.getDurationDays() != null
                ? Math.max(template.getDurationDays(), maxStageEndOffset)
                : maxStageEndOffset;

        // Global Pre-Planting check
        if (elapsedDays < minDayOffset) {
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.PRE_PLANTING)
                    .elapsedDays(elapsedDays)
                    .overallProgressPercentage(0.0)
                    .templateName(template.getCropName())
                    .templateVersion(activePlan.getTemplateVersion())
                    .message(String.format("Crop planting date is in the future (%d days remaining until planting; template schedule begins at day %d).",
                            -elapsedDays, minDayOffset))
                    .build();
        }

        // Expected harvest date check: ensure POST_HARVEST is not reported incorrectly for missing or inconsistent harvest dates
        boolean hasConsistentExpectedHarvest = planting.getExpectedHarvestDate() != null
                && !planting.getExpectedHarvestDate().toLocalDate().isBefore(plantingLocalDate);

        if (hasConsistentExpectedHarvest && targetDate.isAfter(planting.getExpectedHarvestDate().toLocalDate())) {
            LocalDate harvestDate = planting.getExpectedHarvestDate().toLocalDate();
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.POST_HARVEST)
                    .elapsedDays(elapsedDays)
                    .overallProgressPercentage(100.0)
                    .templateName(template.getCropName())
                    .templateVersion(activePlan.getTemplateVersion())
                    .message(String.format("Crop has passed expected harvest date (%s; %d days elapsed).",
                            harvestDate, elapsedDays))
                    .build();
        }

        // Global Post-Harvest check (beyond template duration or max stage offset)
        if (elapsedDays > cycleDuration) {
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.POST_HARVEST)
                    .elapsedDays(elapsedDays)
                    .overallProgressPercentage(100.0)
                    .templateName(template.getCropName())
                    .templateVersion(activePlan.getTemplateVersion())
                    .message(String.format("Cultivation cycle completed (%d days elapsed exceeding planned duration of %d days).",
                            elapsedDays, cycleDuration))
                    .build();
        }

        // Match stages whose interval [startDayOffset, endDayOffset] covers elapsedDays
        List<CultivationStage> matchingStages = stages.stream()
                .filter(s -> s.getStartDayOffset() <= elapsedDays && elapsedDays <= s.getEndDayOffset())
                .collect(Collectors.toList());

        if (matchingStages.isEmpty()) {
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.STAGE_DEFINITION_ERROR)
                    .elapsedDays(elapsedDays)
                    .templateName(template.getCropName())
                    .templateVersion(activePlan.getTemplateVersion())
                    .message(String.format("Inconsistent stage definitions: elapsed day %d falls into an undefined gap between stages.", elapsedDays))
                    .build();
        }

        CultivationStage resolvedStage;
        if (matchingStages.size() == 1) {
            resolvedStage = matchingStages.get(0);
        } else if (matchingStages.size() == 2) {
            CultivationStage first = matchingStages.get(0);
            CultivationStage second = matchingStages.get(1);

            // Verify if this is a legitimate shared boundary between two strictly adjacent sequential stages
            boolean isAdjacentInOrder = (first.getStageOrder() + 1 == second.getStageOrder());
            boolean isExactSharedBoundary = first.getEndDayOffset() != null
                    && second.getStartDayOffset() != null
                    && first.getEndDayOffset().equals(second.getStartDayOffset())
                    && first.getEndDayOffset().equals((int) elapsedDays);

            if (isAdjacentInOrder && isExactSharedBoundary) {
                // At the exact boundary day, deterministically transition into the entering stage (second)
                resolvedStage = second;
            } else {
                String overlappingNames = matchingStages.stream()
                        .map(s -> String.format("'%s' [%d, %d]", s.getStageName(), s.getStartDayOffset(), s.getEndDayOffset()))
                        .collect(Collectors.joining(" and "));
                return EstimatedGrowthStageResponse.builder()
                        .status(GrowthStageStatus.STAGE_DEFINITION_ERROR)
                        .elapsedDays(elapsedDays)
                        .templateName(template.getCropName())
                        .templateVersion(activePlan.getTemplateVersion())
                        .message(String.format("Ambiguous overlapping stage definitions: stages %s both claim elapsed day %d.",
                                overlappingNames, elapsedDays))
                        .build();
            }
        } else {
            String overlappingNames = matchingStages.stream()
                    .map(s -> String.format("'%s' [%d, %d]", s.getStageName(), s.getStartDayOffset(), s.getEndDayOffset()))
                    .collect(Collectors.joining(" and "));
            return EstimatedGrowthStageResponse.builder()
                    .status(GrowthStageStatus.STAGE_DEFINITION_ERROR)
                    .elapsedDays(elapsedDays)
                    .templateName(template.getCropName())
                    .templateVersion(activePlan.getTemplateVersion())
                    .message(String.format("Ambiguous overlapping stage definitions: stages %s claim elapsed day %d.",
                            overlappingNames, elapsedDays))
                    .build();
        }

        int start = resolvedStage.getStartDayOffset();
        int end = resolvedStage.getEndDayOffset();

        double stageProgress;
        if (end > start) {
            stageProgress = ((double) (elapsedDays - start) / (end - start)) * 100.0;
        } else {
            stageProgress = 100.0;
        }
        stageProgress = Math.max(0.0, Math.min(100.0, Math.round(stageProgress * 10.0) / 10.0));

        Double overallProgress = 0.0;
        if (cycleDuration > 0) {
            if (elapsedDays <= 0) {
                overallProgress = 0.0;
            } else {
                double overall = ((double) elapsedDays / cycleDuration) * 100.0;
                overallProgress = Math.max(0.0, Math.min(100.0, Math.round(overall * 10.0) / 10.0));
            }
        }

        return EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.RESOLVED)
                .stageId(resolvedStage.getId())
                .stageName(resolvedStage.getStageName())
                .stageOrder(resolvedStage.getStageOrder())
                .totalStages(stages.size())
                .elapsedDays(elapsedDays)
                .stageStartDay(start)
                .stageEndDay(end)
                .stageProgressPercentage(stageProgress)
                .overallProgressPercentage(overallProgress)
                .templateName(template.getCropName())
                .templateVersion(activePlan.getTemplateVersion())
                .message(String.format("Estimated stage '%s' (Stage %d of %d, Day %d of [%d, %d]) based on template %s v%s.",
                        resolvedStage.getStageName(), resolvedStage.getStageOrder(), stages.size(), elapsedDays, start, end,
                        template.getCropName(), activePlan.getTemplateVersion()))
                .build();
    }

    /**
     * Resolves the growth stage for a specific crop planting ID, enforcing authentication and ownership isolation.
     *
     * @param plantingId  the crop planting identifier
     * @param userDetails the authenticated user principal
     * @return the estimated growth stage response
     */
    @PreAuthorize("hasRole('FARMER')")
    @Transactional(readOnly = true)
    public EstimatedGrowthStageResponse resolveGrowthStage(UUID plantingId, CustomUserDetails userDetails) {
        return resolveGrowthStage(plantingId, LocalDate.now(), userDetails);
    }

    /**
     * Resolves the growth stage for a specific crop planting ID as of a given date,
     * enforcing authentication and ownership isolation.
     *
     * @param plantingId  the crop planting identifier
     * @param asOfDate    the evaluation date
     * @param userDetails the authenticated user principal
     * @return the estimated growth stage response
     */
    @PreAuthorize("hasRole('FARMER')")
    @Transactional(readOnly = true)
    public EstimatedGrowthStageResponse resolveGrowthStage(UUID plantingId, LocalDate asOfDate, CustomUserDetails userDetails) {
        validateFarmerRole(userDetails);

        CropPlanting planting = cropPlantingRepository.findById(plantingId)
                .orElseThrow(() -> new IllegalArgumentException("Crop planting not found with id: " + plantingId));

        validatePlantingOwnership(planting, userDetails.getUserId());

        return resolveGrowthStage(planting, asOfDate);
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

    private void validatePlantingOwnership(CropPlanting cropPlanting, UUID userId) {
        if (!cropPlanting.getCropCycle().getFarm().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied. You do not own the parent farm of this crop planting.");
        }
    }
}
