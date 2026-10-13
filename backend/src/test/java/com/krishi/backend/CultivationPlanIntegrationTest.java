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
import com.krishi.backend.cultivation.dto.CultivationPlanGenerateRequest;
import com.krishi.backend.cultivation.entity.CultivationPlanStatus;
import com.krishi.backend.cultivation.repository.CultivationPlanRepository;
import com.krishi.backend.cultivation.repository.CultivationTemplateRepository;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import com.krishi.backend.notification.repository.NotificationRepository;
import com.krishi.backend.task.dto.TaskRescheduleRequest;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskSource;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.repository.TreatmentRepository;
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
public class CultivationPlanIntegrationTest {

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
    private CultivationTemplateRepository cultivationTemplateRepository;

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
    private static final UUID WHEAT_TEMPLATE_ID = UUID.fromString("c0000003-0000-0000-0000-000000000001");

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

        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        domainEventRepository.deleteAll();
        notificationRepository.deleteAll();
        taskRepository.deleteAll();
        cultivationPlanRepository.deleteAll();
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

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return jsonNode.get("data").get("accessToken").asText();
    }

    private UUID createFarm(String name, String token) throws Exception {
        FarmCreateRequest farmRequest = new FarmCreateRequest();
        farmRequest.setName(name);
        farmRequest.setSizeHectares(10.0);
        farmRequest.setLocation("Thanjavur, Tamil Nadu");
        farmRequest.setState("Tamil Nadu");
        farmRequest.setDistrict("Thanjavur");
        farmRequest.setLatitude(10.7870);
        farmRequest.setLongitude(79.1378);

        MvcResult result = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(responseNode.get("data").get("id").asText());
    }

    private UUID createCropCycleAndStart(String name, UUID farmId, String token) throws Exception {
        CropCycleCreateRequest cycleRequest = new CropCycleCreateRequest();
        cycleRequest.setFarmId(farmId);
        cycleRequest.setName(name);
        cycleRequest.setSeasonType(com.krishi.backend.cropcycle.entity.SeasonType.MONSOON);
        cycleRequest.setYear(2026);

        MvcResult result = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID cycleId = UUID.fromString(responseNode.get("data").get("id").asText());

        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return cycleId;
    }

    private UUID createCropPlanting(UUID cycleId, String name, LocalDateTime plantingDate, UUID templateId, String token) throws Exception {
        CropPlantingCreateRequest createRequest = new CropPlantingCreateRequest();
        createRequest.setCropCycleId(cycleId);
        createRequest.setCropName(name);
        createRequest.setCropVariety("IR20");
        createRequest.setPlantingDate(plantingDate);
        createRequest.setTemplateId(templateId);

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
    void testBrowseCropCatalogAndTemplates() throws Exception {
        String token = registerAndLogin("Farmer Ram", "9811111111", "ram.catalog@krishi.com", "Pass@123");

        // 1. Browse all published templates
        MvcResult allResult = mockMvc.perform(get("/api/v1/cultivation-templates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode allNode = objectMapper.readTree(allResult.getResponse().getContentAsString()).get("data");
        assertTrue(allNode.size() >= 3, "Expected at least 3 seeded templates (Rice, Tomato, Wheat)");

        // 2. Filter templates by cropName
        MvcResult tomatoResult = mockMvc.perform(get("/api/v1/cultivation-templates?cropName=Tomato")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tomatoNode = objectMapper.readTree(tomatoResult.getResponse().getContentAsString()).get("data");
        assertEquals(1, tomatoNode.size());
        assertEquals("Tomato", tomatoNode.get(0).get("cropName").asText());
        assertEquals(95, tomatoNode.get(0).get("durationDays").asInt());

        // 3. Get single template details
        MvcResult riceResult = mockMvc.perform(get("/api/v1/cultivation-templates/" + RICE_TEMPLATE_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode riceData = objectMapper.readTree(riceResult.getResponse().getContentAsString()).get("data");
        assertEquals("Rice (Paddy)", riceData.get("cropName").asText());
        assertEquals("2024.1", riceData.get("version").asText());
        assertTrue(riceData.get("stages").size() >= 6, "Expected at least 6 stages for Rice");
        assertTrue(riceData.get("templateTasks").size() >= 10, "Expected at least 10 activities for Rice");
    }

    @Test
    void testAutomaticPlanGenerationOnPlantingCreationAndIdempotency() throws Exception {
        String tokenA = registerAndLogin("Farmer Anitha", "9822222222", "anitha.plan@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Balu", "9822222223", "balu.other@krishi.com", "Pass@123");

        UUID farmId = createFarm("Cauvery Delta Farm", tokenA);
        UUID cycleId = createCropCycleAndStart("Samba 2026", farmId, tokenA);

        LocalDateTime plantingDate = LocalDateTime.now().minusDays(5);

        // 1. Create crop planting WITH templateId -> automatically generates cultivation plan and tasks
        UUID plantingId = createCropPlanting(cycleId, "Rice (Paddy)", plantingDate, RICE_TEMPLATE_ID, tokenA);

        // 2. Verify active cultivation plan
        MvcResult planResult = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode planData = objectMapper.readTree(planResult.getResponse().getContentAsString()).get("data");
        assertNotNull(planData);
        assertEquals("ACTIVE", planData.get("status").asText());
        assertEquals(1, planData.get("planVersion").asInt());
        assertEquals("2024.1", planData.get("templateVersion").asText());
        assertEquals(RICE_TEMPLATE_ID.toString(), planData.get("templateId").asText());

        JsonNode tasksArray = planData.get("tasks");
        assertTrue(tasksArray.size() >= 10, "Expected at least 10 generated tasks");
        assertEquals("PLAN", tasksArray.get(0).get("source").asText());
        assertEquals("PENDING", tasksArray.get(0).get("status").asText());

        // 3. IDEMPOTENCY: Retry generating plan with the same template
        CultivationPlanGenerateRequest retryRequest = new CultivationPlanGenerateRequest();
        retryRequest.setTemplateId(RICE_TEMPLATE_ID);

        MvcResult retryResult = mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(retryRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode retryData = objectMapper.readTree(retryResult.getResponse().getContentAsString()).get("data");
        assertEquals(planData.get("id").asText(), retryData.get("id").asText(), "Should return existing plan");
        assertEquals(1, retryData.get("planVersion").asInt(), "Version should not increment on duplicate retry");

        long taskCountInDb = taskRepository.findByCropPlantingId(plantingId).size();
        assertEquals(tasksArray.size(), taskCountInDb, "Duplicate tasks must NOT be created");

        // 4. IDOR Protection: Farmer B cannot generate or access Farmer A's plan
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(retryRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPlanRevisionPreservesCompletedAndInProgressTasks() throws Exception {
        String token = registerAndLogin("Farmer Rev", "9833333333", "rev@krishi.com", "Pass@123");

        UUID farmId = createFarm("Coimbatore Farm", token);
        UUID cycleId = createCropCycleAndStart("Annual 2026", farmId, token);

        LocalDateTime plantingDate = LocalDateTime.now().minusDays(10);
        UUID plantingId = createCropPlanting(cycleId, "Tomato", plantingDate, TOMATO_TEMPLATE_ID, token);

        List<Task> initialTasks = taskRepository.findByCropPlantingId(plantingId);
        assertTrue(initialTasks.size() >= 8);

        Task taskToComplete = initialTasks.get(0);
        Task taskToStart = initialTasks.get(1);
        Task taskToSkip = initialTasks.get(2);

        // 1. Complete task 0 directly from PENDING
        mockMvc.perform(post("/api/v1/tasks/" + taskToComplete.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. Start task 1 -> IN_PROGRESS
        mockMvc.perform(post("/api/v1/tasks/" + taskToStart.getId() + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Skip task 2 -> SKIPPED
        mockMvc.perform(post("/api/v1/tasks/" + taskToSkip.getId() + "/skip")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 4. Revise plan by generating a new plan (e.g. switching to Rice template or re-running with new settings)
        CultivationPlanGenerateRequest reviseRequest = new CultivationPlanGenerateRequest();
        reviseRequest.setTemplateId(RICE_TEMPLATE_ID);

        MvcResult reviseResult = mockMvc.perform(post("/api/v1/crop-plantings/" + plantingId + "/cultivation-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviseRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode revisedData = objectMapper.readTree(reviseResult.getResponse().getContentAsString()).get("data");
        assertEquals(2, revisedData.get("planVersion").asInt(), "Revised plan should be version 2");
        assertEquals("ACTIVE", revisedData.get("status").asText());

        // 5. Verify historical tasks preservation:
        // - Completed task must remain COMPLETED
        Task completedReloaded = taskRepository.findById(taskToComplete.getId()).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, completedReloaded.getStatus(), "Completed task must be preserved");

        // - In-progress task must remain IN_PROGRESS
        Task inProgressReloaded = taskRepository.findById(taskToStart.getId()).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, inProgressReloaded.getStatus(), "In-progress task must be preserved");

        // - Skipped task must remain SKIPPED
        Task skippedReloaded = taskRepository.findById(taskToSkip.getId()).orElseThrow();
        assertEquals(TaskStatus.SKIPPED, skippedReloaded.getStatus(), "Skipped task must be preserved");

        // - The previous plan must be marked SUPERSEDED
        var plans = cultivationPlanRepository.findByCropPlantingIdOrderByPlanVersionDesc(plantingId);
        assertEquals(2, plans.size());
        assertEquals(CultivationPlanStatus.ACTIVE, plans.get(0).getStatus());
        assertEquals(CultivationPlanStatus.SUPERSEDED, plans.get(1).getStatus());
    }

    @Test
    void testTaskRescheduleWorkflow() throws Exception {
        String token = registerAndLogin("Farmer Schedule", "9844444444", "sched@krishi.com", "Pass@123");

        UUID farmId = createFarm("Salem Farm", token);
        UUID cycleId = createCropCycleAndStart("Winter 2026", farmId, token);
        LocalDateTime plantingDate = LocalDateTime.now().minusDays(2);
        UUID plantingId = createCropPlanting(cycleId, "Wheat", plantingDate, WHEAT_TEMPLATE_ID, token);

        List<Task> tasks = taskRepository.findByCropPlantingId(plantingId);
        Task targetTask = tasks.get(0);

        LocalDateTime newDueDate = LocalDateTime.now().plusDays(10);
        TaskRescheduleRequest rescheduleRequest = new TaskRescheduleRequest();
        rescheduleRequest.setDueDate(newDueDate);

        MvcResult result = mockMvc.perform(put("/api/v1/tasks/" + targetTask.getId() + "/reschedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rescheduleRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertEquals(targetTask.getId().toString(), data.get("id").asText());
        assertNotNull(data.get("dueDate"));

        Task updated = taskRepository.findById(targetTask.getId()).orElseThrow();
        assertNotNull(updated.getDueDate());
    }

    @Test
    void testUnifiedWorkspaceReturnsCultivationPlan() throws Exception {
        String token = registerAndLogin("Farmer Unified", "9855555555", "unified@krishi.com", "Pass@123");

        UUID farmId = createFarm("Madurai Farm", token);
        UUID cycleId = createCropCycleAndStart("Kharif 2026", farmId, token);
        LocalDateTime plantingDate = LocalDateTime.now().minusDays(3);
        UUID plantingId = createCropPlanting(cycleId, "Rice (Paddy)", plantingDate, RICE_TEMPLATE_ID, token);

        // Call unified workspace endpoint
        MvcResult workspaceResult = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode wsData = objectMapper.readTree(workspaceResult.getResponse().getContentAsString()).get("data");
        assertNotNull(wsData.get("crop"));
        assertNotNull(wsData.get("farm"));
        assertNotNull(wsData.get("season"));
        assertTrue(wsData.get("tasks").size() >= 10);
        assertNotNull(wsData.get("cultivationPlan"), "Unified workspace must contain cultivationPlan");
        assertEquals("Rice (Paddy)", wsData.get("cultivationPlan").get("templateCropName").asText());
    }

    @Test
    void testInvalidTemplateRejection() throws Exception {
        String token = registerAndLogin("Farmer Invalid", "9866666666", "inv@krishi.com", "Pass@123");

        UUID farmId = createFarm("Tirunelveli Farm", token);
        UUID cycleId = createCropCycleAndStart("Summer 2026", farmId, token);
        LocalDateTime plantingDate = LocalDateTime.now().minusDays(1);

        // Attempt to create planting with non-existent template
        UUID nonExistentTemplateId = UUID.randomUUID();
        CropPlantingCreateRequest createRequest = new CropPlantingCreateRequest();
        createRequest.setCropCycleId(cycleId);
        createRequest.setCropName("Millets");
        createRequest.setCropVariety("Local");
        createRequest.setPlantingDate(plantingDate);
        createRequest.setTemplateId(nonExistentTemplateId);

        mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidTaskTransitionsAndRescheduleValidation() throws Exception {
        String token = registerAndLogin("Farmer Transitions", "9877777777", "trans@krishi.com", "Pass@123");

        UUID farmId = createFarm("Erode Farm", token);
        UUID cycleId = createCropCycleAndStart("Kuruvai 2026", farmId, token);
        LocalDateTime plantingDate = LocalDateTime.now().minusDays(4);
        UUID plantingId = createCropPlanting(cycleId, "Rice (Paddy)", plantingDate, RICE_TEMPLATE_ID, token);

        List<Task> tasks = taskRepository.findByCropPlantingId(plantingId);
        Task task = tasks.get(0);

        // 1. Complete the task
        mockMvc.perform(post("/api/v1/tasks/" + task.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. Attempt to start an already COMPLETED task -> must be rejected
        mockMvc.perform(post("/api/v1/tasks/" + task.getId() + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // 3. Attempt to reschedule an already COMPLETED task -> must be rejected
        TaskRescheduleRequest rescheduleRequest = new TaskRescheduleRequest();
        rescheduleRequest.setDueDate(LocalDateTime.now().plusDays(5));
        mockMvc.perform(put("/api/v1/tasks/" + task.getId() + "/reschedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rescheduleRequest)))
                .andExpect(status().isBadRequest());

        // 4. Cancel task 1 and attempt to complete it -> must be rejected
        Task task2 = tasks.get(1);
        mockMvc.perform(post("/api/v1/tasks/" + task2.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tasks/" + task2.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDatabaseEnforcesSingleActiveCultivationPlanPerPlanting() throws Exception {
        String token = registerAndLogin("Farmer Concurrency", "9888888888", "concur@krishi.com", "Pass@123");

        UUID farmId = createFarm("Tanjore Farm", token);
        UUID cycleId = createCropCycleAndStart("Thaladi 2026", farmId, token);
        LocalDateTime plantingDate = LocalDateTime.now().minusDays(2);
        UUID plantingId = createCropPlanting(cycleId, "Rice (Paddy)", plantingDate, RICE_TEMPLATE_ID, token);

        var activePlans = cultivationPlanRepository.findByCropPlantingIdOrderByPlanVersionDesc(plantingId);
        assertEquals(1, activePlans.size());
        assertEquals(CultivationPlanStatus.ACTIVE, activePlans.get(0).getStatus());

        // Attempting to manually insert a second ACTIVE plan for the same planting must violate database constraint
        var cropPlanting = cropPlantingRepository.findById(plantingId).orElseThrow();
        var template = cultivationTemplateRepository.findById(RICE_TEMPLATE_ID).orElseThrow();

        com.krishi.backend.cultivation.entity.CultivationPlan duplicateActivePlan = new com.krishi.backend.cultivation.entity.CultivationPlan();
        duplicateActivePlan.setId(UUID.randomUUID());
        duplicateActivePlan.setCropPlanting(cropPlanting);
        duplicateActivePlan.setTemplate(template);
        duplicateActivePlan.setTemplateVersion("2024.1");
        duplicateActivePlan.setPlanVersion(99);
        duplicateActivePlan.setStatus(CultivationPlanStatus.ACTIVE);
        duplicateActivePlan.setGeneratedAt(LocalDateTime.now());
        duplicateActivePlan.setCreatedAt(LocalDateTime.now());

        assertThrows(Exception.class, () -> {
            cultivationPlanRepository.saveAndFlush(duplicateActivePlan);
        }, "Database unique index must reject concurrent duplicate ACTIVE plans");
    }
}
