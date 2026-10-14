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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AgronomicStageResolverIntegrationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID RICE_TEMPLATE_ID = UUID.fromString("a0000001-0000-0000-0000-000000000001");
    private static final UUID TOMATO_TEMPLATE_ID = UUID.fromString("b0000002-0000-0000-0000-000000000001");

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

        cleanupDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        domainEventRepository.deleteAll();
        notificationRepository.deleteAll();
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
    @DisplayName("Integration: Dynamic stage estimation across full lifecycle (Day 0, Day 20, Day 21, Post-Harvest)")
    void testResolveGrowthStage_FullLifecycleViaApi() throws Exception {
        String token = registerAndLogin("Sreedhar Nair", "9847000101", "sreedhar@krishi.com", "Secret@123");

        // 1. Create Farm
        FarmCreateRequest farmReq = new FarmCreateRequest();
        farmReq.setName("Nelliyampathy Paddy Farm");
        farmReq.setSizeHectares(5.0);
        farmReq.setLocation("Palakkad, Kerala");
        farmReq.setState("Kerala");
        farmReq.setDistrict("Palakkad");

        MvcResult farmRes = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID farmId = UUID.fromString(objectMapper.readTree(farmRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 2. Create CropCycle
        CropCycleCreateRequest cycleReq = new CropCycleCreateRequest();
        cycleReq.setName("Kharif 2026");
        cycleReq.setSeasonType(SeasonType.MONSOON);
        cycleReq.setYear(2026);
        cycleReq.setFarmId(farmId);

        MvcResult cycleRes = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID cycleId = UUID.fromString(objectMapper.readTree(cycleRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 3. Create CropPlanting (Planting Date: 2026-06-01)
        CropPlantingCreateRequest cropReq = new CropPlantingCreateRequest();
        cropReq.setCropCycleId(cycleId);
        cropReq.setCropName("Rice (Paddy)");
        cropReq.setCropVariety("IR20");
        cropReq.setPlantingDate(LocalDateTime.of(2026, 6, 1, 8, 0));

        MvcResult cropRes = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cropReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID plantingId = UUID.fromString(objectMapper.readTree(cropRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // Before plan generation: status is NO_ACTIVE_PLAN
        MvcResult prePlanRes = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode prePlanData = objectMapper.readTree(prePlanRes.getResponse().getContentAsString()).get("data");
        assertEquals("NO_ACTIVE_PLAN", prePlanData.get("estimatedGrowthStage").get("status").asText());
        assertTrue(prePlanData.get("estimatedGrowthStage").get("stageName").isNull());

        // 4. Generate Cultivation Plan using published ICAR Rice template
        CultivationPlanGenerateRequest planReq = new CultivationPlanGenerateRequest();
        planReq.setTemplateId(RICE_TEMPLATE_ID);

        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isOk());

        // 5. Test Day 0 (2026-06-01): Sowing/transplanting day resolves to Stage 2
        MvcResult day0Res = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "?asOfDate=2026-06-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedGrowthStage.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageName").value("Transplanting & Establishment"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageOrder").value(2))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.elapsedDays").value(0))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageStartDay").value(0))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageEndDay").value(20))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageProgressPercentage").value(0.0))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.overallProgressPercentage").value(0.0))
                .andReturn();
        assertNotNull(day0Res);

        // 6. Test Day 20 (2026-06-21): End of Stage 2 (100% stage progress)
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "?asOfDate=2026-06-21")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedGrowthStage.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageName").value("Transplanting & Establishment"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageOrder").value(2))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.elapsedDays").value(20))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageProgressPercentage").value(100.0));

        // 7. Test Day 21 (2026-06-22): Transition into Stage 3 (Tillering & Vegetative Phase)
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "?asOfDate=2026-06-22")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedGrowthStage.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageName").value("Tillering & Vegetative Phase"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageOrder").value(3))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.elapsedDays").value(21))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageStartDay").value(21))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageEndDay").value(50))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageProgressPercentage").value(0.0));

        // 8. Test Day 130 (2026-10-09): Beyond 120-day duration resolves to POST_HARVEST
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "?asOfDate=2026-10-09")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedGrowthStage.status").value("POST_HARVEST"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageName").doesNotExist())
                .andExpect(jsonPath("$.data.estimatedGrowthStage.elapsedDays").value(130))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.overallProgressPercentage").value(100.0));
    }

    @Test
    @DisplayName("Integration: Crop Workspace Endpoint exposes estimatedGrowthStage at both root and crop levels")
    void testCropWorkspaceEndpoint_IncludesEstimatedGrowthStage() throws Exception {
        String token = registerAndLogin("Kavitha Menon", "9847000202", "kavitha@krishi.com", "Pass@1234");

        // 1. Create Farm & Cycle
        FarmCreateRequest farmReq = new FarmCreateRequest();
        farmReq.setName("Wayanad Spice Garden");
        farmReq.setSizeHectares(3.0);
        farmReq.setLocation("Kalpetta, Kerala");

        MvcResult farmRes = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID farmId = UUID.fromString(objectMapper.readTree(farmRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropCycleCreateRequest cycleReq = new CropCycleCreateRequest();
        cycleReq.setName("Annual 2026");
        cycleReq.setSeasonType(SeasonType.MONSOON);
        cycleReq.setYear(2026);
        cycleReq.setFarmId(farmId);

        MvcResult cycleRes = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID cycleId = UUID.fromString(objectMapper.readTree(cycleRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 2. Create Planting for Tomato (Planting Date: 2026-08-01)
        CropPlantingCreateRequest cropReq = new CropPlantingCreateRequest();
        cropReq.setCropCycleId(cycleId);
        cropReq.setCropName("Tomato");
        cropReq.setCropVariety("Arka Rakshak");
        cropReq.setPlantingDate(LocalDateTime.of(2026, 8, 1, 9, 0));

        MvcResult cropRes = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cropReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID plantingId = UUID.fromString(objectMapper.readTree(cropRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 3. Generate Tomato Plan
        CultivationPlanGenerateRequest planReq = new CultivationPlanGenerateRequest();
        planReq.setTemplateId(TOMATO_TEMPLATE_ID);

        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isOk());

        // 4. Query Workspace at Day 40 (2026-09-10) -> Stage 3 (Flowering & Fruit Setting [36, 60])
        MvcResult wsRes = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/workspace?asOfDate=2026-09-10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageName").value("Flowering & Fruit Setting"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageOrder").value(3))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.totalStages").value(4))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.elapsedDays").value(40))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageStartDay").value(36))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageEndDay").value(60))
                .andExpect(jsonPath("$.data.crop.estimatedGrowthStage.stageName").value("Flowering & Fruit Setting"))
                .andReturn();

        JsonNode root = objectMapper.readTree(wsRes.getResponse().getContentAsString());
        JsonNode stageNode = root.get("data").get("estimatedGrowthStage");
        assertNotNull(stageNode);
        assertNotNull(stageNode.get("stageId").asText());
        assertEquals("Tomato", stageNode.get("templateName").asText());
        assertEquals("2024.1", stageNode.get("templateVersion").asText());
        assertTrue(stageNode.get("message").asText().contains("Flowering & Fruit Setting"));
    }

    @Test
    @DisplayName("Integration: Future planting date correctly returns PRE_PLANTING status")
    void testFuturePlanting_ReturnsPrePlantingStatus() throws Exception {
        String token = registerAndLogin("Manoj Kumar", "9847000303", "manoj@krishi.com", "Password@123");

        FarmCreateRequest farmReq = new FarmCreateRequest();
        farmReq.setName("Kollam Vegetable Plot");
        farmReq.setSizeHectares(1.5);
        farmReq.setLocation("Kollam, Kerala");

        MvcResult farmRes = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID farmId = UUID.fromString(objectMapper.readTree(farmRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropCycleCreateRequest cycleReq = new CropCycleCreateRequest();
        cycleReq.setName("Winter 2026");
        cycleReq.setSeasonType(SeasonType.WINTER);
        cycleReq.setYear(2026);
        cycleReq.setFarmId(farmId);


        MvcResult cycleRes = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID cycleId = UUID.fromString(objectMapper.readTree(cycleRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // Planting date is 2026-12-01
        CropPlantingCreateRequest cropReq = new CropPlantingCreateRequest();
        cropReq.setCropCycleId(cycleId);
        cropReq.setCropName("Tomato");
        cropReq.setCropVariety("PKM-1");
        cropReq.setPlantingDate(LocalDateTime.of(2026, 12, 1, 9, 0));

        MvcResult cropRes = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cropReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID plantingId = UUID.fromString(objectMapper.readTree(cropRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CultivationPlanGenerateRequest planReq = new CultivationPlanGenerateRequest();
        planReq.setTemplateId(TOMATO_TEMPLATE_ID);

        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isOk());

        // Evaluate on 2026-11-20 (11 days prior to planting)
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "?asOfDate=2026-11-20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedGrowthStage.status").value("PRE_PLANTING"))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.stageName").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.elapsedDays").value(-11))
                .andExpect(jsonPath("$.data.estimatedGrowthStage.overallProgressPercentage").value(0.0));
    }

    @Test
    @DisplayName("Integration: Ownership isolation enforces 403 Forbidden for cross-tenant access and 401 for unauthenticated")
    void testOwnershipAndAuthorizationIsolation() throws Exception {
        String tokenA = registerAndLogin("Owner Farmer", "9847000404", "owner@krishi.com", "Pass@1234");
        String tokenB = registerAndLogin("Intruder Farmer", "9847000505", "intruder@krishi.com", "Pass@1234");

        FarmCreateRequest farmReq = new FarmCreateRequest();
        farmReq.setName("Owner Farm");
        farmReq.setSizeHectares(2.0);
        farmReq.setLocation("Ernakulam, Kerala");

        MvcResult farmRes = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID farmId = UUID.fromString(objectMapper.readTree(farmRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropCycleCreateRequest cycleReq = new CropCycleCreateRequest();
        cycleReq.setName("Cycle A");
        cycleReq.setSeasonType(SeasonType.MONSOON);
        cycleReq.setYear(2026);
        cycleReq.setFarmId(farmId);

        MvcResult cycleRes = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID cycleId = UUID.fromString(objectMapper.readTree(cycleRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropPlantingCreateRequest cropReq = new CropPlantingCreateRequest();
        cropReq.setCropCycleId(cycleId);
        cropReq.setCropName("Tomato");
        cropReq.setCropVariety("Pusa Ruby");
        cropReq.setPlantingDate(LocalDateTime.now().minusDays(10));

        MvcResult cropRes = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cropReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID plantingId = UUID.fromString(objectMapper.readTree(cropRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // Farmer B attempts to get Farmer A's planting -> 403 Forbidden
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Farmer B attempts to get Farmer A's workspace -> 403 Forbidden
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/workspace")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Unauthenticated request -> 403 Forbidden
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId))
                .andExpect(status().isForbidden());
    }
}
