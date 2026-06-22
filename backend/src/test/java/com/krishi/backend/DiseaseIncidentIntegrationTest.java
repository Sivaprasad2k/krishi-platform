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
import com.krishi.backend.diseaseincident.entity.DiseaseIncidentStatus;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
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
public class DiseaseIncidentIntegrationTest {

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
        diseaseIncidentRepository.deleteAll();
        cropPlantingRepository.deleteAll();
        cropCycleRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        domainEventRepository.deleteAll();
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
        createRequest.setSizeHectares(8.0);
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
        cycleRequest.setCropName("Tomato");
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
        createRequest.setPlantingDate(LocalDateTime.now().minusDays(10));
        createRequest.setExpectedHarvestDate(LocalDateTime.now().plusDays(80));

        MvcResult result = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(responseNode.get("data").get("id").asText());
    }

    @Test
    void testDiseaseIncidentWorkflowAndValidationRules() throws Exception {
        // 1. Setup two Farmers
        String tokenA = registerAndLogin("Farmer Alice", "9666666661", "alice.disease@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9666666662", "bob.disease@krishi.com", "Pass@123");

        // 2. Farmer A Creates Farm, Cycle, and Planting
        UUID farmId = createFarm("Punjab Fields", tokenA);
        UUID cycleId = createCropCycleAndStart("Tomato Monsoon", farmId, tokenA);
        UUID plantingId = createCropPlanting(cycleId, tokenA);

        // 3. Farmer A reports a DiseaseIncident -> Succeeds
        DiseaseIncidentReportRequest reportRequest = new DiseaseIncidentReportRequest();
        reportRequest.setCropPlantingId(plantingId);
        reportRequest.setDiseaseName("Late Blight");
        reportRequest.setDescription("Black spots appearing on leaves after heavy rain.");

        MvcResult reportResult = mockMvc.perform(post("/api/v1/disease-incidents")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(reportResult.getResponse().getContentAsString());
        UUID incidentId = UUID.fromString(responseNode.get("data").get("id").asText());
        assertNotNull(incidentId);

        // Verify state is REPORTED in DB
        var incidentInDb = diseaseIncidentRepository.findById(incidentId).get();
        assertEquals(DiseaseIncidentStatus.REPORTED, incidentInDb.getStatus());
        assertNotNull(incidentInDb.getReportedAt());

        // Verify DiseaseIncidentReportedEvent is persisted
        List<DomainEvent> events = domainEventRepository.findAll();
        DomainEvent reportedEvent = events.stream()
                .filter(e -> "DISEASE_INCIDENT_REPORTED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DISEASE_INCIDENT_REPORTED event not found"));
        assertEquals(incidentId, reportedEvent.getAggregateId());

        // 4. Farmer B tries to report a DiseaseIncident on Farmer A's CropPlanting -> 403 Forbidden
        mockMvc.perform(post("/api/v1/disease-incidents")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isForbidden());

        // 5. Farmer B tries to view Farmer A's DiseaseIncident -> 403 Forbidden
        mockMvc.perform(get("/api/v1/disease-incidents/" + incidentId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 6. Farmer B tries to review Farmer A's DiseaseIncident -> 403 Forbidden
        mockMvc.perform(post("/api/v1/disease-incidents/" + incidentId + "/review")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 7. Farmer A moves the DiseaseIncident to UNDER_REVIEW -> Succeeds
        MvcResult reviewResult = mockMvc.perform(post("/api/v1/disease-incidents/" + incidentId + "/review")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode reviewResponseNode = objectMapper.readTree(reviewResult.getResponse().getContentAsString());
        assertEquals("UNDER_REVIEW", reviewResponseNode.get("data").get("status").asText());
        assertNotNull(reviewResponseNode.get("data").get("reviewedAt"));

        // Verify DiseaseIncidentUnderReviewEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "DISEASE_INCIDENT_UNDER_REVIEW".equals(e.getEventType())));

        // 8. Try invalid transition (UNDER_REVIEW -> CLOSED directly without TREATED) -> fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/disease-incidents/" + incidentId + "/close")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());

        // 9. Farmer A moves the DiseaseIncident to TREATED -> Succeeds
        MvcResult treatResult = mockMvc.perform(post("/api/v1/disease-incidents/" + incidentId + "/treat")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode treatResponseNode = objectMapper.readTree(treatResult.getResponse().getContentAsString());
        assertEquals("TREATED", treatResponseNode.get("data").get("status").asText());
        assertNotNull(treatResponseNode.get("data").get("treatedAt"));

        // Verify DiseaseIncidentTreatedEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "DISEASE_INCIDENT_TREATED".equals(e.getEventType())));

        // 10. Farmer A closes the DiseaseIncident -> Succeeds
        MvcResult closeResult = mockMvc.perform(post("/api/v1/disease-incidents/" + incidentId + "/close")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode closeResponseNode = objectMapper.readTree(closeResult.getResponse().getContentAsString());
        assertEquals("CLOSED", closeResponseNode.get("data").get("status").asText());
        assertNotNull(closeResponseNode.get("data").get("closedAt"));

        // Verify DiseaseIncidentClosedEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "DISEASE_INCIDENT_CLOSED".equals(e.getEventType())));

        // 11. Retrieve all disease incidents for the crop planting
        MvcResult listResult = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/disease-incidents")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listResponseNode = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertEquals(1, listResponseNode.get("data").size());
        assertEquals("Late Blight", listResponseNode.get("data").get(0).get("diseaseName").asText());
    }
}
