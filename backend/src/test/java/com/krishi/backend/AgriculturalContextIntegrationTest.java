package com.krishi.backend;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.entity.SeasonType;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cultivation.dto.CultivationPlanGenerateRequest;
import com.krishi.backend.cultivation.repository.CultivationPlanRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import com.krishi.backend.notification.repository.NotificationRepository;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.weather.dto.DailyForecastItem;
import com.krishi.backend.weather.dto.WeatherObservation;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AgriculturalContextIntegrationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TestConfiguration
    static class ContextWeatherTestConfig {
        @Bean
        @Primary
        public WeatherDataProvider mockContextWeatherProvider() {
            return new WeatherDataProvider() {
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
                    WeatherObservation obs = WeatherObservation.builder()
                            .temperatureCelsius(29.0)
                            .relativeHumidity(80)
                            .precipitationMm(2.5)
                            .windSpeedKmh(12.0)
                            .weatherCode(61)
                            .weatherCondition("Slight rain")
                            .observedAt(OffsetDateTime.now())
                            .build();

                    DailyForecastItem dailyItem = DailyForecastItem.builder()
                            .date(LocalDate.now())
                            .weatherCode(61)
                            .weatherCondition("Slight rain")
                            .maxTempCelsius(31.0)
                            .minTempCelsius(24.0)
                            .precipitationMm(5.0)
                            .precipitationProbabilityMax(70)
                            .maxWindSpeedKmh(15.0)
                            .build();

                    return ProviderForecastResult.builder()
                            .current(obs)
                            .daily(List.of(dailyItem))
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

        farmerToken = registerAndLogin("Farmer Context A", "9847111001", "context_a@krishi.com", "Password123!");
        farmerBToken = registerAndLogin("Farmer Context B", "9847111002", "context_b@krishi.com", "Password123!");

        // Create Farm for Farmer A
        FarmCreateRequest farmRequest = new FarmCreateRequest();
        farmRequest.setName("Ernakulam Agro Farm");
        farmRequest.setSizeHectares(3.5);
        farmRequest.setLocation("Aluva, Ernakulam");
        farmRequest.setState("Kerala");
        farmRequest.setDistrict("Ernakulam");
        farmRequest.setLatitude(10.1076);
        farmRequest.setLongitude(76.3516);

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
        cycleRequest.setName("Kharif 2026");
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
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        notificationRepository.deleteAll();
        domainEventRepository.deleteAll();
        taskRepository.deleteAll();
        treatmentRepository.deleteAll();
        diseaseIncidentRepository.deleteAll();
        cultivationPlanRepository.deleteAll();
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

        String responseContent = loginResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseContent);
        return root.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("1. End-to-end Agricultural Context resolution with active plan, weather, and tasks")
    public void testEndToEndAgriculturalContextResolution() throws Exception {
        // Create planting: planting date = 2026-06-01
        CropPlantingCreateRequest plantingReq = new CropPlantingCreateRequest();
        plantingReq.setCropCycleId(cropCycleId);
        plantingReq.setCropName("Rice (Paddy)");
        plantingReq.setCropVariety("Jyothi (PTB 39)");
        plantingReq.setPlantingDate(LocalDateTime.of(2026, 6, 1, 0, 0));

        MvcResult plantingResult = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plantingReq)))
                .andExpect(status().isOk())
                .andReturn();

        UUID plantingId = UUID.fromString(objectMapper.readTree(plantingResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Generate Cultivation Plan
        CultivationPlanGenerateRequest planReq = new CultivationPlanGenerateRequest();
        planReq.setTemplateId(RICE_TEMPLATE_ID);

        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isOk());

        // Query Agricultural Context as of 2026-06-16 (Day 15: Stage 2 - Transplanting & Establishment [0, 20])
        MvcResult debugRes = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/context")
                        .header("Authorization", "Bearer " + farmerToken)
                        .param("asOfDate", "2026-06-16"))
                .andReturn();
        System.out.println("DEBUG_CONTEXT_STATUS: " + debugRes.getResponse().getStatus());
        System.out.println("DEBUG_CONTEXT_BODY: " + debugRes.getResponse().getContentAsString());
        if (debugRes.getResolvedException() != null) {
            debugRes.getResolvedException().printStackTrace();
        }
        assertEquals(200, debugRes.getResponse().getStatus());
        JsonNode data = objectMapper.readTree(debugRes.getResponse().getContentAsString()).get("data");
        assertEquals("2026-06-16", data.get("asOfDate").asText());
        assertEquals("Rice (Paddy)", data.get("crop").get("cropName").asText());
        assertEquals("Jyothi (PTB 39)", data.get("crop").get("cropVariety").asText());
        assertEquals(15, data.get("crop").get("elapsedDays").asLong());
        assertEquals("RESOLVED", data.get("growthStage").get("estimatedStage").get("status").asText());
        assertEquals("Transplanting & Establishment", data.get("growthStage").get("estimatedStage").get("stageName").asText());
        assertEquals(2, data.get("growthStage").get("estimatedStage").get("stageOrder").asInt());
        assertTrue(data.get("growthStage").get("observedStage").isNull());
        assertEquals("CALENDAR_DAY_MODEL_ESTIMATE", data.get("growthStage").get("agronomicBasis").asText());
        assertEquals("Ernakulam Agro Farm", data.get("location").get("farmName").asText());
        assertEquals("Ernakulam", data.get("location").get("district").asText());
        assertEquals("FARM_GPS", data.get("location").get("coordinateSource").asText());
        assertEquals("ACTIVE", data.get("activePlan").get("status").asText());
        assertEquals(1, data.get("activePlan").get("revisionNumber").asInt());
        assertEquals(6, data.get("activePlan").get("totalStages").asInt());
        assertTrue(data.get("relevantTasks").isArray());
        assertEquals("FRESH", data.get("weather").get("freshness").asText());
        assertEquals(29.0, data.get("weather").get("current").get("temperatureCelsius").asDouble());
    }

    @Test
    @DisplayName("2. Agricultural Context for planting without active plan returns NO_ACTIVE_PLAN")
    public void testAgriculturalContextWithoutActivePlan() throws Exception {
        CropPlantingCreateRequest plantingReq = new CropPlantingCreateRequest();
        plantingReq.setCropCycleId(cropCycleId);
        plantingReq.setCropName("Tomato");
        plantingReq.setCropVariety("Roma VF");
        plantingReq.setPlantingDate(LocalDateTime.of(2026, 6, 1, 0, 0));

        MvcResult plantingResult = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plantingReq)))
                .andExpect(status().isOk())
                .andReturn();

        UUID plantingId = UUID.fromString(objectMapper.readTree(plantingResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/context")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.growthStage.estimatedStage.status").value("NO_ACTIVE_PLAN"))
                .andExpect(jsonPath("$.data.growthStage.estimatedStage.stageName").isEmpty())
                .andExpect(jsonPath("$.data.activePlan").isEmpty())
                .andExpect(jsonPath("$.data.weather.freshness").value("FRESH"));
    }

    @Test
    @DisplayName("3. Multiple plantings in same crop cycle resolve independent stages in context")
    public void testMultiplePlantingsResolveIndependentStages() throws Exception {
        // Planting 1: planted on 2026-06-01
        CropPlantingCreateRequest req1 = new CropPlantingCreateRequest();
        req1.setCropCycleId(cropCycleId);
        req1.setCropName("Rice (Paddy)");
        req1.setCropVariety("Jyothi");
        req1.setPlantingDate(LocalDateTime.of(2026, 6, 1, 0, 0));

        MvcResult res1 = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andReturn();
        UUID planting1Id = UUID.fromString(objectMapper.readTree(res1.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Planting 2: planted on 2026-07-01 (1 month later)
        CropPlantingCreateRequest req2 = new CropPlantingCreateRequest();
        req2.setCropCycleId(cropCycleId);
        req2.setCropName("Rice (Paddy)");
        req2.setCropVariety("Jyothi");
        req2.setPlantingDate(LocalDateTime.of(2026, 7, 1, 0, 0));

        MvcResult res2 = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andReturn();
        UUID planting2Id = UUID.fromString(objectMapper.readTree(res2.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Generate plans for both
        CultivationPlanGenerateRequest plan1 = new CultivationPlanGenerateRequest();
        plan1.setTemplateId(RICE_TEMPLATE_ID);
        mockMvc.perform(post("/api/v1/crop-plantings/" + planting1Id + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan1)))
                .andExpect(status().isOk());

        CultivationPlanGenerateRequest plan2 = new CultivationPlanGenerateRequest();
        plan2.setTemplateId(RICE_TEMPLATE_ID);
        mockMvc.perform(post("/api/v1/crop-plantings/" + planting2Id + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan2)))
                .andExpect(status().isOk());

        // Evaluate both on 2026-07-15
        // Planting 1: Day 44 -> Stage 3 (Active Tillering & Panicle Initiation [21, 55])
        mockMvc.perform(get("/api/v1/crop-plantings/" + planting1Id + "/context")
                        .header("Authorization", "Bearer " + farmerToken)
                        .param("asOfDate", "2026-07-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.crop.elapsedDays").value(44))
                .andExpect(jsonPath("$.data.growthStage.estimatedStage.stageOrder").value(3))
                .andExpect(jsonPath("$.data.growthStage.estimatedStage.stageName").value("Tillering & Vegetative Phase"));

        // Planting 2: Day 14 -> Stage 2 (Transplanting & Establishment [0, 20])
        mockMvc.perform(get("/api/v1/crop-plantings/" + planting2Id + "/context")
                        .header("Authorization", "Bearer " + farmerToken)
                        .param("asOfDate", "2026-07-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.crop.elapsedDays").value(14))
                .andExpect(jsonPath("$.data.growthStage.estimatedStage.stageOrder").value(2))
                .andExpect(jsonPath("$.data.growthStage.estimatedStage.stageName").value("Transplanting & Establishment"));
    }

    @Test
    @DisplayName("4. Tenant isolation: Farmer B querying Farmer A's planting receives 403 Forbidden")
    public void testTenantIsolationDeniesAccess() throws Exception {
        CropPlantingCreateRequest plantingReq = new CropPlantingCreateRequest();
        plantingReq.setCropCycleId(cropCycleId);
        plantingReq.setCropName("Rice (Paddy)");
        plantingReq.setCropVariety("IR20");
        plantingReq.setPlantingDate(LocalDateTime.of(2026, 6, 1, 0, 0));

        MvcResult plantingResult = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plantingReq)))
                .andExpect(status().isOk())
                .andReturn();

        UUID plantingId = UUID.fromString(objectMapper.readTree(plantingResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Farmer B requests Farmer A's planting context
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/context")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Existing crop workspace endpoint is enriched with weather while preserving backward compatibility")
    public void testCropWorkspaceEnrichedWithWeather() throws Exception {
        CropPlantingCreateRequest plantingReq = new CropPlantingCreateRequest();
        plantingReq.setCropCycleId(cropCycleId);
        plantingReq.setCropName("Rice (Paddy)");
        plantingReq.setCropVariety("IR20");
        plantingReq.setPlantingDate(LocalDateTime.of(2026, 6, 1, 0, 0));

        MvcResult plantingResult = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plantingReq)))
                .andExpect(status().isOk())
                .andReturn();

        UUID plantingId = UUID.fromString(objectMapper.readTree(plantingResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Query existing workspace endpoint
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/workspace")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // Verify existing fields remain intact
                .andExpect(jsonPath("$.data.crop").isNotEmpty())
                .andExpect(jsonPath("$.data.season").isNotEmpty())
                .andExpect(jsonPath("$.data.farm").isNotEmpty())
                .andExpect(jsonPath("$.data.farmName").value("Ernakulam Agro Farm"))
                // Verify weather enrichment is present
                .andExpect(jsonPath("$.data.weather.freshness").value("FRESH"))
                .andExpect(jsonPath("$.data.weather.current.temperatureCelsius").value(29.0));
    }
}
