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
import com.krishi.backend.task.entity.TaskSource;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.entity.TaskType;
import com.krishi.backend.task.repository.TaskRepository;
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

        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return cycleId;
    }

    private UUID createCropPlanting(UUID cycleId, String name, String token) throws Exception {
        CropPlantingCreateRequest createRequest = new CropPlantingCreateRequest();
        createRequest.setCropCycleId(cycleId);
        createRequest.setCropName(name);
        createRequest.setCropVariety("Hybrid");
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
    void testGeneralOperationalTaskWorkflow() throws Exception {
        String tokenA = registerAndLogin("Farmer Alice", "9888888881", "alice.gen@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9888888882", "bob.gen@krishi.com", "Pass@123");

        UUID farmId = createFarm("Alice Fields", tokenA);
        UUID cycleId = createCropCycleAndStart("Summer Season", farmId, tokenA);
        UUID plantingId = createCropPlanting(cycleId, "Banana", tokenA);

        // 1. Create General Irrigation Task (no treatment)
        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setCropPlantingId(plantingId);
        taskRequest.setTaskType(TaskType.IRRIGATION);
        taskRequest.setTitle("Water Banana Crop");
        taskRequest.setDescription("Provide 50L irrigation per plant");
        taskRequest.setDueDate(LocalDateTime.now().plusDays(1));

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        UUID taskId = UUID.fromString(createNode.get("id").asText());
        assertEquals("MANUAL", createNode.get("source").asText());
        assertEquals("IRRIGATION", createNode.get("taskType").asText());

        // 2. Reject Farmer B accessing Farmer A's task -> 403
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 3. Start task -> IN_PROGRESS
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/start")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // 4. Complete task -> COMPLETED
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // 5. Verify task listed via /api/v1/tasks endpoint for authenticated farmer
        MvcResult allTasksResult = mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode allTasksNode = objectMapper.readTree(allTasksResult.getResponse().getContentAsString()).get("data");
        assertEquals(1, allTasksNode.size());
        assertEquals("Water Banana Crop", allTasksNode.get(0).get("title").asText());

        // Verify dashboard pending task count (completed task is not pending)
        MvcResult dashResult = mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dashNode = objectMapper.readTree(dashResult.getResponse().getContentAsString()).get("data");
        assertEquals(0, dashNode.get("pendingTasks").asLong());
    }

    @Test
    void testCrossEntityMismatchRejection() throws Exception {
        String token = registerAndLogin("Farmer Charlie", "9888888883", "charlie.mismatch@krishi.com", "Pass@123");

        UUID farmId = createFarm("Charlie Farm", token);
        UUID cycleId = createCropCycleAndStart("Kharif 2026", farmId, token);
        UUID planting1 = createCropPlanting(cycleId, "Rice", token);
        UUID planting2 = createCropPlanting(cycleId, "Corn", token);

        UUID incident1 = reportDiseaseIncident(planting1, token);
        UUID treatment1 = createTreatment(incident1, token);

        // Attempt to create a task targeting Planting2 with Treatment from Planting1 -> Bad Request mismatch
        TaskCreateRequest mismatchRequest = new TaskCreateRequest();
        mismatchRequest.setCropPlantingId(planting2);
        mismatchRequest.setTreatmentId(treatment1);
        mismatchRequest.setTaskType(TaskType.DISEASE_TREATMENT);
        mismatchRequest.setTitle("Mismatched Treatment Task");

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testTreatmentLinkedTaskWorkflow() throws Exception {
        String token = registerAndLogin("Farmer Dave", "9888888884", "dave.treat@krishi.com", "Pass@123");

        UUID farmId = createFarm("Dave Farm", token);
        UUID cycleId = createCropCycleAndStart("Monsoon 2026", farmId, token);
        UUID plantingId = createCropPlanting(cycleId, "Tomato", token);
        UUID incidentId = reportDiseaseIncident(plantingId, token);
        UUID treatmentId = createTreatment(incidentId, token);

        // Create Task linked to both planting and treatment
        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setCropPlantingId(plantingId);
        taskRequest.setTreatmentId(treatmentId);
        taskRequest.setTaskType(TaskType.DISEASE_TREATMENT);
        taskRequest.setTitle("Apply Fungicide Spray");

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        assertEquals(treatmentId.toString(), createNode.get("treatmentId").asText());
        assertEquals(plantingId.toString(), createNode.get("cropPlantingId").asText());

        // Verify retrieval via treatment tasks endpoint
        MvcResult treatTasksResult = mockMvc.perform(get("/api/v1/treatments/" + treatmentId + "/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode treatTasksNode = objectMapper.readTree(treatTasksResult.getResponse().getContentAsString()).get("data");
        assertEquals(1, treatTasksNode.size());
    }

    @Test
    void testSkipTaskWithReasonAndEnrichedHierarchy() throws Exception {
        String token = registerAndLogin("Farmer Eva", "9888888885", "eva.skip@krishi.com", "Pass@123");

        UUID farmId = createFarm("Sunrise Farm", token);
        UUID cycleId = createCropCycleAndStart("Spring 2026", farmId, token);
        UUID plantingId = createCropPlanting(cycleId, "Wheat", token);

        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setCropPlantingId(plantingId);
        taskRequest.setTaskType(TaskType.IRRIGATION);
        taskRequest.setTitle("Field Flood Irrigation");

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        UUID taskId = UUID.fromString(createNode.get("id").asText());
        assertEquals("Sunrise Farm", createNode.get("farmName").asText());
        assertEquals("Wheat", createNode.get("cropName").asText());
        assertEquals(farmId.toString(), createNode.get("farmId").asText());

        // Skip with validated reason
        String skipJson = "{\"reason\":\"Adverse Weather / Heavy Rain\"}";
        MvcResult skipResult = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/skip")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skipJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode skipNode = objectMapper.readTree(skipResult.getResponse().getContentAsString()).get("data");
        assertEquals("SKIPPED", skipNode.get("status").asText());
        assertEquals("Adverse Weather / Heavy Rain", skipNode.get("skipReason").asText());

        // Verify retrieval via GET /api/v1/tasks persists skipReason
        MvcResult listResult = mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        // Verify backward compatibility: skipping without body succeeds with null skipReason
        TaskCreateRequest taskRequest2 = new TaskCreateRequest();
        taskRequest2.setCropPlantingId(plantingId);
        taskRequest2.setTaskType(TaskType.WEEDING);
        taskRequest2.setTitle("Manual Weeding");

        MvcResult createResult2 = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest2)))
                .andExpect(status().isOk())
                .andReturn();

        UUID taskId2 = UUID.fromString(objectMapper.readTree(createResult2.getResponse().getContentAsString()).get("data").get("id").asText());

        MvcResult skipResult2 = mockMvc.perform(post("/api/v1/tasks/" + taskId2 + "/skip")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode skipNode2 = objectMapper.readTree(skipResult2.getResponse().getContentAsString()).get("data");
        assertEquals("SKIPPED", skipNode2.get("status").asText());
        assertTrue(skipNode2.get("skipReason") == null || skipNode2.get("skipReason").isNull());
    }

    @Test
    void testOverdueTaskAllowedTransitions() throws Exception {
        String token = registerAndLogin("Farmer Frank", "9888888886", "frank.overdue@krishi.com", "Pass@123");

        UUID farmId = createFarm("Valley View Farm", token);
        UUID cycleId = createCropCycleAndStart("Autumn 2026", farmId, token);
        UUID plantingId = createCropPlanting(cycleId, "Corn", token);

        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setCropPlantingId(plantingId);
        taskRequest.setTaskType(TaskType.FERTILIZER_APPLICATION);
        taskRequest.setTitle("Urea Top Dressing");

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asText());

        // Simulate OVERDUE status in repository
        com.krishi.backend.task.entity.Task task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus(TaskStatus.OVERDUE);
        taskRepository.save(task);

        // 1. OVERDUE -> START (Permitted)
        MvcResult startResult = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode startNode = objectMapper.readTree(startResult.getResponse().getContentAsString()).get("data");
        assertEquals("IN_PROGRESS", startNode.get("status").asText());

        // Set back to OVERDUE to test SKIP with reason
        task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus(TaskStatus.OVERDUE);
        taskRepository.save(task);

        // 2. OVERDUE -> SKIP with reason (Permitted)
        MvcResult skipResult = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/skip")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Soil Moisture Adequate\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode skipNode = objectMapper.readTree(skipResult.getResponse().getContentAsString()).get("data");
        assertEquals("SKIPPED", skipNode.get("status").asText());
        assertEquals("Soil Moisture Adequate", skipNode.get("skipReason").asText());
    }
}
