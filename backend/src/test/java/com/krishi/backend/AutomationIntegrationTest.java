package com.krishi.backend;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.krishi.backend.automation.entity.AutomationRule;
import com.krishi.backend.automation.repository.AutomationRuleRepository;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentReportRequest;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.treatment.dto.TreatmentPlanRequest;
import com.krishi.backend.treatment.repository.TreatmentRepository;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskStatus;
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
public class AutomationIntegrationTest {

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
    private AutomationRuleRepository automationRuleRepository;

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

        // Ensure rules are enabled before each test run
        List<AutomationRule> rules = automationRuleRepository.findAll();
        for (AutomationRule rule : rules) {
            rule.setEnabled(true);
        }
        automationRuleRepository.saveAll(rules);
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

    @Test
    void testAutomationRulesEngineFlow() throws Exception {
        // 1. Setup Farmer
        String token = registerAndLogin("Farmer Dan", "9444444441", "dan.auto@krishi.com", "Pass@123");

        // 2. Trigger Rule 1 (DISEASE_REPORTED -> CREATE_NOTIFICATION)
        UUID farmId = createFarm("Dan Farms", token);
        UUID cycleId = createCropCycleAndStart("Tomato season", farmId, token);
        UUID plantingId = createCropPlanting(cycleId, token);
        
        // Reporting disease publishes DiseaseIncidentReportedEvent
        UUID incidentId = reportDiseaseIncident(plantingId, token);

        // Verify that Rule 1 created a notification
        List<Notification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());
        assertTrue(notifications.stream()
                .anyMatch(n -> n.getMessage().contains("Late Blight reported for Tomato")));

        // 3. Trigger Rule 2 (TREATMENT_COMPLETED -> CREATE_TASK)
        UUID treatmentId = createTreatment(incidentId, token);
        
        // Start treatment
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Complete treatment publishes TreatmentCompletedEvent
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify Rule 2 automatically created the follow-up task "Inspect Crop Recovery"
        List<Task> tasks = taskRepository.findByTreatmentId(treatmentId);
        assertFalse(tasks.isEmpty());
        
        Task followUpTask = tasks.stream()
                .filter(t -> "Inspect Crop Recovery".equals(t.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Follow-up task not found"));

        assertEquals(TaskStatus.PENDING, followUpTask.getStatus());

        // 4. Trigger Rule 3 (TASK_COMPLETED -> CREATE_NOTIFICATION)
        // Start the automatically created follow-up task
        mockMvc.perform(post("/api/v1/tasks/" + followUpTask.getId() + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Complete the task which publishes TaskCompletedEvent
        mockMvc.perform(post("/api/v1/tasks/" + followUpTask.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify Rule 3 automatically created a task completed notification
        notifications = notificationRepository.findAll();
        assertTrue(notifications.stream()
                .anyMatch(n -> n.getMessage().contains("Task Inspect Crop Recovery completed successfully")));
    }

    @Test
    void testDisabledAutomationRulesAreIgnored() throws Exception {
        // 1. Setup Farmer
        String token = registerAndLogin("Farmer Emi", "9444444442", "emi.auto@krishi.com", "Pass@123");

        // 2. Disable Rule 2 in database
        AutomationRule rule2 = automationRuleRepository.findById(UUID.fromString("b2c3d4e5-f67a-8b9c-0d1e-2f3a4b5c6d7e")).get();
        rule2.setEnabled(false);
        automationRuleRepository.save(rule2);

        // 3. Create treatment and complete it
        UUID farmId = createFarm("Emi Farms", token);
        UUID cycleId = createCropCycleAndStart("Tomato season B", farmId, token);
        UUID plantingId = createCropPlanting(cycleId, token);
        UUID incidentId = reportDiseaseIncident(plantingId, token);
        UUID treatmentId = createTreatment(incidentId, token);

        // Start treatment
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Complete treatment (publishes TreatmentCompletedEvent)
        mockMvc.perform(post("/api/v1/treatments/" + treatmentId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 4. Verify Rule 2 did NOT create the follow-up task
        List<Task> tasks = taskRepository.findByTreatmentId(treatmentId);
        assertTrue(tasks.isEmpty());
    }
}
