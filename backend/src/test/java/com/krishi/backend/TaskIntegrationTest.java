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
import com.krishi.backend.treatment.repository.TreatmentRepository;
import com.krishi.backend.task.dto.TaskCreateRequest;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.repository.TaskRepository;
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
public class TaskIntegrationTest {

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

    @Test
    void testTaskWorkflowAndValidationRules() throws Exception {
        // 1. Setup two Farmers
        String tokenA = registerAndLogin("Farmer Alice", "9888888881", "alice.task@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9888888882", "bob.task@krishi.com", "Pass@123");

        // 2. Farmer A Creates Farm, Cycle, Planting, Disease Incident, and Treatment
        UUID farmId = createFarm("Alice Ranch", tokenA);
        UUID cycleId = createCropCycleAndStart("Tomato Winter", farmId, tokenA);
        UUID plantingId = createCropPlanting(cycleId, tokenA);
        UUID incidentId = reportDiseaseIncident(plantingId, tokenA);
        UUID treatmentId = createTreatment(incidentId, tokenA);

        // 3. Farmer A creates a Task -> Succeeds (status PENDING)
        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setTreatmentId(treatmentId);
        taskRequest.setTitle("Apply Mancozeb Foliar Spray");
        taskRequest.setDescription("Mix 2g/L and spray affected leaves thoroughly.");
        taskRequest.setDueDate(LocalDateTime.now().plusDays(2));

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID taskId = UUID.fromString(responseNode.get("data").get("id").asText());
        assertNotNull(taskId);

        // Verify task state in DB is PENDING
        var taskInDb = taskRepository.findById(taskId).get();
        assertEquals(TaskStatus.PENDING, taskInDb.getStatus());
        assertNull(taskInDb.getCompletedAt());

        // Verify TaskCreatedEvent is persisted
        List<DomainEvent> events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "TASK_CREATED".equals(e.getEventType())));

        // 4. Farmer B tries to create a Task on Farmer A's Treatment -> 403 Forbidden
        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isForbidden());

        // 5. Farmer B tries to view Farmer A's Task -> 403 Forbidden
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 6. Farmer B tries to start Farmer A's Task -> 403 Forbidden
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/start")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 7. Try invalid transition (PENDING -> COMPLETED) -> fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());

        // 8. Farmer A starts their Task -> Succeeds (status IN_PROGRESS)
        MvcResult startResult = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/start")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode startResponseNode = objectMapper.readTree(startResult.getResponse().getContentAsString());
        assertEquals("IN_PROGRESS", startResponseNode.get("data").get("status").asText());

        // Verify TaskStartedEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "TASK_STARTED".equals(e.getEventType())));

        // 9. Try invalid transition (IN_PROGRESS -> CANCELLED) -> fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());

        // 10. Farmer A completes their Task -> Succeeds (status COMPLETED)
        MvcResult completeResult = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode completeResponseNode = objectMapper.readTree(completeResult.getResponse().getContentAsString());
        assertEquals("COMPLETED", completeResponseNode.get("data").get("status").asText());
        assertNotNull(completeResponseNode.get("data").get("completedAt"));

        // Verify TaskCompletedEvent is persisted
        events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "TASK_COMPLETED".equals(e.getEventType())));

        // 11. Retrieve all tasks for the treatment
        MvcResult listResult = mockMvc.perform(get("/api/v1/treatments/" + treatmentId + "/tasks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listResponseNode = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertEquals(1, listResponseNode.get("data").size());
        assertEquals("Apply Mancozeb Foliar Spray", listResponseNode.get("data").get(0).get("title").asText());
    }

    @Test
    void testTaskCancellationWorkflow() throws Exception {
        // 1. Setup Farmer
        String token = registerAndLogin("Farmer Charlie", "9888888883", "charlie.task@krishi.com", "Pass@123");

        // 2. Create Treatment
        UUID farmId = createFarm("Charlie Ranch", token);
        UUID cycleId = createCropCycleAndStart("Tomato Charlie", farmId, token);
        UUID plantingId = createCropPlanting(cycleId, token);
        UUID incidentId = reportDiseaseIncident(plantingId, token);
        UUID treatmentId = createTreatment(incidentId, token);

        // 3. Create Task
        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setTreatmentId(treatmentId);
        taskRequest.setTitle("Prune Yellow Leaves");

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID taskId = UUID.fromString(responseNode.get("data").get("id").asText());

        // 4. Cancel Task directly from PENDING -> Succeeds (status CANCELLED)
        MvcResult cancelResult = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cancelResponseNode = objectMapper.readTree(cancelResult.getResponse().getContentAsString());
        assertEquals("CANCELLED", cancelResponseNode.get("data").get("status").asText());
        assertNotNull(cancelResponseNode.get("data").get("completedAt"));

        // Verify TaskCancelledEvent is persisted
        List<DomainEvent> events = domainEventRepository.findAll();
        assertTrue(events.stream().anyMatch(e -> "TASK_CANCELLED".equals(e.getEventType())));

        // 5. Try starting a CANCELLED task -> fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
