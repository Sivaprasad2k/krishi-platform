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
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentReportRequest;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.treatment.dto.TreatmentPlanRequest;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import com.krishi.backend.task.dto.TaskCreateRequest;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import com.krishi.backend.notification.entity.Notification;
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
public class NotificationIntegrationTest {

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
    private TaskRepository taskRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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
        notificationRepository.deleteAll();
        taskRepository.deleteAll();
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
        notificationRepository.deleteAll();
        taskRepository.deleteAll();
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

    private UUID createTreatment(UUID incidentId, String token) throws Exception {
        TreatmentPlanRequest planRequest = new TreatmentPlanRequest();
        planRequest.setDiseaseIncidentId(incidentId);
        planRequest.setTreatmentName("Mancozeb Spray Application");
        planRequest.setDescription("Apply Mancozeb to prevent Late Blight spread.");
        planRequest.setRecommendedDosage("2g per Liter");
        planRequest.setApplicationMethod("Foliar Spray");

        MvcResult result = mockMvc.perform(post("/api/v1/treatments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(responseNode.get("data").get("id").asText());
    }

    private UUID createTask(UUID treatmentId, String token) throws Exception {
        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setTreatmentId(treatmentId);
        taskRequest.setTitle("Spray Mancozeb");
        taskRequest.setDescription("Apply Mancozeb 2g/L to affected plants");

        MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(responseNode.get("data").get("id").asText());
    }

    @Test
    void testNotificationLifecycleAndSecurity() throws Exception {
        // 1. Setup two Farmers
        String tokenA = registerAndLogin("Farmer Alice", "9555555551", "alice.notif@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9555555552", "bob.notif@krishi.com", "Pass@123");

        // 2. Farmer A triggers events (Crop Cycle Start, Disease Incident, Task Creation)
        UUID farmId = createFarm("Alice Ranch", tokenA);
        UUID cycleId = createCropCycleAndStart("Tomato season", farmId, tokenA);
        UUID plantingId = createCropPlanting(cycleId, tokenA);
        UUID incidentId = reportDiseaseIncident(plantingId, tokenA);
        UUID treatmentId = createTreatment(incidentId, tokenA);
        UUID taskId = createTask(treatmentId, tokenA);

        // At this point, the listeners should have created:
        // - CROP_CYCLE_STARTED notification
        // - DISEASE_REPORTED notification
        // - TREATMENT_PLANNED notification
        // - TASK_CREATED notification
        // For Farmer A in the database.
        List<Notification> allNotificationsInDb = notificationRepository.findAll();
        assertFalse(allNotificationsInDb.isEmpty());

        // 3. Farmer A retrieves all notifications -> Should see notifications sorted desc
        MvcResult listResult = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listResponseNode = objectMapper.readTree(listResult.getResponse().getContentAsString());
        JsonNode data = listResponseNode.get("data");
        assertTrue(data.size() >= 4);

        UUID notificationId = UUID.fromString(data.get(0).get("id").asText());
        assertFalse(data.get(0).get("isRead").asBoolean());

        // 4. Farmer B tries to view Farmer A's notification -> 403 Forbidden
        mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 5. Farmer B tries to mark Farmer A's notification as read -> 403 Forbidden
        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 6. Farmer A views their specific notification details -> Succeeds
        MvcResult detailsResult = mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode detailsNode = objectMapper.readTree(detailsResult.getResponse().getContentAsString()).get("data");
        assertEquals(notificationId.toString(), detailsNode.get("id").asText());

        // 7. Farmer A marks single notification as read -> Succeeds
        MvcResult readResult = mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode readNode = objectMapper.readTree(readResult.getResponse().getContentAsString()).get("data");
        assertTrue(readNode.get("isRead").asBoolean());
        assertNotNull(readNode.get("readAt"));

        // 8. Farmer A queries unread notifications -> Count should have decreased by 1
        MvcResult unreadResult = mockMvc.perform(get("/api/v1/notifications/unread")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode unreadNode = objectMapper.readTree(unreadResult.getResponse().getContentAsString()).get("data");
        assertEquals(data.size() - 1, unreadNode.size());

        // 9. Farmer A marks all as read -> Succeeds
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Verify unread list is now empty
        unreadResult = mockMvc.perform(get("/api/v1/notifications/unread")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        unreadNode = objectMapper.readTree(unreadResult.getResponse().getContentAsString()).get("data");
        assertEquals(0, unreadNode.size());
    }
}
