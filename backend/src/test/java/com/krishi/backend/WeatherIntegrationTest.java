package com.krishi.backend;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
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
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskSource;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.entity.TaskType;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import com.krishi.backend.weather.dto.*;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import com.krishi.backend.weather.entity.AdvisoryStatus;
import com.krishi.backend.weather.entity.WeatherAdvisory;
import com.krishi.backend.weather.entity.WeatherForecastCache;
import com.krishi.backend.weather.provider.ProviderForecastResult;
import com.krishi.backend.weather.provider.WeatherDataProvider;
import com.krishi.backend.weather.provider.WeatherProviderException;
import com.krishi.backend.weather.repository.DistrictCoordinateRepository;
import com.krishi.backend.weather.repository.WeatherAdvisoryRepository;
import com.krishi.backend.weather.repository.WeatherForecastCacheRepository;
import com.krishi.backend.weather.scheduler.WeatherSyncScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class WeatherIntegrationTest {

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
    private WeatherAdvisoryRepository advisoryRepository;

    @Autowired
    private DistrictCoordinateRepository districtCoordinateRepository;

    @Autowired
    private WeatherSyncScheduler weatherSyncScheduler;

    @Autowired
    private TestMockWeatherProvider testMockWeatherProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TestConfiguration
    static class WeatherTestConfig {
        @Bean
        @Primary
        public TestMockWeatherProvider testMockWeatherProvider() {
            return new TestMockWeatherProvider();
        }
    }

    public static class TestMockWeatherProvider implements WeatherDataProvider {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private boolean shouldFail = false;
        private final Map<LocalDate, DailyForecastItem> dailyOverrides = new HashMap<>();

        public void reset() {
            callCount.set(0);
            shouldFail = false;
            dailyOverrides.clear();
        }

        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        public void overrideDaily(LocalDate date, DailyForecastItem item) {
            dailyOverrides.put(date, item);
        }

        public int getCallCount() {
            return callCount.get();
        }

        @Override
        public String getProviderName() {
            return "MOCK_OPEN_METEO";
        }

        @Override
        public String getAttribution() {
            return "Weather data by Open-Meteo.com (CC BY 4.0)";
        }

        @Override
        public String getAttributionUrl() {
            return "https://open-meteo.com/";
        }

        @Override
        public ProviderForecastResult fetchForecast(double latitude, double longitude) {
            callCount.incrementAndGet();

            if (shouldFail) {
                throw new WeatherProviderException("Simulated upstream provider outage");
            }

            OffsetDateTime now = OffsetDateTime.now();
            LocalDate today = LocalDate.now();

            WeatherObservation current = WeatherObservation.builder()
                    .temperatureCelsius(29.0)
                    .relativeHumidity(70)
                    .precipitationProbability(10)
                    .precipitationMm(0.0)
                    .windSpeedKmh(12.0)
                    .windGustsKmh(18.0)
                    .weatherCode(2)
                    .weatherCondition("Partly cloudy")
                    .observedAt(now)
                    .build();

            List<DailyForecastItem> daily = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate d = today.plusDays(i);
                if (dailyOverrides.containsKey(d)) {
                    daily.add(dailyOverrides.get(d));
                } else {
                    daily.add(DailyForecastItem.builder()
                            .date(d)
                            .maxTempCelsius(32.0)
                            .minTempCelsius(24.0)
                            .precipitationMm(0.0)
                            .precipitationProbabilityMax(10)
                            .maxWindSpeedKmh(14.0)
                            .maxWindGustsKmh(20.0)
                            .weatherCode(1)
                            .weatherCondition("Mainly clear")
                            .build());
                }
            }

            return ProviderForecastResult.builder()
                    .current(current)
                    .daily(daily)
                    .hourly(Collections.emptyList())
                    .rawPayloadJson("{\"mock\":true}")
                    .fetchedAt(now)
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });
        objectMapper.registerModule(module);

        testMockWeatherProvider.reset();
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        advisoryRepository.deleteAll();
        cacheRepository.deleteAll();
        domainEventRepository.deleteAll();
        notificationRepository.deleteAll();
        taskRepository.deleteAll();
        cultivationPlanRepository.deleteAll();
        treatmentRepository.deleteAll();
        diseaseIncidentRepository.deleteAll();
        cropPlantingRepository.deleteAll();
        cropCycleRepository.deleteAll();
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

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return jsonNode.get("data").get("accessToken").asText();
    }

    private UUID createFarm(String name, String state, String district, Double lat, Double lng, String token) throws Exception {
        FarmCreateRequest farmRequest = new FarmCreateRequest();
        farmRequest.setName(name);
        farmRequest.setSizeHectares(5.0);
        farmRequest.setLocation((district != null ? district : "Location") + ", " + (state != null ? state : "State"));
        farmRequest.setState(state);
        farmRequest.setDistrict(district);
        farmRequest.setLatitude(lat);
        farmRequest.setLongitude(lng);

        MvcResult result = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(jsonNode.get("data").get("id").asText());
    }

    private UUID createCropCycle(UUID farmId, String token) throws Exception {
        CropCycleCreateRequest cycleRequest = new CropCycleCreateRequest();
        cycleRequest.setFarmId(farmId);
        cycleRequest.setName("Kharif Paddy 2026");
        cycleRequest.setSeasonType(com.krishi.backend.cropcycle.entity.SeasonType.MONSOON);
        cycleRequest.setYear(2026);

        MvcResult result = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID cycleId = UUID.fromString(node.get("data").get("id").asText());

        // Start crop cycle to make it ACTIVE
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return cycleId;
    }

    private UUID createPlanting(UUID cycleId, String cropName, String token) throws Exception {
        CropPlantingCreateRequest plantingRequest = new CropPlantingCreateRequest();
        plantingRequest.setCropCycleId(cycleId);
        plantingRequest.setCropName(cropName);
        plantingRequest.setCropVariety("IR20");
        plantingRequest.setPlantingDate(LocalDateTime.now().minusDays(5));
        plantingRequest.setExpectedHarvestDate(LocalDateTime.now().plusDays(115));

        MvcResult result = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plantingRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(node.get("data").get("id").asText());
    }

    @Test
    void testForecastWithDirectFarmGps() throws Exception {
        String token = registerAndLogin("Siva Prasad", "9847000001", "siva1@krishi.test", "Password@123");
        UUID farmId = createFarm("Green Palms Farm", "Kerala", "Palakkad", 10.7867, 76.6548, token);

        MvcResult result = mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.coordinateSource").value("FARM_GPS"))
                .andExpect(jsonPath("$.data.latitude").value(10.7867))
                .andExpect(jsonPath("$.data.longitude").value(76.6548))
                .andExpect(jsonPath("$.data.stale").value(false))
                .andExpect(jsonPath("$.data.attribution").isNotEmpty())
                .andExpect(jsonPath("$.data.daily.length()").value(7))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertEquals("FARM_GPS", data.get("coordinateSource").asText());

        // Verify spatial grid cache persisted with normalized coordinates (10.79, 76.65)
        List<WeatherForecastCache> caches = cacheRepository.findAll();
        assertEquals(1, caches.size());
        assertEquals("OPEN_METEO:10.79:76.65", caches.get(0).getGridKey());
        assertEquals(10.79, caches.get(0).getNormalizedLatitude(), 0.001);
        assertEquals(76.65, caches.get(0).getNormalizedLongitude(), 0.001);
    }

    @Test
    void testDistrictCentroidFallback() throws Exception {
        String token = registerAndLogin("Ramesh Kumar", "9847000002", "ramesh@krishi.test", "Password@123");
        // Coordinates omitted, only district and state specified
        UUID farmId = createFarm("Palakkad Paddy Fields", "Kerala", "Palakkad", null, null, token);

        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.coordinateSource").value("DISTRICT_CENTROID"))
                .andExpect(jsonPath("$.data.locationLabel").value("Palakkad, Kerala"))
                .andExpect(jsonPath("$.data.latitude").value(10.7867))
                .andExpect(jsonPath("$.data.longitude").value(76.6548))
                .andExpect(jsonPath("$.data.stale").value(false));

        assertEquals(1, testMockWeatherProvider.getCallCount());
    }

    @Test
    void testLocationUnavailableForUnknownDistrict() throws Exception {
        String token = registerAndLogin("Anil Kumar", "9847000003", "anil@krishi.test", "Password@123");
        // Unknown district that cannot be resolved
        UUID farmId = createFarm("Remote Island Farm", "UnknownState", "NonExistentDistrict", null, null, token);

        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("LOCATION_UNAVAILABLE"));

        assertEquals(0, testMockWeatherProvider.getCallCount());
    }

    @Test
    void testSpatialGridCacheSharingAcrossNeighboringFarms() throws Exception {
        String tokenA = registerAndLogin("Farmer Alpha", "9847000004", "alpha@krishi.test", "Password@123");
        String tokenB = registerAndLogin("Farmer Beta", "9847000005", "beta@krishi.test", "Password@123");

        // Two farms ~400 meters apart in the same village (both round to 10.79 : 76.65)
        UUID farmA = createFarm("Alpha Farm", "Kerala", "Palakkad", 10.7867, 76.6548, tokenA);
        UUID farmB = createFarm("Beta Farm", "Kerala", "Palakkad", 10.7890, 76.6520, tokenB);

        // Fetch weather for Farm A
        mockMvc.perform(get("/api/v1/farms/" + farmA + "/weather")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        assertEquals(1, testMockWeatherProvider.getCallCount());

        // Fetch weather for Farm B (Neighboring farm)
        mockMvc.perform(get("/api/v1/farms/" + farmB + "/weather")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Crucial verification: Provider must NOT be called again because cache was reused!
        assertEquals(1, testMockWeatherProvider.getCallCount());
    }

    @Test
    void testFarmOwnershipAuthorizationIdorProtection() throws Exception {
        String tokenA = registerAndLogin("Farmer Alpha", "9847000006", "alpha2@krishi.test", "Password@123");
        String tokenB = registerAndLogin("Farmer Beta", "9847000007", "beta2@krishi.test", "Password@123");

        UUID farmA = createFarm("Alpha Farm", "Kerala", "Palakkad", 10.7867, 76.6548, tokenA);

        // Farmer B attempts to query Farmer A's farm weather
        mockMvc.perform(get("/api/v1/farms/" + farmA + "/weather")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void testHeavyRainAdvisoryGenerationAndDeduplication() throws Exception {
        String token = registerAndLogin("Farmer Rain", "9847000008", "rain@krishi.test", "Password@123");
        UUID farmId = createFarm("Rainy Farm", "Kerala", "Palakkad", 10.7867, 76.6548, token);
        UUID cycleId = createCropCycle(farmId, token);
        UUID plantingId = createPlanting(cycleId, "Rice (Paddy)", token);

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Create an IRRIGATION task for tomorrow
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setCropPlanting(cropPlantingRepository.findById(plantingId).orElseThrow());
        task.setTitle("Panicle Stage Irrigation");
        task.setTaskType(TaskType.IRRIGATION);
        task.setSource(TaskSource.PLAN);
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(tomorrow.atTime(8, 0));
        task.setCreatedAt(LocalDateTime.now());
        taskRepository.save(task);

        // Configure weather forecast: 35 mm rain tomorrow
        testMockWeatherProvider.overrideDaily(tomorrow, DailyForecastItem.builder()
                .date(tomorrow)
                .maxTempCelsius(28.0)
                .minTempCelsius(22.0)
                .precipitationMm(35.0)
                .precipitationProbabilityMax(85)
                .maxWindSpeedKmh(12.0)
                .maxWindGustsKmh(18.0)
                .weatherCode(65)
                .weatherCondition("Heavy rain")
                .build());

        // Fetch weather
        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify active advisory created
        List<WeatherAdvisory> advisories = advisoryRepository.findByFarmIdAndStatus(farmId, AdvisoryStatus.ACTIVE);
        assertEquals(1, advisories.size());
        WeatherAdvisory advisory = advisories.get(0);
        assertEquals("HEAVY_RAIN_IRRIGATION", advisory.getRuleCode());
        assertEquals(AdvisorySeverity.MEDIUM, advisory.getSeverity());
        assertEquals(task.getId(), advisory.getTask().getId());
        assertEquals(tomorrow, advisory.getTargetDate());

        // Verify task was NOT modified (Farmer operational control preserved!)
        Task reloadedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(TaskStatus.PENDING, reloadedTask.getStatus());
        assertEquals(tomorrow.atTime(8, 0), reloadedTask.getDueDate());

        // Verify in-database notification dispatched
        List<Notification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());
        assertTrue(notifications.stream().anyMatch(n -> n.getTitle().contains("Heavy Rain")));

        int initialNotificationCount = notifications.size();

        // Second weather fetch: Condition is the same
        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify ZERO duplicate advisories and ZERO duplicate notifications!
        List<WeatherAdvisory> secondAdvisories = advisoryRepository.findByFarmIdAndStatus(farmId, AdvisoryStatus.ACTIVE);
        assertEquals(1, secondAdvisories.size());
        assertEquals(initialNotificationCount, notificationRepository.findAll().size());
    }

    @Test
    void testAdvisorySeverityEscalation() throws Exception {
        String token = registerAndLogin("Farmer Escalate", "9847000009", "escalate@krishi.test", "Password@123");
        UUID farmId = createFarm("Escalation Farm", "Kerala", "Palakkad", 10.7867, 76.6548, token);
        UUID cycleId = createCropCycle(farmId, token);
        UUID plantingId = createPlanting(cycleId, "Rice (Paddy)", token);

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setCropPlanting(cropPlantingRepository.findById(plantingId).orElseThrow());
        task.setTitle("Panicle Stage Irrigation");
        task.setTaskType(TaskType.IRRIGATION);
        task.setSource(TaskSource.PLAN);
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(tomorrow.atTime(8, 0));
        task.setCreatedAt(LocalDateTime.now());
        taskRepository.save(task);

        // Step 1: Moderate rain (25mm) -> MEDIUM severity
        testMockWeatherProvider.overrideDaily(tomorrow, DailyForecastItem.builder()
                .date(tomorrow)
                .maxTempCelsius(28.0)
                .minTempCelsius(22.0)
                .precipitationMm(25.0)
                .precipitationProbabilityMax(80)
                .maxWindSpeedKmh(12.0)
                .maxWindGustsKmh(18.0)
                .weatherCode(63)
                .weatherCondition("Moderate rain")
                .build());

        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        WeatherAdvisory initial = advisoryRepository.findByFarmIdAndStatus(farmId, AdvisoryStatus.ACTIVE).get(0);
        assertEquals(AdvisorySeverity.MEDIUM, initial.getSeverity());

        // Step 2: Severe rain escalation (60mm) -> HIGH severity
        testMockWeatherProvider.overrideDaily(tomorrow, DailyForecastItem.builder()
                .date(tomorrow)
                .maxTempCelsius(26.0)
                .minTempCelsius(21.0)
                .precipitationMm(60.0)
                .precipitationProbabilityMax(95)
                .maxWindSpeedKmh(15.0)
                .maxWindGustsKmh(22.0)
                .weatherCode(65)
                .weatherCondition("Torrential rain")
                .build());

        // Age the cache past the 15-minute cooldown so refresh queries the updated forecast
        WeatherForecastCache cache = cacheRepository.findAll().get(0);
        cache.setFetchedAt(OffsetDateTime.now().minusMinutes(20));
        cacheRepository.save(cache);

        // Force cache refresh
        mockMvc.perform(post("/api/v1/farms/" + farmId + "/weather/refresh")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify old advisory was SUPERSEDED and new advisory is HIGH
        WeatherAdvisory oldAdvisory = advisoryRepository.findById(initial.getId()).orElseThrow();
        assertEquals(AdvisoryStatus.SUPERSEDED, oldAdvisory.getStatus());

        List<WeatherAdvisory> activeAdvisories = advisoryRepository.findByFarmIdAndStatus(farmId, AdvisoryStatus.ACTIVE);
        assertEquals(1, activeAdvisories.size());
        assertEquals(AdvisorySeverity.HIGH, activeAdvisories.get(0).getSeverity());

        // Verify escalation notification was sent
        assertTrue(notificationRepository.findAll().stream().anyMatch(n -> n.getTitle().contains("[ESCALATION]")));
    }

    @Test
    void testAdvisoryAcknowledgment() throws Exception {
        String token = registerAndLogin("Farmer Ack", "9847000010", "ack@krishi.test", "Password@123");
        UUID farmId = createFarm("Ack Farm", "Kerala", "Palakkad", 10.7867, 76.6548, token);
        UUID cycleId = createCropCycle(farmId, token);
        UUID plantingId = createPlanting(cycleId, "Tomato", token);

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setCropPlanting(cropPlantingRepository.findById(plantingId).orElseThrow());
        task.setTitle("Fungicide Spraying");
        task.setTaskType(TaskType.PEST_CONTROL);
        task.setSource(TaskSource.PLAN);
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(tomorrow.atTime(9, 0));
        task.setCreatedAt(LocalDateTime.now());
        taskRepository.save(task);

        // Wind 25 km/h -> HIGH_WIND_SPRAYING advisory
        testMockWeatherProvider.overrideDaily(tomorrow, DailyForecastItem.builder()
                .date(tomorrow)
                .maxTempCelsius(30.0)
                .minTempCelsius(24.0)
                .precipitationMm(0.0)
                .precipitationProbabilityMax(0)
                .maxWindSpeedKmh(25.0)
                .maxWindGustsKmh(35.0)
                .weatherCode(1)
                .weatherCondition("Windy")
                .build());

        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        WeatherAdvisory active = advisoryRepository.findByFarmIdAndStatus(farmId, AdvisoryStatus.ACTIVE).get(0);

        // Farmer acknowledges advisory
        mockMvc.perform(post("/api/v1/advisories/" + active.getId() + "/acknowledge")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));

        WeatherAdvisory acknowledged = advisoryRepository.findById(active.getId()).orElseThrow();
        assertEquals(AdvisoryStatus.ACKNOWLEDGED, acknowledged.getStatus());
        assertNotNull(acknowledged.getAcknowledgedAt());

        // Re-evaluating does NOT recreate active advisory since it was acknowledged
        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertEquals(0, advisoryRepository.findByFarmIdAndStatus(farmId, AdvisoryStatus.ACTIVE).size());
    }

    @Test
    void testAdvisoryLinkageToTask() throws Exception {
        String token = registerAndLogin("Farmer Link", "9847000011", "link@krishi.test", "Password@123");
        UUID farmId = createFarm("Link Farm", "Kerala", "Palakkad", 10.7867, 76.6548, token);
        UUID cycleId = createCropCycle(farmId, token);
        UUID plantingId = createPlanting(cycleId, "Rice (Paddy)", token);

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setCropPlanting(cropPlantingRepository.findById(plantingId).orElseThrow());
        task.setTitle("Panicle Irrigation");
        task.setTaskType(TaskType.IRRIGATION);
        task.setSource(TaskSource.PLAN);
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(tomorrow.atTime(8, 0));
        task.setCreatedAt(LocalDateTime.now());
        taskRepository.save(task);

        testMockWeatherProvider.overrideDaily(tomorrow, DailyForecastItem.builder()
                .date(tomorrow)
                .maxTempCelsius(28.0)
                .minTempCelsius(22.0)
                .precipitationMm(30.0)
                .precipitationProbabilityMax(80)
                .maxWindSpeedKmh(10.0)
                .maxWindGustsKmh(15.0)
                .weatherCode(63)
                .weatherCondition("Moderate rain")
                .build());

        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Query task advisories endpoint
        mockMvc.perform(get("/api/v1/tasks/" + task.getId() + "/advisories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].taskId").value(task.getId().toString()))
                .andExpect(jsonPath("$.data[0].ruleCode").value("HEAVY_RAIN_IRRIGATION"));
    }

    @Test
    void testStaleCacheFallbackOnProviderFailure() throws Exception {
        String token = registerAndLogin("Farmer Stale", "9847000012", "stale@krishi.test", "Password@123");
        UUID farmId = createFarm("Stale Farm", "Kerala", "Palakkad", 10.7867, 76.6548, token);

        // Successful first call establishes cache
        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stale").value(false));

        // Manually expire the cache in DB
        WeatherForecastCache cache = cacheRepository.findAll().get(0);
        cache.setExpiresAt(OffsetDateTime.now().minusMinutes(5));
        cacheRepository.save(cache);

        // Simulate upstream weather provider outage
        testMockWeatherProvider.setShouldFail(true);

        // Subsequent call must fall back to existing cache with stale: true indicator
        mockMvc.perform(get("/api/v1/farms/" + farmId + "/weather")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stale").value(true));
    }

    @Test
    void testPostgreSqlAdvisoryLockingInScheduler() {
        assertDoesNotThrow(() -> weatherSyncScheduler.syncScheduledWeatherForecasts());
    }
}
