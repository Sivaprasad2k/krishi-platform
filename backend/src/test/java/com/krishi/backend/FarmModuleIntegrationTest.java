package com.krishi.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishi.backend.common.event.entity.DomainEvent;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.dto.FarmUpdateRequest;
import com.krishi.backend.farm.entity.FarmStatus;
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
public class FarmModuleIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private DomainEventRepository domainEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        domainEventRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        domainEventRepository.deleteAll();
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

    @Test
    void testFarmLifecycleAndOwnershipEnforcement() throws Exception {
        // 1. Register and Login Farmer A and Farmer B
        String tokenA = registerAndLogin("Farmer Alice", "9000000001", "alice@farm.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9000000002", "bob@farm.com", "Pass@123");

        // Assert 0 initial farms
        assertEquals(0, farmRepository.count());

        // 2. Farmer A Creates a Farm
        FarmCreateRequest createRequest = new FarmCreateRequest();
        createRequest.setName("Alice Valley");
        createRequest.setSizeHectares(12.5);
        createRequest.setLocation("Punjab, IN");

        MvcResult createResult = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Parse farm ID from the response
        String createResponseStr = createResult.getResponse().getContentAsString();
        JsonNode createResponseNode = objectMapper.readTree(createResponseStr);
        String farmIdStr = createResponseNode.get("data").get("id").asText();
        UUID farmId = UUID.fromString(farmIdStr);
        assertNotNull(farmId);

        // Verify farm is saved in the database
        assertEquals(1, farmRepository.count());
        assertTrue(farmRepository.findById(farmId).isPresent());

        // 3. Verify FarmCreatedEvent Persistence
        List<DomainEvent> domainEvents = domainEventRepository.findAll();
        // There should be events for User registered (A & B) and Farm created
        DomainEvent farmCreatedEvent = domainEvents.stream()
                .filter(e -> "FARM_CREATED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FARM_CREATED event was not found in domain_events"));

        assertEquals("FARM", farmCreatedEvent.getAggregateType());
        assertEquals(farmId, farmCreatedEvent.getAggregateId());
        
        JsonNode eventDataNode = objectMapper.readTree(farmCreatedEvent.getEventData());
        assertEquals("Alice Valley", eventDataNode.get("name").asText());
        assertEquals(12.5, eventDataNode.get("sizeHectares").asDouble());
        assertEquals("Punjab, IN", eventDataNode.get("location").asText());

        // 4. Get Farm Details - Authorized (Farmer A accesses own farm)
        mockMvc.perform(get("/api/v1/farms/" + farmId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // 5. Get Farm Details - Unauthorized (Farmer B tries to access Farmer A's farm)
        mockMvc.perform(get("/api/v1/farms/" + farmId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 6. Update Farm - Unauthorized (Farmer B tries to update Farmer A's farm)
        FarmUpdateRequest updateRequest = new FarmUpdateRequest();
        updateRequest.setName("Bob Valley");
        updateRequest.setSizeHectares(15.0);
        updateRequest.setLocation(" Punjab, IN");
        updateRequest.setStatus(FarmStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/farms/" + farmId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // 7. Update Farm - Authorized (Farmer A updates own farm)
        updateRequest.setName("Alice Valley Updated");
        updateRequest.setSizeHectares(14.5);
        updateRequest.setLocation("Haryana, IN");
        updateRequest.setStatus(FarmStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/farms/" + farmId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Verify updates in database
        var updatedFarm = farmRepository.findById(farmId).get();
        assertEquals("Alice Valley Updated", updatedFarm.getName());
        assertEquals(14.5, updatedFarm.getSizeHectares());
        assertEquals("Haryana, IN", updatedFarm.getLocation());

        // 8. List Farms - Farmer A lists own farms
        MvcResult listResult = mockMvc.perform(get("/api/v1/farms")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listResponseNode = objectMapper.readTree(listResult.getResponse().getContentAsString());
        JsonNode dataArray = listResponseNode.get("data");
        assertEquals(1, dataArray.size());
        assertEquals("Alice Valley Updated", dataArray.get(0).get("name").asText());
    }
}
