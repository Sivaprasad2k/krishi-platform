package com.krishi.backend;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.krishi.backend.common.event.entity.DomainEvent;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentReportRequest;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.treatment.dto.TreatmentPlanRequest;
import com.krishi.backend.treatment.entity.TreatmentStatus;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class TreatmentIntegrationTest {

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
    private DiseaseIncidentRepository diseaseIncidentRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private DomainEventRepository domainEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });
        objectMapper.registerModule(module);

        domainEventRepository.deleteAll();
        treatmentRepository.deleteAll();
        diseaseIncidentRepository.deleteAll();
        cropPlantingRepository.deleteAll();
        cropCycleRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        domainEventRepository.deleteAll();
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
        registerRequest.setPassword(password);
        registerRequest.setEmail(email);

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

        String responseString = loginResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseString, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        return (String) data.get("accessToken");
    }

    private UUID createFarm(String name, String token) throws Exception {
        FarmCreateRequest createRequest = new FarmCreateRequest();
        createRequest.setName(name);
        createRequest.setSizeHectares(6.5);
        createRequest.setLocation("Haryana, IN");

        MvcResult createResult = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseStr = createResult.getResponse().getContentAsString();
        JsonNode createResponseNode = objectMapper.readTree(createResponseStr);
        return UUID.fromString(createResponseNode.get("data").get("id").asText());
    }

    private UUID createCropCycleAndStart(String name, UUID farmId, String token) throws Exception {
        CropCycleCreateRequest cycleRequest = new CropCycleCreateRequest();
        cycleRequest.setName(name);
        cycleRequest.setSeasonType(com.krishi.backend.cropcycle.entity.SeasonType.MONSOON);
        cycleRequest.setYear(2026);
        cycleRequest.setFarmId(farmId);

        MvcResult cycleResult = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(cycleResult.getResponse().getContentAsString());
        UUID cycleId = UUID.fromString(responseNode.get("data").get("id").asText());

        // Start it
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return cycleId;
    }

    private UUID createCropPlanting(UUID cycleId, String token) throws Exception {
        CropPlantingCreateRequest createRequest = new CropPlantingCreateRequest();
        createRequest.setCropCycleId(cycleId);
        createRequest.setCropName("Tomato");
        createRequest.setCropVariety("TH-101 Hybrid");
        createRequest.setPlantingDate(LocalDateTime.now().minusDays(15));
        createRequest.setExpectedHarvestDate(LocalDateTime.now().plusDays(75));

        MvcResult result = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(responseNode.get("data").get("id").asText());
    }

    private UUID reportDiseaseIncident(UUID plantingId, String token) throws Exception {
        DiseaseIncidentReportRequest reportRequest = new DiseaseIncidentReportRequest();
        reportRequest.setCropPlantingId(plantingId);
        reportRequest.setDiseaseName("Late Blight");
        reportRequest.setDescription("Spots on leaves.");

        MvcResult result = mockMvc.perform(post("/api/v1/disease-incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(responseNode.get("data").get("id").asText());
    }

    @Test
    void testTreatmentWorkflowAndValidationRules() throws Exception {
        // 1. Setup two Farmers
        String tokenA = registerAndLogin("Farmer Alice", "9777777771", "alice.treatment@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9777777772", "bob.treatment@krishi.com", "Pass@123");

        // 2. Farmer A Creates Farm, Cycle, Planting, and Disease Incident
        UUID farmId = createFarm("Alice Ranch", tokenA);
        UUID cycleId = createCropCycleAndStart("Tomato Winter", farmId, tokenA);
        UUID plantingId = createCropPlanting(cycleId, tokenA);
        UUID incidentId = reportDiseaseIncident(plantingId, tokenA);

        // 3. Farmer A creates a Treatment plan -> Succeeds (status PLANNED)
        TreatmentPlanRequest planRequest = new TreatmentPlanRequest();
        planRequest.setDiseaseIncidentId(incidentId);
        planRequest.setTreatmentName("Mancozeb Spray Application");
        planRequest.setDescription("Apply Mancozeb to prevent Late Blight spread.");
        planRequest.setRecommendedDosage("2g per Liter");
        planRequest.setApplicationMethod("Foliar Spray");

        MvcResult createResult = mockMvc.perform(post("/api/v1/treatments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID treatmentId = UUID.fromString(responseNode.get("data").get("id").asText());
        assertNotNull(treatmentId);

        // Verify state in DB is PLANNED
        var treatmentInDb = treatmentRepository.findById(treatmentId).get();
        assertEquals(TreatmentStatus.PLANNED, treatmentInDb.getStatus());
        assertNull(treatmentInDb.getStartDate());
        assertNull(treatmentInDb.getCompletionDate());

        // Verify TreatmentPlannedEvent is persisted
        List<DomainEvent> events = domainEventRepository.findAll();
        DomainEvent plannedEvent = events.stream()
                .filter(e -> "TREATMENT_PLANNED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("TREATMENT_PLANNED event not found"));
        assertEquals(treatmentId, plannedEvent.getAggregateId());

        // 4. Farmer B tries to create a Treatment on Farmer A's DiseaseIncident -> 403 Forbidden
        mockMvc.perform(post("/api/v1/treatments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isForbidden());

        // 5. Farmer B tries to view Farmer A's Treatment -> 403 Forbidden
        mockMvc.perform(get("/api/v1/treatments/" + treatmentId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 6. Farmer B tries to start Farmer A's Treatment -> 403 Forbidden
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/start")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 7. Farmer A starts their Treatment -> Succeeds (status IN_PROGRESS)
        MvcResult startResult = mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/start")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode startResponseNode = objectMapper.readTree(startResult.getResponse().getContentAsString());
        assertEquals("IN_PROGRESS", startResponseNode.get("data").get("status").asText());
        assertNotNull(startResponseNode.get("data").get("startDate"));

        // Verify TreatmentStartedEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "TREATMENT_STARTED".equals(e.getEventType())));

        // 8. Try invalid transition (IN_PROGRESS -> PLANNED) -> fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/start")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());

        // 9. Farmer A completes their Treatment -> Succeeds (status COMPLETED)
        MvcResult completeResult = mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode completeResponseNode = objectMapper.readTree(completeResult.getResponse().getContentAsString());
        assertEquals("COMPLETED", completeResponseNode.get("data").get("status").asText());
        assertNotNull(completeResponseNode.get("data").get("completionDate"));

        // Verify TreatmentCompletedEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "TREATMENT_COMPLETED".equals(e.getEventType())));

        // 10. Try invalid transition (COMPLETED -> IN_PROGRESS) -> fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());

        // 11. Retrieve all treatments for the disease incident
        MvcResult listResult = mockMvc.perform(get("/api/v1/disease-incidents/" + incidentId + "/treatments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listResponseNode = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertEquals(1, listResponseNode.get("data").size());
        assertEquals("Mancozeb Spray Application", listResponseNode.get("data").get(0).get("treatmentName").asText());
    }

    @Test
    void testGetAllTreatmentsConsolidatedEndpoint() throws Exception {
        String tokenA = registerAndLogin("Farmer Alpha", "9998887771", "alpha.treat@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Beta", "9998887772", "beta.treat@krishi.com", "Pass@123");

        UUID farmId = createFarm("Green Valley Farm", tokenA);
        UUID cycleId = createCropCycleAndStart("Summer 2026", farmId, tokenA);
        UUID plantingId = createCropPlanting(cycleId, tokenA);
        UUID incidentId = reportDiseaseIncident(plantingId, tokenA);

        TreatmentPlanRequest planRequest = new TreatmentPlanRequest();
        planRequest.setDiseaseIncidentId(incidentId);
        planRequest.setTreatmentName("Copper Oxychloride Spray");
        planRequest.setDescription("Apply 3g/L spray in early morning");
        planRequest.setRecommendedDosage("3g per Liter");
        planRequest.setApplicationMethod("Foliar Spray");

        MvcResult createRes = mockMvc.perform(post("/api/v1/treatments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isOk())
                .andReturn();
        UUID treatmentId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 1. Farmer A calls GET /api/v1/treatments -> returns 1 enriched treatment
        MvcResult getResultA = mockMvc.perform(get("/api/v1/treatments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arrayA = objectMapper.readTree(getResultA.getResponse().getContentAsString()).get("data");
        assertEquals(1, arrayA.size());
        JsonNode itemA = arrayA.get(0);
        assertEquals(treatmentId.toString(), itemA.get("id").asText());
        assertEquals("Copper Oxychloride Spray", itemA.get("treatmentName").asText());
        assertEquals("Late Blight", itemA.get("diseaseName").asText());
        assertEquals("Tomato", itemA.get("cropName").asText());
        assertEquals("Green Valley Farm", itemA.get("farmName").asText());
        assertEquals(farmId.toString(), itemA.get("farmId").asText());

        // 2. Farmer B calls GET /api/v1/treatments -> returns empty list (farmer isolation)
        MvcResult getResultB = mockMvc.perform(get("/api/v1/treatments")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arrayB = objectMapper.readTree(getResultB.getResponse().getContentAsString()).get("data");
        assertEquals(0, arrayB.size());
    }
}
