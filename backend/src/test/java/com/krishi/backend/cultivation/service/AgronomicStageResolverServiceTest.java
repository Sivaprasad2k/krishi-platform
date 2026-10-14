package com.krishi.backend.cultivation.service;

import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import com.krishi.backend.cultivation.dto.GrowthStageStatus;
import com.krishi.backend.cultivation.entity.*;
import com.krishi.backend.cultivation.repository.CultivationPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgronomicStageResolverServiceTest {

    @Mock
    private CultivationPlanRepository cultivationPlanRepository;

    @InjectMocks
    private AgronomicStageResolverService stageResolverService;

    private CropPlanting planting;
    private CultivationPlan activePlan;
    private CultivationTemplate template;
    private UUID plantingId;

    @BeforeEach
    void setUp() {
        plantingId = UUID.randomUUID();
        planting = new CropPlanting();
        planting.setId(plantingId);
        planting.setCropName("Rice (Paddy)");
        planting.setPlantingDate(LocalDateTime.of(2026, 6, 1, 9, 0));

        template = new CultivationTemplate();
        template.setId(UUID.randomUUID());
        template.setCropName("Rice (Paddy)");
        template.setDurationDays(120);

        activePlan = new CultivationPlan();
        activePlan.setId(UUID.randomUUID());
        activePlan.setCropPlanting(planting);
        activePlan.setTemplate(template);
        activePlan.setTemplateVersion("2024.1");
        activePlan.setStatus(CultivationPlanStatus.ACTIVE);
    }

    private CultivationStage createStage(String name, int order, int start, int end) {
        CultivationStage stage = new CultivationStage();
        stage.setId(UUID.randomUUID());
        stage.setTemplate(template);
        stage.setStageName(name);
        stage.setStageOrder(order);
        stage.setStartDayOffset(start);
        stage.setEndDayOffset(end);
        return stage;
    }

    @Test
    @DisplayName("1. Day Zero Planting: Resolves to field establishment stage, not pre-planting nursery")
    void testPlantingOnDayZero_RiceEstablishmentResolved() {
        // Rice has Stage 1: [-15, 0] (Nursery) and Stage 2: [0, 20] (Transplanting & Establishment)
        CultivationStage nursery = createStage("Nursery & Land Preparation", 1, -15, 0);
        CultivationStage transplanting = createStage("Transplanting & Establishment", 2, 0, 20);
        template.setStages(List.of(nursery, transplanting));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // Evaluate on 2026-06-01 (Day 0)
        LocalDate asOfDate = LocalDate.of(2026, 6, 1);
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertNotNull(result);
        assertEquals(GrowthStageStatus.RESOLVED, result.getStatus());
        assertEquals("Transplanting & Establishment", result.getStageName());
        assertEquals(2, result.getStageOrder());
        assertEquals(0L, result.getElapsedDays());
        assertEquals(0, result.getStageStartDay());
        assertEquals(20, result.getStageEndDay());
        assertEquals(0.0, result.getStageProgressPercentage());
        assertEquals(0.0, result.getOverallProgressPercentage());
    }

    @Test
    @DisplayName("2. Stage Start and End Boundaries: Wheat CRI stage boundaries at Day 0 and Day 25")
    void testStageStartAndEndBoundaries_Wheat() {
        CultivationStage cri = createStage("Sowing & Crown Root Initiation", 1, 0, 25);
        CultivationStage tillering = createStage("Tillering & Jointing Stage", 2, 26, 65);
        template.setStages(List.of(cri, tillering));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // Start boundary: Day 0
        EstimatedGrowthStageResponse atStart = stageResolverService.resolveGrowthStage(planting, LocalDate.of(2026, 6, 1));
        assertEquals(GrowthStageStatus.RESOLVED, atStart.getStatus());
        assertEquals("Sowing & Crown Root Initiation", atStart.getStageName());
        assertEquals(0.0, atStart.getStageProgressPercentage());

        // End boundary: Day 25
        EstimatedGrowthStageResponse atEnd = stageResolverService.resolveGrowthStage(planting, LocalDate.of(2026, 6, 26));
        assertEquals(GrowthStageStatus.RESOLVED, atEnd.getStatus());
        assertEquals("Sowing & Crown Root Initiation", atEnd.getStageName());
        assertEquals(25L, atEnd.getElapsedDays());
        assertEquals(100.0, atEnd.getStageProgressPercentage());
    }

    @Test
    @DisplayName("3. Transition Between Consecutive Stages: Day 25 to Day 26 in Wheat")
    void testTransitionBetweenConsecutiveStages_Wheat() {
        CultivationStage cri = createStage("Sowing & Crown Root Initiation", 1, 0, 25);
        CultivationStage tillering = createStage("Tillering & Jointing Stage", 2, 26, 65);
        template.setStages(List.of(cri, tillering));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // Day 25 is Stage 1
        EstimatedGrowthStageResponse day25 = stageResolverService.resolveGrowthStage(planting, LocalDate.of(2026, 6, 26));
        assertEquals("Sowing & Crown Root Initiation", day25.getStageName());
        assertEquals(1, day25.getStageOrder());

        // Day 26 is Stage 2
        EstimatedGrowthStageResponse day26 = stageResolverService.resolveGrowthStage(planting, LocalDate.of(2026, 6, 27));
        assertEquals("Tillering & Jointing Stage", day26.getStageName());
        assertEquals(2, day26.getStageOrder());
        assertEquals(26L, day26.getElapsedDays());
        assertEquals(0.0, day26.getStageProgressPercentage());
    }

    @Test
    @DisplayName("4. Missing Cultivation Plan: Handled explicitly without fabricating a confirmed stage")
    void testMissingCultivationPlan_ReturnsNoActivePlan() {
        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());

        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, LocalDate.of(2026, 6, 15));

        assertNotNull(result);
        assertEquals(GrowthStageStatus.NO_ACTIVE_PLAN, result.getStatus());
        assertNull(result.getStageName());
        assertNull(result.getStageId());
        assertNull(result.getStageOrder());
        assertEquals(14L, result.getElapsedDays());
        assertTrue(result.getMessage().contains("No active cultivation plan found"));
    }

    @Test
    @DisplayName("5. Future Planting Dates: Resolves pre-planting nursery stage if covered")
    void testFuturePlantingDate_PrePlantingNurseryResolved() {
        CultivationStage nursery = createStage("Nursery & Land Preparation", 1, -15, 0);
        CultivationStage transplanting = createStage("Transplanting & Establishment", 2, 0, 20);
        template.setStages(List.of(nursery, transplanting));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // 7 days before planting: 2026-05-25 (-7 days)
        LocalDate asOfDate = LocalDate.of(2026, 5, 25);
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.RESOLVED, result.getStatus());
        assertEquals("Nursery & Land Preparation", result.getStageName());
        assertEquals(-7L, result.getElapsedDays());
        assertEquals(1, result.getStageOrder());
        assertEquals(0.0, result.getOverallProgressPercentage());
    }

    @Test
    @DisplayName("6. Future Planting Dates: Pre-planting status when target date precedes all stage definitions")
    void testFuturePlantingDate_BeforeAnyStage() {
        CultivationStage s1 = createStage("Vegetative", 1, 0, 20);
        template.setStages(List.of(s1));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // 10 days before planting
        LocalDate asOfDate = LocalDate.of(2026, 5, 22);
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.PRE_PLANTING, result.getStatus());
        assertNull(result.getStageName());
        assertEquals(-10L, result.getElapsedDays());
        assertEquals(0.0, result.getOverallProgressPercentage());
        assertTrue(result.getMessage().contains("future"));
    }

    @Test
    @DisplayName("7. Beyond Expected Harvest: Handled as POST_HARVEST without fabricating a stage")
    void testPlantingBeyondExpectedHarvest_ReturnsPostHarvest() {
        template.setDurationDays(95);
        CultivationStage lastStage = createStage("Fruit Maturation & Harvesting", 4, 61, 95);
        template.setStages(List.of(lastStage));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // 100 days after planting
        LocalDate asOfDate = LocalDate.of(2026, 9, 9);
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.POST_HARVEST, result.getStatus());
        assertNull(result.getStageName());
        assertEquals(100L, result.getElapsedDays());
        assertEquals(100.0, result.getOverallProgressPercentage());
        assertTrue(result.getMessage().contains("completed"));
    }

    @Test
    @DisplayName("8. Missing Stage Definitions: Returns STAGE_DEFINITION_ERROR when template has no stages")
    void testMissingStageDefinitions_ReturnsError() {
        template.setStages(Collections.emptyList());

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, LocalDate.of(2026, 6, 10));

        assertEquals(GrowthStageStatus.STAGE_DEFINITION_ERROR, result.getStatus());
        assertNull(result.getStageName());
        assertTrue(result.getMessage().contains("no defined growth stages"));
    }

    @Test
    @DisplayName("9. Inconsistent Stage Definitions: Inverted boundaries (start > end)")
    void testInconsistentStageDefinitions_InvertedOffsets() {
        CultivationStage corrupt = createStage("Corrupt Stage", 1, 30, 10);
        template.setStages(List.of(corrupt));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, LocalDate.of(2026, 6, 15));

        assertEquals(GrowthStageStatus.STAGE_DEFINITION_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("greater than endDayOffset"));
    }

    @Test
    @DisplayName("10. Inconsistent Stage Definitions: Ambiguous overlapping boundaries does NOT silently pick an arbitrary stage")
    void testAmbiguousOverlappingStages_FlagsErrorWithoutSilentSelection() {
        CultivationStage s1 = createStage("Stage Alpha", 1, 0, 30);
        CultivationStage s2 = createStage("Stage Beta", 2, 20, 50);
        template.setStages(List.of(s1, s2));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // Day 25 falls in BOTH Stage Alpha [0, 30] and Stage Beta [20, 50]
        LocalDate asOfDate = LocalDate.of(2026, 6, 26); // Day 25
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.STAGE_DEFINITION_ERROR, result.getStatus());
        assertNull(result.getStageName());
        assertTrue(result.getMessage().contains("Ambiguous overlapping stage definitions"));
        assertTrue(result.getMessage().contains("Stage Alpha"));
        assertTrue(result.getMessage().contains("Stage Beta"));
    }

    @Test
    @DisplayName("11. Inconsistent Stage Definitions: Undefined gap between stages flagged explicitly")
    void testUndefinedGapBetweenStages_FlagsError() {
        CultivationStage s1 = createStage("Stage Early", 1, 0, 10);
        CultivationStage s2 = createStage("Stage Late", 2, 20, 40);
        template.setStages(List.of(s1, s2));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // Day 15 falls in undefined gap [11, 19]
        LocalDate asOfDate = LocalDate.of(2026, 6, 16); // Day 15
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.STAGE_DEFINITION_ERROR, result.getStatus());
        assertNull(result.getStageName());
        assertTrue(result.getMessage().contains("undefined gap"));
    }

    @Test
    @DisplayName("12. Missing Planting Date: Handled explicitly with MISSING_PLANTING_DATE status")
    void testMissingPlantingDate_ReturnsMissingPlantingDateStatus() {
        planting.setPlantingDate(null);

        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, LocalDate.now());

        assertNotNull(result);
        assertEquals(GrowthStageStatus.MISSING_PLANTING_DATE, result.getStatus());
        assertNull(result.getStageName());
        assertNull(result.getElapsedDays());
    }

    @Test
    @DisplayName("13. Null CropPlanting Parameter: Throws IllegalArgumentException")
    void testNullPlanting_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> stageResolverService.resolveGrowthStage(null));
    }

    @Test
    @DisplayName("14. Shared Boundary Between Adjacent Stages (Mid-Cycle): Deterministically transitions into succeeding stage")
    void testSharedBoundaryBetweenAdjacentStages_MidCycle() {
        CultivationStage s1 = createStage("Vegetative Growth", 1, 0, 20);
        CultivationStage s2 = createStage("Tillering & Flowering", 2, 20, 50);
        template.setStages(List.of(s1, s2));

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // At Day 20, both s1 [0, 20] and s2 [20, 50] match -> deterministically resolves to s2
        LocalDate asOfDate = LocalDate.of(2026, 6, 21); // Day 20
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.RESOLVED, result.getStatus());
        assertEquals("Tillering & Flowering", result.getStageName());
        assertEquals(2, result.getStageOrder());
        assertEquals(20L, result.getElapsedDays());
        assertEquals(0.0, result.getStageProgressPercentage());
    }

    @Test
    @DisplayName("15. Expected Harvest Date: Consistent date in past triggers POST_HARVEST")
    void testExpectedHarvestDate_PastDateReturnsPostHarvest() {
        planting.setExpectedHarvestDate(planting.getPlantingDate().plusDays(90));
        CultivationStage s1 = createStage("Vegetative", 1, 0, 50);
        CultivationStage s2 = createStage("Reproductive", 2, 51, 120);
        template.setStages(List.of(s1, s2));
        template.setDurationDays(120);

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // 95 days elapsed: beyond expectedHarvestDate (90 days) but before template duration (120 days)
        LocalDate asOfDate = LocalDate.of(2026, 9, 4); // Day 95
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.POST_HARVEST, result.getStatus());
        assertNull(result.getStageName());
        assertEquals(95L, result.getElapsedDays());
        assertEquals(100.0, result.getOverallProgressPercentage());
        assertTrue(result.getMessage().contains("expected harvest date"));
    }

    @Test
    @DisplayName("16. Expected Harvest Date: Missing harvest date does NOT prematurely trigger POST_HARVEST")
    void testExpectedHarvestDate_MissingDateDoesNotTriggerPrematurePostHarvest() {
        planting.setExpectedHarvestDate(null);
        CultivationStage s1 = createStage("Vegetative", 1, 0, 50);
        CultivationStage s2 = createStage("Reproductive", 2, 51, 120);
        template.setStages(List.of(s1, s2));
        template.setDurationDays(120);

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // 95 days elapsed: before template duration (120 days)
        LocalDate asOfDate = LocalDate.of(2026, 9, 4); // Day 95
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.RESOLVED, result.getStatus());
        assertEquals("Reproductive", result.getStageName());
        assertEquals(95L, result.getElapsedDays());
    }

    @Test
    @DisplayName("17. Expected Harvest Date: Inconsistent date (before planting) does NOT trigger premature POST_HARVEST")
    void testExpectedHarvestDate_InconsistentDateIgnoredForPostHarvest() {
        // Inconsistent: harvest date is 10 days before planting date
        planting.setExpectedHarvestDate(planting.getPlantingDate().minusDays(10));
        CultivationStage s1 = createStage("Vegetative", 1, 0, 50);
        template.setStages(List.of(s1));
        template.setDurationDays(100);

        when(cultivationPlanRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(activePlan));

        // Day 15: Should resolve to Stage 1, NOT post-harvest
        LocalDate asOfDate = LocalDate.of(2026, 6, 16); // Day 15
        EstimatedGrowthStageResponse result = stageResolverService.resolveGrowthStage(planting, asOfDate);

        assertEquals(GrowthStageStatus.RESOLVED, result.getStatus());
        assertEquals("Vegetative", result.getStageName());
        assertEquals(15L, result.getElapsedDays());
    }
}
