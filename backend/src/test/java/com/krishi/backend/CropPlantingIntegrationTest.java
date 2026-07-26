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
import com.krishi.backend.cropcycle.entity.CropCycle;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.dto.CropPlantingUpdateRequest;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import com.krishi.backend.notification.repository.NotificationRepository;
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
public class CropPlantingIntegrationTest {

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
    private DomainEventRepository domainEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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
        
        notificationRepository.deleteAll();
        domainEventRepository.deleteAll();
        cropPlantingRepository.deleteAll();
        cropCycleRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
        domainEventRepository.deleteAll();
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
        createRequest.setSizeHectares(5.5);
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

    private UUID createCropCycle(String name, UUID farmId, String token) throws Exception {
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
        return UUID.fromString(responseNode.get("data").get("id").asText());
    }

    @Test
    void testCropPlantingWorkflowAndValidationRules() throws Exception {
        // 1. Setup two Farmers
        String tokenA = registerAndLogin("Farmer Alice", "9555555551", "alice.plant@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9555555552", "bob.plant@krishi.com", "Pass@123");

        // 2. Farmer A Creates a Farm and a CropCycle (status is DRAFT by default)
        UUID farmIdStrStr = createFarm("Alice Fields", tokenA);
        UUID cycleId = createCropCycle("Summer Tomato Season", farmIdStrStr, tokenA);

        // 3. Farmer A creates a CropPlanting on the DRAFT CropCycle -> Succeeds
        CropPlantingCreateRequest createRequest = new CropPlantingCreateRequest();
        createRequest.setCropCycleId(cycleId);
        createRequest.setCropName("Tomato");
        createRequest.setCropVariety("TH-101 Hybrid");
        createRequest.setPlantingDate(LocalDateTime.now().minusDays(5));
        createRequest.setExpectedHarvestDate(LocalDateTime.now().plusDays(90));

        MvcResult plantingResult = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(plantingResult.getResponse().getContentAsString());
        UUID plantingId = UUID.fromString(responseNode.get("data").get("id").asText());
        assertNotNull(plantingId);

        // Verify CropPlantingCreatedEvent is persisted
        List<DomainEvent> events = domainEventRepository.findAll();
        DomainEvent createdEvent = events.stream()
                .filter(e -> "CROP_PLANTING_CREATED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CROP_PLANTING_CREATED event not found"));
        assertEquals(plantingId, createdEvent.getAggregateId());

        // 4. Farmer B tries to create a CropPlanting on Farmer A's CropCycle -> 403 Forbidden
        mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        // 5. Farmer B tries to view Farmer A's CropPlanting -> 403 Forbidden
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 6. Farmer B tries to update Farmer A's CropPlanting -> 403 Forbidden
        CropPlantingUpdateRequest updateRequest = new CropPlantingUpdateRequest();
        updateRequest.setCropName("Tomato Updated");
        updateRequest.setCropVariety("TH-102 Super Hybrid");
        updateRequest.setPlantingDate(LocalDateTime.now().minusDays(3));
        updateRequest.setExpectedHarvestDate(LocalDateTime.now().plusDays(95));

        mockMvc.perform(put("/api/v1/crop-plantings/" + plantingId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // 7. Farmer A updates their CropPlanting details -> Succeeds
        mockMvc.perform(put("/api/v1/crop-plantings/" + plantingId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Verify in database and verify CropPlantingUpdatedEvent is persisted
        var plantingInDb = cropPlantingRepository.findById(plantingId).get();
        assertEquals("Tomato Updated", plantingInDb.getCropName());
        assertEquals("TH-102 Super Hybrid", plantingInDb.getCropVariety());

        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "CROP_PLANTING_UPDATED".equals(e.getEventType())));

        // 8. Farmer A starts their CropCycle (moves status to ACTIVE)
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/start")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Farmer A creates a second CropPlanting on the ACTIVE CropCycle -> Succeeds
        CropPlantingCreateRequest createRequest2 = new CropPlantingCreateRequest();
        createRequest2.setCropCycleId(cycleId);
        createRequest2.setCropName("Marigold Companion");
        createRequest2.setCropVariety("Orange Boy");
        createRequest2.setPlantingDate(LocalDateTime.now());
        createRequest2.setExpectedHarvestDate(LocalDateTime.now().plusDays(60));

        mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest2)))
                .andExpect(status().isOk());

        // 9. Farmer A completes their CropCycle (moves status to COMPLETED)
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // 10. Farmer A tries to create a CropPlanting on the COMPLETED CropCycle -> fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest2)))
                .andExpect(status().isBadRequest());

        // 10b. Farmer A tries to update a CropPlanting on the COMPLETED CropCycle -> fails with 400 Bad Request
        mockMvc.perform(put("/api/v1/crop-plantings/" + plantingId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());

        // 11. Retrieve all crop plantings for the crop cycle
        MvcResult listResult = mockMvc.perform(get("/api/v1/crop-cycles/" + cycleId + "/plantings")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listResponse = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertEquals(2, listResponse.get("data").size());

        // 12. Check that notifications were generated for Crop Planting creation & update
        List<com.krishi.backend.notification.entity.Notification> notifications = notificationRepository.findAll();
        long createdNotificationsCount = notifications.stream()
                .filter(n -> n.getNotificationType().name().equals("CROP_PLANTING_CREATED"))
                .count();
        long updatedNotificationsCount = notifications.stream()
                .filter(n -> n.getNotificationType().name().equals("CROP_PLANTING_UPDATED"))
                .count();

        assertEquals(2, createdNotificationsCount, "Should have 2 CROP_PLANTING_CREATED notifications");
        assertEquals(1, updatedNotificationsCount, "Should have 1 CROP_PLANTING_UPDATED notification");

        var createdNotif = notifications.stream()
                .filter(n -> n.getNotificationType().name().equals("CROP_PLANTING_CREATED"))
                .findFirst()
                .orElseThrow();
        assertTrue(createdNotif.getMessage().contains("cropName") && createdNotif.getMessage().contains("Tomato"));
    }

    @Test
    void testInvalidDateRangeRejection() throws Exception {
        String token = registerAndLogin("Farmer DateTest", "9555555599", "datetest@krishi.com", "Pass@123");
        UUID farmId = createFarm("Date Test Farm", token);
        UUID cycleId = createCropCycle("Date Test Season", farmId, token);

        CropPlantingCreateRequest invalidRequest = new CropPlantingCreateRequest();
        invalidRequest.setCropCycleId(cycleId);
        invalidRequest.setCropName("Carrot");
        invalidRequest.setPlantingDate(LocalDateTime.now());
        invalidRequest.setExpectedHarvestDate(LocalDateTime.now().minusDays(30)); // 30 days in the PAST

        mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
