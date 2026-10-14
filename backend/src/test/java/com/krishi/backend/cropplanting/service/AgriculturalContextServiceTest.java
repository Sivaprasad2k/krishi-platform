package com.krishi.backend.cropplanting.service;

import com.krishi.backend.common.exception.LocationUnavailableException;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropcycle.entity.CropCycle;
import com.krishi.backend.cropplanting.dto.context.*;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import com.krishi.backend.cultivation.dto.GrowthStageStatus;
import com.krishi.backend.cultivation.entity.CultivationPlan;
import com.krishi.backend.cultivation.entity.CultivationPlanStatus;
import com.krishi.backend.cultivation.entity.CultivationStage;
import com.krishi.backend.cultivation.entity.CultivationTemplate;
import com.krishi.backend.cultivation.entity.CultivationTemplateTask;
import com.krishi.backend.cultivation.repository.CultivationPlanRepository;
import com.krishi.backend.cultivation.service.AgronomicStageResolverService;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.identity.entity.User;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.entity.TaskType;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.weather.dto.CoordinateSource;
import com.krishi.backend.weather.dto.ResolvedLocation;
import com.krishi.backend.weather.dto.WeatherAdvisoryResponse;
import com.krishi.backend.weather.dto.WeatherForecastResponse;
import com.krishi.backend.weather.dto.WeatherObservation;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import com.krishi.backend.weather.entity.AdvisoryStatus;
import com.krishi.backend.weather.entity.WeatherAdvisory;
import com.krishi.backend.weather.provider.WeatherProviderException;
import com.krishi.backend.weather.repository.WeatherAdvisoryRepository;
import com.krishi.backend.weather.service.DistrictCentroidResolver;
import com.krishi.backend.weather.service.WeatherForecastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgriculturalContextServiceTest {

    @Mock
    private CropPlantingRepository cropPlantingRepository;

    @Mock
    private AgronomicStageResolverService agronomicStageResolverService;

    @Mock
    private CultivationPlanRepository planRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WeatherForecastService weatherForecastService;

    @Mock
    private WeatherAdvisoryRepository advisoryRepository;

    @Mock
    private DistrictCentroidResolver districtCentroidResolver;

    @InjectMocks
    private AgriculturalContextService contextService;

    private UUID farmerId;
    private CustomUserDetails farmerUserDetails;
    private Farm farm;
    private CropCycle cropCycle;
    private CropPlanting planting;
    private UUID plantingId;

    @BeforeEach
    void setUp() {
        farmerId = UUID.randomUUID();
        plantingId = UUID.randomUUID();

        farmerUserDetails = mock(CustomUserDetails.class);
        lenient().when(farmerUserDetails.getUserId()).thenReturn(farmerId);
        lenient().when(farmerUserDetails.getAuthorities())
                .thenAnswer(inv -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_FARMER")));

        User owner = new User();
        owner.setId(farmerId);

        farm = new Farm();
        farm.setId(UUID.randomUUID());
        farm.setName("Green Valley Farm");
        farm.setLocation("Aluva, Ernakulam");
        farm.setState("Kerala");
        farm.setDistrict("Ernakulam");
        farm.setLatitude(10.1076);
        farm.setLongitude(76.3516);
        farm.setOwner(owner);

        cropCycle = new CropCycle();
        cropCycle.setId(UUID.randomUUID());
        cropCycle.setFarm(farm);

        planting = new CropPlanting();
        planting.setId(plantingId);
        planting.setCropCycle(cropCycle);
        planting.setCropName("Rice");
        planting.setCropVariety("Jyothi (PTB 39)");
        planting.setPlantingDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        planting.setExpectedHarvestDate(LocalDateTime.of(2026, 9, 29, 0, 0));
    }

    @Test
    @DisplayName("1. Valid planting with active plan returns full context")
    void testValidPlantingWithActivePlanReturnsFullContext() {
        LocalDate asOfDate = LocalDate.of(2026, 6, 16); // Day 15

        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));

        EstimatedGrowthStageResponse stageResponse = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.RESOLVED)
                .stageName("Vegetative Stage")
                .stageOrder(2)
                .stageStartDay(1)
                .stageEndDay(35)
                .elapsedDays(15L)
                .stageProgressPercentage(41.18)
                .overallProgressPercentage(12.5)
                .build();
        when(agronomicStageResolverService.resolveGrowthStage(planting, asOfDate)).thenReturn(stageResponse);

        CultivationTemplate template = new CultivationTemplate();
        template.setCropName("Rice (Paddy)");
        template.setCropVariety("Jyothi");
        template.setSource("ICAR-KAU Package of Practices");
        template.setVersion("2026.1");
        template.setDurationDays(120);
        template.setStages(List.of(new CultivationStage(), new CultivationStage()));

        CultivationPlan plan = new CultivationPlan();
        plan.setId(UUID.randomUUID());
        plan.setPlanVersion(1);
        plan.setStatus(CultivationPlanStatus.ACTIVE);
        plan.setTemplate(template);
        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.of(plan));

        when(taskRepository.findByCultivationPlanId(plan.getId())).thenReturn(List.of(new Task(), new Task()));

        Task task1 = new Task();
        task1.setId(UUID.randomUUID());
        task1.setTitle("Transplanting Field Inspection");
        task1.setTaskType(TaskType.CROP_INSPECTION);
        task1.setStatus(TaskStatus.PENDING);
        task1.setDueDate(LocalDateTime.of(2026, 6, 10, 0, 0)); // Due 6 days ago -> Overdue!
        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(List.of(task1));

        ResolvedLocation location = ResolvedLocation.builder()
                .latitude(10.11)
                .longitude(76.35)
                .coordinateSource(CoordinateSource.FARM_GPS)
                .locationLabel("Ernakulam, Kerala")
                .build();
        when(districtCentroidResolver.resolveLocation(farm)).thenReturn(location);

        WeatherForecastResponse forecast = WeatherForecastResponse.builder()
                .farmId(farm.getId())
                .stale(false)
                .fetchedAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(2))
                .current(WeatherObservation.builder()
                        .temperatureCelsius(28.5)
                        .relativeHumidity(82)
                        .precipitationMm(5.0)
                        .windSpeedKmh(14.0)
                        .weatherCode(61)
                        .weatherCondition("Slight rain")
                        .build())
                .attribution("Weather data by Open-Meteo.com")
                .build();
        when(weatherForecastService.getWeatherForFarm(farm.getId(), farmerUserDetails)).thenReturn(forecast);

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, asOfDate, farmerUserDetails);

        assertNotNull(context);
        assertEquals(asOfDate, context.getAsOfDate());

        // Crop Identity assertions
        assertEquals("Rice", context.getCrop().getCropName());
        assertEquals("Jyothi (PTB 39)", context.getCrop().getCropVariety());
        assertEquals(15L, context.getCrop().getElapsedDays());

        // Growth stage duality assertions
        assertEquals(GrowthStageStatus.RESOLVED, context.getGrowthStage().getEstimatedStage().getStatus());
        assertEquals("Vegetative Stage", context.getGrowthStage().getEstimatedStage().getStageName());
        assertNull(context.getGrowthStage().getObservedStage()); // Observed stage is unconfirmed
        assertEquals("CALENDAR_DAY_MODEL_ESTIMATE", context.getGrowthStage().getAgronomicBasis());

        // Location assertions
        assertEquals("Green Valley Farm", context.getLocation().getFarmName());
        assertEquals(CoordinateSource.FARM_GPS, context.getLocation().getCoordinateSource());

        // Active plan assertions
        assertNotNull(context.getActivePlan());
        assertEquals(1, context.getActivePlan().getRevisionNumber());
        assertEquals("ACTIVE", context.getActivePlan().getStatus());
        assertEquals("ICAR-KAU Package of Practices", context.getActivePlan().getPackageSource());

        // Relevant tasks assertions
        assertEquals(1, context.getRelevantTasks().size());
        assertTrue(context.getRelevantTasks().get(0).isOverdue());
        assertEquals(6, context.getRelevantTasks().get(0).getDaysOverdue());

        // Weather assertions
        assertEquals(WeatherFreshnessStatus.FRESH, context.getWeather().getFreshness());
        assertNotNull(context.getWeather().getCurrent());
        assertEquals(28.5, context.getWeather().getCurrent().getTemperatureCelsius());
    }

    @Test
    @DisplayName("2. Planting without active plan returns NO_ACTIVE_PLAN and null plan summary")
    void testPlantingWithoutActivePlanReturnsNoActivePlan() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));

        EstimatedGrowthStageResponse noPlanStage = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.NO_ACTIVE_PLAN)
                .elapsedDays(10L)
                .build();
        when(agronomicStageResolverService.resolveGrowthStage(eq(planting), any())).thenReturn(noPlanStage);

        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());

        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(Collections.emptyList());

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, null, farmerUserDetails);

        assertNotNull(context);
        assertEquals(GrowthStageStatus.NO_ACTIVE_PLAN, context.getGrowthStage().getEstimatedStage().getStatus());
        assertNull(context.getActivePlan());
        assertTrue(context.getRelevantTasks().isEmpty());
    }

    @Test
    @DisplayName("3. Missing location returns LOCATION_UNAVAILABLE status without throwing exception")
    void testMissingLocationReturnsLocationUnavailableStatus() {
        farm.setLatitude(null);
        farm.setLongitude(null);
        farm.setDistrict(null);

        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agronomicStageResolverService.resolveGrowthStage(eq(planting), any()))
                .thenReturn(EstimatedGrowthStageResponse.builder().status(GrowthStageStatus.RESOLVED).build());
        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(Collections.emptyList());

        when(districtCentroidResolver.resolveLocation(farm))
                .thenThrow(new LocationUnavailableException("Farm coordinates and district are not configured"));

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, null, farmerUserDetails);

        assertNotNull(context);
        assertEquals(WeatherFreshnessStatus.LOCATION_UNAVAILABLE, context.getWeather().getFreshness());
        assertNull(context.getWeather().getCurrent());
        assertTrue(context.getWeather().getStatusMessage().contains("not configured"));
        assertNull(context.getLocation().getCoordinateSource());
    }

    @Test
    @DisplayName("4. Unavailable weather provider returns UNAVAILABLE status gracefully")
    void testUnavailableWeatherProviderReturnsUnavailableStatus() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agronomicStageResolverService.resolveGrowthStage(eq(planting), any()))
                .thenReturn(EstimatedGrowthStageResponse.builder().status(GrowthStageStatus.RESOLVED).build());
        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(Collections.emptyList());

        when(weatherForecastService.getWeatherForFarm(farm.getId(), farmerUserDetails))
                .thenThrow(new WeatherProviderException("External Open-Meteo outage"));

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, null, farmerUserDetails);

        assertNotNull(context);
        assertEquals(WeatherFreshnessStatus.UNAVAILABLE, context.getWeather().getFreshness());
        assertNull(context.getWeather().getCurrent());
        assertTrue(context.getWeather().getStatusMessage().contains("currently unavailable"));
    }

    @Test
    @DisplayName("5. Stale cached weather returns STALE freshness status")
    void testStaleCachedWeatherReturnsStaleStatus() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agronomicStageResolverService.resolveGrowthStage(eq(planting), any()))
                .thenReturn(EstimatedGrowthStageResponse.builder().status(GrowthStageStatus.RESOLVED).build());
        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(Collections.emptyList());

        WeatherForecastResponse staleForecast = WeatherForecastResponse.builder()
                .farmId(farm.getId())
                .stale(true)
                .fetchedAt(OffsetDateTime.now().minusHours(3))
                .current(WeatherObservation.builder()
                        .temperatureCelsius(26.0)
                        .relativeHumidity(75)
                        .build())
                .build();
        when(weatherForecastService.getWeatherForFarm(farm.getId(), farmerUserDetails)).thenReturn(staleForecast);

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, null, farmerUserDetails);

        assertNotNull(context);
        assertEquals(WeatherFreshnessStatus.STALE, context.getWeather().getFreshness());
        assertNotNull(context.getWeather().getCurrent());
        assertEquals(26.0, context.getWeather().getCurrent().getTemperatureCelsius());
        assertTrue(context.getWeather().getStatusMessage().contains("stale cached"));
    }

    @Test
    @DisplayName("6. Missing crop variety and optional plot return null without inventing values")
    void testMissingVarietyAndPlotReturnNull() {
        planting.setCropVariety(null);

        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agronomicStageResolverService.resolveGrowthStage(eq(planting), any()))
                .thenReturn(EstimatedGrowthStageResponse.builder().status(GrowthStageStatus.RESOLVED).build());
        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(Collections.emptyList());

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, null, farmerUserDetails);

        assertNotNull(context);
        assertNull(context.getCrop().getCropVariety());
        assertNull(context.getLocation().getPlotId());
        assertNull(context.getLocation().getPlotName());
    }

    @Test
    @DisplayName("7. Pre-planting date returns PRE_PLANTING status")
    void testPrePlantingDateReturnsPrePlanting() {
        LocalDate futurePlantingTarget = LocalDate.of(2026, 5, 1); // 1 month before planting date

        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));

        EstimatedGrowthStageResponse prePlantingStage = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.PRE_PLANTING)
                .elapsedDays(-31L)
                .overallProgressPercentage(0.0)
                .build();
        when(agronomicStageResolverService.resolveGrowthStage(planting, futurePlantingTarget)).thenReturn(prePlantingStage);
        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(Collections.emptyList());

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, futurePlantingTarget, farmerUserDetails);

        assertNotNull(context);
        assertEquals(GrowthStageStatus.PRE_PLANTING, context.getGrowthStage().getEstimatedStage().getStatus());
        assertEquals(-31L, context.getCrop().getElapsedDays());
    }

    @Test
    @DisplayName("8. Post-harvest date returns POST_HARVEST status and 100% progress")
    void testPostHarvestDateReturnsPostHarvest() {
        LocalDate postHarvestTarget = LocalDate.of(2026, 10, 15);

        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));

        EstimatedGrowthStageResponse postHarvestStage = EstimatedGrowthStageResponse.builder()
                .status(GrowthStageStatus.POST_HARVEST)
                .elapsedDays(136L)
                .overallProgressPercentage(100.0)
                .build();
        when(agronomicStageResolverService.resolveGrowthStage(planting, postHarvestTarget)).thenReturn(postHarvestStage);
        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(Collections.emptyList());

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, postHarvestTarget, farmerUserDetails);

        assertNotNull(context);
        assertEquals(GrowthStageStatus.POST_HARVEST, context.getGrowthStage().getEstimatedStage().getStatus());
        assertEquals(100.0, context.getGrowthStage().getEstimatedStage().getOverallProgressPercentage());
    }

    @Test
    @DisplayName("9. Active weather advisories for planting are included in WeatherContext")
    void testActiveWeatherAdvisoriesIncludedInContext() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agronomicStageResolverService.resolveGrowthStage(eq(planting), any()))
                .thenReturn(EstimatedGrowthStageResponse.builder().status(GrowthStageStatus.RESOLVED).build());
        when(planRepository.findFirstByCropPlantingIdAndStatusOrderByPlanVersionDesc(plantingId, CultivationPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)).thenReturn(Collections.emptyList());

        WeatherAdvisory advisory = new WeatherAdvisory();
        advisory.setId(UUID.randomUUID());
        advisory.setFarm(farm);
        advisory.setCropPlanting(planting);
        advisory.setTitle("Heavy Rain Advisory");
        advisory.setSeverity(AdvisorySeverity.HIGH);
        advisory.setStatus(AdvisoryStatus.ACTIVE);

        when(advisoryRepository.findByFarmIdAndStatusInOrderByTargetDateAsc(eq(farm.getId()), any()))
                .thenReturn(List.of(advisory));

        WeatherAdvisoryResponse advisoryResponse = WeatherAdvisoryResponse.builder()
                .id(advisory.getId())
                .title("Heavy Rain Advisory")
                .severity(AdvisorySeverity.HIGH)
                .build();
        when(weatherForecastService.mapToAdvisoryResponse(advisory)).thenReturn(advisoryResponse);

        AgriculturalContextResponse context = contextService.getAgriculturalContext(plantingId, null, farmerUserDetails);

        assertNotNull(context.getWeather().getActiveAdvisories());
        assertEquals(1, context.getWeather().getActiveAdvisories().size());
        assertEquals("Heavy Rain Advisory", context.getWeather().getActiveAdvisories().get(0).getTitle());
    }

    @Test
    @DisplayName("10. Cross-farmer access throws AccessDeniedException")
    void testCrossFarmerAccessThrowsAccessDeniedException() {
        User otherOwner = new User();
        otherOwner.setId(UUID.randomUUID()); // Different owner
        farm.setOwner(otherOwner);

        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));

        assertThrows(AccessDeniedException.class, () ->
                contextService.getAgriculturalContext(plantingId, null, farmerUserDetails)
        );
    }
}
