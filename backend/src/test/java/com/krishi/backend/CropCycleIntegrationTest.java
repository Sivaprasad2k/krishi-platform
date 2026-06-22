package com.krishi.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishi.backend.common.event.entity.DomainEvent;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.entity.CropCycleStatus;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class CropCycleIntegrationTest {

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
    private DomainEventRepository domainEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        domainEventRepository.deleteAll();
        cropCycleRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        domainEventRepository.deleteAll();
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
        createRequest.setSizeHectares(10.0);
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

    @Test
    void testCropCycleWorkflowAndTransitions() throws Exception {
        // 1. Setup two Farmers
        String tokenA = registerAndLogin("Farmer Alice", "9111111111", "alice@crop.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9222222222", "bob@crop.com", "Pass@123");

        // 2. Farmer A Creates a Farm
        UUID farmId = createFarm("Alice Acres", tokenA);

        // 3. Farmer A Creates a CropCycle (DRAFT)
        CropCycleCreateRequest cycleRequest = new CropCycleCreateRequest();
        cycleRequest.setName("Tomato Monsoon 2026");
        cycleRequest.setCropName("Tomato");
        cycleRequest.setFarmId(farmId);

        MvcResult cycleResult = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(cycleResult.getResponse().getContentAsString());
        UUID cycleId = UUID.fromString(responseNode.get("data").get("id").asText());
        assertNotNull(cycleId);

        // Verify state is DRAFT and start/end dates are null
        var cycleInDb = cropCycleRepository.findById(cycleId).get();
        assertEquals(CropCycleStatus.DRAFT, cycleInDb.getStatus());
        assertNull(cycleInDb.getStartDate());
        assertNull(cycleInDb.getEndDate());

        // Verify CropCycleCreatedEvent is persisted
        List<DomainEvent> events = domainEventRepository.findAll();
        DomainEvent createdEvent = events.stream()
                .filter(e -> "CROP_CYCLE_CREATED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CROP_CYCLE_CREATED event not found"));
        assertEquals(cycleId, createdEvent.getAggregateId());

        // 4. Farmer B tries to access/view Farmer A's CropCycle -> 403 Forbidden
        mockMvc.perform(get("/api/v1/crop-cycles/" + cycleId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 5. Farmer B tries to start Farmer A's CropCycle -> 403 Forbidden
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/start")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 6. Farmer A starts their CropCycle -> ACTIVE
        MvcResult startResult = mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/start")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode startResponseNode = objectMapper.readTree(startResult.getResponse().getContentAsString());
        assertEquals("ACTIVE", startResponseNode.get("data").get("status").asText());
        assertNotNull(startResponseNode.get("data").get("startDate"));

        // Verify CropCycleStartedEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "CROP_CYCLE_STARTED".equals(e.getEventType())));

        // 7. Try starting again (invalid transition ACTIVE -> ACTIVE) -> should fail (400 or 500)
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/start")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().is4xxClientError());

        // 8. Farmer A completes their CropCycle -> COMPLETED
        MvcResult completeResult = mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode completeResponse = objectMapper.readTree(completeResult.getResponse().getContentAsString());
        assertEquals("COMPLETED", completeResponse.get("data").get("status").asText());
        assertNotNull(completeResponse.get("data").get("endDate"));

        // Verify CropCycleCompletedEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "CROP_CYCLE_COMPLETED".equals(e.getEventType())));

        // 9. Try cancelling a completed crop cycle (invalid transition COMPLETED -> CANCELLED) -> fails
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().is4xxClientError());

        // 10. Create a second cycle and cancel it from ACTIVE
        CropCycleCreateRequest cycleRequest2 = new CropCycleCreateRequest();
        cycleRequest2.setName("Chilli Monsoon 2026");
        cycleRequest2.setCropName("Chilli");
        cycleRequest2.setFarmId(farmId);

        MvcResult cycleResult2 = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleRequest2)))
                .andExpect(status().isOk())
                .andReturn();

        UUID cycleId2 = UUID.fromString(objectMapper.readTree(cycleResult2.getResponse().getContentAsString()).get("data").get("id").asText());

        // Start it
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId2 + "/start")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Cancel it
        MvcResult cancelResult = mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId2 + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cancelResponse = objectMapper.readTree(cancelResult.getResponse().getContentAsString());
        assertEquals("CANCELLED", cancelResponse.get("data").get("status").asText());
        assertNotNull(cancelResponse.get("data").get("endDate"));

        // Verify CropCycleCancelledEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "CROP_CYCLE_CANCELLED".equals(e.getEventType())));

        // 11. Retrieve all crop cycles for the farm
        MvcResult listResult = mockMvc.perform(get("/api/v1/farms/" + farmId + "/crop-cycles")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listResponse = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertEquals(2, listResponse.get("data").size());
    }
}
