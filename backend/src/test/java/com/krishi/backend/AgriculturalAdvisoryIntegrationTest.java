package com.krishi.backend;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.krishi.backend.advisory.entity.AdvisoryStatus;
import com.krishi.backend.advisory.entity.AgriculturalAdvisory;
import com.krishi.backend.advisory.repository.AgriculturalAdvisoryRepository;
import com.krishi.backend.common.event.entity.DomainEvent;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.entity.SeasonType;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cultivation.dto.CultivationPlanGenerateRequest;
import com.krishi.backend.cultivation.repository.CultivationPlanRepository;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import com.krishi.backend.notification.entity.Notification;
import com.krishi.backend.notification.repository.NotificationRepository;
import com.krishi.backend.task.dto.TaskCreateRequest;
import com.krishi.backend.task.entity.TaskType;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import com.krishi.backend.weather.dto.DailyForecastItem;
import com.krishi.backend.weather.dto.WeatherObservation;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import com.krishi.backend.weather.provider.ProviderForecastResult;
import com.krishi.backend.weather.provider.WeatherDataProvider;
import com.krishi.backend.weather.repository.WeatherAdvisoryRepository;
import com.krishi.backend.weather.repository.WeatherForecastCacheRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AgriculturalAdvisoryIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private CropCycleRepository cropCycleRepository;

    @Autowired
    private CropPlantingRepository cropPlantingRepository;

    @Autowired
    private CultivationPlanRepository cultivationPlanRepository;

    @Autowired
    private DiseaseIncidentRepository diseaseIncidentRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DomainEventRepository domainEventRepository;

    @Autowired
    private WeatherForecastCacheRepository cacheRepository;

    @Autowired
    private WeatherAdvisoryRepository weatherAdvisoryRepository;

    @Autowired
    private AgriculturalAdvisoryRepository agriAdvisoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static volatile boolean simulateWeatherFailure = false;

    @TestConfiguration
    static class AdvisoryWeatherTestConfig {
        @Bean
        @Primary
        public WeatherDataProvider mockAdvisoryWeatherProvider() {
            return new WeatherDataProvider() {
                @Override
                public String getProviderName() {
                    return "MOCK_ADVISORY_WEATHER";
                }

                @Override
                public String getAttribution() {
                    return "Mock Weather Service";
                }

                @Override
                public String getAttributionUrl() {
                    return "https://mock.krishi.com";
                }

                @Override
                public ProviderForecastResult fetchForecast(double latitude, double longitude) {
                    if (simulateWeatherFailure) {
                        throw new com.krishi.backend.weather.provider.WeatherProviderException("Simulated weather provider outage");
                    }

                    LocalDate today = LocalDate.now();

                    WeatherObservation obs = WeatherObservation.builder()
                            .temperatureCelsius(28.0)
                            .relativeHumidity(85)
                            .precipitationMm(15.0)
                            .windSpeedKmh(10.0)
                            .weatherCode(63)
                            .weatherCondition("Moderate rain")
                            .observedAt(OffsetDateTime.now())
                            .build();

                    // Today has heavy rain (55mm, 90% probability) -> triggers IRRIGATION_RAINFALL_CONFLICT (HIGH)
                    DailyForecastItem day0 = DailyForecastItem.builder()
                            .date(today)
                            .maxTempCelsius(30.0)
                            .minTempCelsius(23.0)
                            .precipitationMm(55.0)
                            .precipitationProbabilityMax(90)
                            .maxWindSpeedKmh(12.0)
                            .maxWindGustsKmh(18.0)
                            .build();

                    // Tomorrow has high wind (28 km/h sustained, 38 km/h gusts) -> triggers CHEMICAL_SPRAYING_WIND_RISK
                    DailyForecastItem day1 = DailyForecastItem.builder()
                            .date(today.plusDays(1))
                            .maxTempCelsius(32.0)
                            .minTempCelsius(24.0)
                            .precipitationMm(2.0)
                            .precipitationProbabilityMax(30)
                            .maxWindSpeedKmh(28.0)
                            .maxWindGustsKmh(38.0)
                            .build();

                    return ProviderForecastResult.builder()
                            .current(obs)
                            .daily(List.of(day0, day1))
                            .hourly(Collections.emptyList())
                            .fetchedAt(OffsetDateTime.now())
                            .rawPayloadJson("{\"mock\":true}")
                            .build();
                }
            };
        }
    }

    private String farmerToken;
    private String farmerBToken;
    private UUID farmId;
    private UUID cropCycleId;
    private static final UUID RICE_TEMPLATE_ID = UUID.fromString("a0000001-0000-0000-0000-000000000001");

    @BeforeEach
    public void setup() throws Exception {
        simulateWeatherFailure = false;

        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });
        objectMapper.registerModule(module);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        cleanupDatabase();

        farmerToken = registerAndLogin("Farmer Adv A", "9847222001", "adv_a@krishi.com", "Password123!");
        farmerBToken = registerAndLogin("Farmer Adv B", "9847222002", "adv_b@krishi.com", "Password123!");

        // Create Farm for Farmer A
        FarmCreateRequest farmRequest = new FarmCreateRequest();
        farmRequest.setName("Alappuzha Rice Farm");
        farmRequest.setSizeHectares(2.8);
        farmRequest.setLocation("Kuttanad, Alappuzha");
        farmRequest.setState("Kerala");
        farmRequest.setDistrict("Alappuzha");
        farmRequest.setLatitude(9.4981);
        farmRequest.setLongitude(76.3388);

        MvcResult farmResult = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmRequest)))
                .andExpect(status().isOk())
                .andReturn();

        farmId = UUID.fromString(objectMapper.readTree(farmResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Create CropCycle for Farm
        CropCycleCreateRequest cycleRequest = new CropCycleCreateRequest();
        cycleRequest.setName("Virippu 2026");
        cycleRequest.setSeasonType(SeasonType.MONSOON);
        cycleRequest.setYear(2026);
        cycleRequest.setFarmId(farmId);

        MvcResult cycleResult = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleRequest)))
                .andExpect(status().isOk())
                .andReturn();

        cropCycleId = UUID.fromString(objectMapper.readTree(cycleResult.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    @AfterEach
    public void cleanup() {
        simulateWeatherFailure = false;
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        agriAdvisoryRepository.deleteAll();
        weatherAdvisoryRepository.deleteAll();
        notificationRepository.deleteAll();
        domainEventRepository.deleteAll();
        taskRepository.deleteAll();
        treatmentRepository.deleteAll();
        diseaseIncidentRepository.deleteAll();
        cultivationPlanRepository.deleteAll();
        cropPlantingRepository.deleteAll();
        cropCycleRepository.deleteAll();
        cacheRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(String name, String phone, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName(name);
        registerRequest.setPhoneNumber(phone);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhoneNumber(phone);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private UUID createPlantingWithPlan(LocalDate plantingDate) throws Exception {
        CropPlantingCreateRequest plantingReq = new CropPlantingCreateRequest();
        plantingReq.setCropCycleId(cropCycleId);
        plantingReq.setCropName("Paddy");
        plantingReq.setCropVariety("Jyothi");
        plantingReq.setPlantingDate(plantingDate.atTime(8, 0));
        plantingReq.setExpectedHarvestDate(plantingDate.plusDays(120).atTime(17, 0));

        MvcResult plantingResult = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plantingReq)))
                .andExpect(status().isOk())
                .andReturn();

        UUID plantingId = UUID.fromString(objectMapper.readTree(plantingResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Generate cultivation plan
        CultivationPlanGenerateRequest planReq = new CultivationPlanGenerateRequest();
        planReq.setTemplateId(RICE_TEMPLATE_ID);
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isOk());

        return plantingId;
    }

    private UUID createManualTask(UUID plantingId, String title, TaskType type, LocalDateTime dueDate) throws Exception {
        TaskCreateRequest taskReq = new TaskCreateRequest();
        taskReq.setCropPlantingId(plantingId);
        taskReq.setTitle(title);
        taskReq.setDescription("Automated test task");
        taskReq.setTaskType(type);
        taskReq.setDueDate(dueDate);

        MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    @Test
    @DisplayName("Evaluate Advisories: Generates high-value advisories, persists them, notifies, and logs domain events")
    void testContextToAdvisoryEvaluation_HeavyRainAndOverdue() throws Exception {
        LocalDate today = LocalDate.now();
        UUID plantingId = createPlantingWithPlan(today.minusDays(15)); // Vegetative Tillering stage

        // 1. Irrigation task scheduled for today -> Heavy rain forecast (55mm) triggers IRRIGATION_RAINFALL_CONFLICT (HIGH)
        createManualTask(plantingId, "Field Inundation Irrigation", TaskType.IRRIGATION, today.atTime(10, 0));

        // 2. Overdue task (due 4 days ago) -> Triggers OVERDUE_CULTIVATION_TASK
        createManualTask(plantingId, "Delayed Hand Weeding", TaskType.WEEDING, today.minusDays(4).atTime(9, 0));

        // Trigger advisory evaluation
        MvcResult evalResult = mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode rootJson = objectMapper.readTree(evalResult.getResponse().getContentAsString());
        assertTrue(rootJson.path("success").asBoolean());
        assertEquals("Agricultural advisories evaluated successfully", rootJson.path("message").asText());
        JsonNode evalJson = rootJson.path("data");
        assertEquals(plantingId.toString(), evalJson.path("cropPlantingId").asText());
        assertEquals(4, evalJson.path("totalRulesEvaluated").asInt());
        assertTrue(evalJson.path("activeAdvisoriesCount").asInt() >= 2);

        // Verify database persistence in krishi.agricultural_advisories
        List<AgriculturalAdvisory> persisted = agriAdvisoryRepository.findByCropPlantingIdOrderByCreatedAtDesc(plantingId);
        assertFalse(persisted.isEmpty());
        assertTrue(persisted.stream().anyMatch(a -> "IRRIGATION_RAINFALL_CONFLICT".equals(a.getRuleCode()) && a.getSeverity() == AdvisorySeverity.HIGH));
        assertTrue(persisted.stream().anyMatch(a -> "OVERDUE_CULTIVATION_TASK".equals(a.getRuleCode())));

        // Verify in-app notifications in krishi.notifications
        List<Notification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());
        assertTrue(notifications.stream().anyMatch(n -> n.getTitle().contains("Heavy Rain") || n.getTitle().contains("Overdue Task")));

        // Verify domain event auditing in krishi.domain_events
        List<DomainEvent> events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "AGRICULTURAL_ADVISORY_CREATED".equals(e.getEventType())));
    }

    @Test
    @DisplayName("Idempotency: Repeated evaluations update timestamps without duplicating active advisories or notifications")
    void testRepeatedEvaluationIdempotency() throws Exception {
        LocalDate today = LocalDate.now();
        UUID plantingId = createPlantingWithPlan(today.minusDays(10));

        createManualTask(plantingId, "Morning Irrigation", TaskType.IRRIGATION, today.atTime(7, 0));

        // Evaluation 1
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        long countAfterFirst = agriAdvisoryRepository.count();
        long notifAfterFirst = notificationRepository.count();
        assertTrue(countAfterFirst >= 1);

        // Evaluation 2 (immediate repeat)
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        long countAfterSecond = agriAdvisoryRepository.count();
        long notifAfterSecond = notificationRepository.count();

        // Exactly zero new advisories and zero new notifications created!
        assertEquals(countAfterFirst, countAfterSecond);
        assertEquals(notifAfterFirst, notifAfterSecond);
    }

    @Test
    @DisplayName("Query Advisories: Farmer can filter advisories by status")
    void testGetAdvisoriesFiltering() throws Exception {
        LocalDate today = LocalDate.now();
        UUID plantingId = createPlantingWithPlan(today.minusDays(10));
        createManualTask(plantingId, "Irrigation Plot 2", TaskType.IRRIGATION, today.atTime(8, 0));

        // Evaluate to create advisory
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Query with status=ACTIVE
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/advisories?status=ACTIVE")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        // Query with status=RESOLVED -> should be empty
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/advisories?status=RESOLVED")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Advisory Lifecycle: Acknowledge and resolve transitions with domain events")
    void testAdvisoryLifecycleTransitions() throws Exception {
        LocalDate today = LocalDate.now();
        UUID plantingId = createPlantingWithPlan(today.minusDays(10));
        createManualTask(plantingId, "Scheduled Drip", TaskType.IRRIGATION, today.atTime(9, 0));

        // Evaluate
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<AgriculturalAdvisory> list = agriAdvisoryRepository.findByCropPlantingIdOrderByCreatedAtDesc(plantingId);
        assertFalse(list.isEmpty());
        UUID advisoryId = list.get(0).getId();

        // 1. Acknowledge advisory
        mockMvc.perform(patch("/api/v1/advisories/" + advisoryId + "/acknowledge")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.data.acknowledgedAt").isNotEmpty());

        AgriculturalAdvisory acknowledged = agriAdvisoryRepository.findById(advisoryId).orElseThrow();
        assertEquals(AdvisoryStatus.ACKNOWLEDGED, acknowledged.getStatus());
        assertNotNull(acknowledged.getAcknowledgedAt());

        // Verify domain event
        assertTrue(domainEventRepository.findAll().stream()
                .anyMatch(e -> "AGRICULTURAL_ADVISORY_ACKNOWLEDGED".equals(e.getEventType())));

        // 2. Resolve advisory
        mockMvc.perform(patch("/api/v1/advisories/" + advisoryId + "/resolve")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        AgriculturalAdvisory resolved = agriAdvisoryRepository.findById(advisoryId).orElseThrow();
        assertEquals(AdvisoryStatus.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());

        // Verify domain event
        assertTrue(domainEventRepository.findAll().stream()
                .anyMatch(e -> "AGRICULTURAL_ADVISORY_RESOLVED".equals(e.getEventType())));
    }

    @Test
    @DisplayName("Auto-Resolution: Completing a linked task automatically resolves active advisory and logs domain event with reason")
    void testAutoResolutionOnTaskCompletionPublishesDomainEvent() throws Exception {
        LocalDate today = LocalDate.now();
        UUID plantingId = createPlantingWithPlan(today.minusDays(10));
        UUID taskId = createManualTask(plantingId, "Field Inundation Irrigation", TaskType.IRRIGATION, today.atTime(10, 0));

        // Evaluate -> creates active advisory
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        AgriculturalAdvisory linkedAdvBefore = agriAdvisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(
                plantingId, AdvisoryStatus.ACTIVE).stream()
                .filter(a -> a.getTask() != null && taskId.equals(a.getTask().getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected active advisory linked to task " + taskId));
        assertEquals(AdvisoryStatus.ACTIVE, linkedAdvBefore.getStatus());

        // Mark task completed
        com.krishi.backend.task.entity.Task task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus(com.krishi.backend.task.entity.TaskStatus.COMPLETED);
        taskRepository.save(task);

        // Evaluate again -> auto-resolves advisory
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        AgriculturalAdvisory linkedAdvAfter = agriAdvisoryRepository.findById(linkedAdvBefore.getId()).orElseThrow();
        assertEquals(AdvisoryStatus.RESOLVED, linkedAdvAfter.getStatus());
        assertNotNull(linkedAdvAfter.getResolvedAt());

        // Verify resolution domain event with reason
        List<DomainEvent> events = domainEventRepository.findAll();
        long resolvedEventsCount = events.stream()
                .filter(e -> "AGRICULTURAL_ADVISORY_RESOLVED".equals(e.getEventType()))
                .count();
        assertTrue(resolvedEventsCount >= 1);

        // Repeated evaluation does NOT create duplicate resolved events
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        long resolvedEventsCountAfterRepeat = domainEventRepository.findAll().stream()
                .filter(e -> "AGRICULTURAL_ADVISORY_RESOLVED".equals(e.getEventType()))
                .count();
        assertEquals(resolvedEventsCount, resolvedEventsCountAfterRepeat);
    }

    @Test
    @DisplayName("Tenant Isolation: Farmer B cannot evaluate, view, acknowledge, or resolve Farmer A's planting/advisory")
    void testFarmerTenantIsolation() throws Exception {
        LocalDate today = LocalDate.now();
        UUID plantingIdA = createPlantingWithPlan(today.minusDays(10));
        createManualTask(plantingIdA, "Irrigation Farmer A", TaskType.IRRIGATION, today.atTime(10, 0));

        // Evaluate under Farmer A
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingIdA + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        UUID advisoryIdA = agriAdvisoryRepository.findByCropPlantingIdOrderByCreatedAtDesc(plantingIdA).get(0).getId();

        // Farmer B attempts to evaluate Farmer A's planting -> 403 Forbidden
        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingIdA + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isForbidden());

        // Farmer B attempts to get Farmer A's advisories -> 403 Forbidden
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingIdA + "/advisories")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isForbidden());

        // Farmer B attempts to acknowledge Farmer A's advisory -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/advisories/" + advisoryIdA + "/acknowledge")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isForbidden());

        // Farmer B attempts to resolve Farmer A's advisory -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/advisories/" + advisoryIdA + "/resolve")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resilience: Weather provider failure yields INSUFFICIENT_CONTEXT without failing overall evaluation")
    void testWeatherFailureGracefulHandling() throws Exception {
        LocalDate today = LocalDate.now();
        UUID plantingId = createPlantingWithPlan(today.minusDays(10));

        // An overdue task (non-weather dependent)
        createManualTask(plantingId, "Overdue Weeding Task", TaskType.WEEDING, today.minusDays(3).atTime(9, 0));

        // Simulate weather provider outage
        simulateWeatherFailure = true;

        MvcResult result = mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/advisories/evaluate")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode rootJson = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(rootJson.path("success").asBoolean());
        JsonNode json = rootJson.path("data");
        assertEquals(4, json.path("totalRulesEvaluated").asInt());

        // Weather rules report INSUFFICIENT_CONTEXT
        JsonNode breakdown = json.path("ruleBreakdown");
        boolean rainRuleHasInsufficientContext = false;
        boolean overdueRuleSucceeded = false;

        for (JsonNode item : breakdown) {
            if ("IRRIGATION_RAINFALL_CONFLICT".equals(item.path("ruleCode").asText())) {
                rainRuleHasInsufficientContext = "INSUFFICIENT_CONTEXT".equals(item.path("outcome").asText());
            }
            if ("OVERDUE_CULTIVATION_TASK".equals(item.path("ruleCode").asText())) {
                overdueRuleSucceeded = "RULE_EVALUATION_SUCCESS".equals(item.path("outcome").asText());
            }
        }

        assertTrue(rainRuleHasInsufficientContext, "Weather rule should report INSUFFICIENT_CONTEXT on provider failure");
        assertTrue(overdueRuleSucceeded, "Overdue task rule should succeed independently of weather provider outage");
    }
}
