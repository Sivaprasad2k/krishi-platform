package com.krishi.backend;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.krishi.backend.common.event.repository.DomainEventRepository;
import com.krishi.backend.cropcycle.dto.CropCycleCreateRequest;
import com.krishi.backend.cropcycle.entity.SeasonType;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.dto.CropPlantingCreateRequest;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentReportRequest;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import com.krishi.backend.notification.repository.NotificationRepository;
import com.krishi.backend.task.dto.TaskCreateRequest;
import com.krishi.backend.task.entity.TaskType;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.treatment.dto.TreatmentPlanRequest;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class CropWorkspaceIntegrationTest {

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

        notificationRepository.deleteAll();
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
        notificationRepository.deleteAll();
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

    @Test
    void testUnifiedCropWorkspaceReturnsCompleteAggregatedData() throws Exception {
        // 1. Register & login Farmer A
        String tokenA = registerAndLogin("Farmer Ram", "9777777701", "ram.workspace@krishi.com", "Pass@123");

        // 2. Create Farm
        FarmCreateRequest farmReq = new FarmCreateRequest();
        farmReq.setName("Valley Green Farm");
        farmReq.setSizeHectares(12.5);
        farmReq.setLocation("Thrissur, Kerala");

        MvcResult farmRes = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID farmId = UUID.fromString(objectMapper.readTree(farmRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 3. Create CropCycle
        CropCycleCreateRequest cycleReq = new CropCycleCreateRequest();
        cycleReq.setName("Mundakan Samba 2026");
        cycleReq.setSeasonType(SeasonType.MONSOON);
        cycleReq.setYear(2026);
        cycleReq.setFarmId(farmId);

        MvcResult cycleRes = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID cycleId = UUID.fromString(objectMapper.readTree(cycleRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 4. Create CropPlanting
        CropPlantingCreateRequest cropReq = new CropPlantingCreateRequest();
        cropReq.setCropCycleId(cycleId);
        cropReq.setCropName("Paddy");
        cropReq.setCropVariety("Uma (MO-16)");
        cropReq.setPlantingDate(LocalDateTime.now().minusDays(15));
        cropReq.setExpectedHarvestDate(LocalDateTime.now().plusDays(105));

        MvcResult cropRes = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cropReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID plantingId = UUID.fromString(objectMapper.readTree(cropRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 5. Create a general Crop Task (non-treatment task)
        TaskCreateRequest generalTaskReq = new TaskCreateRequest();
        generalTaskReq.setCropPlantingId(plantingId);
        generalTaskReq.setTaskType(TaskType.WEEDING);
        generalTaskReq.setTitle("First Weeding Round");
        generalTaskReq.setDescription("Manual weeding between rows");
        generalTaskReq.setDueDate(LocalDateTime.now().plusDays(3));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generalTaskReq)))
                .andExpect(status().isOk());

        // 6. Report a Disease Incident
        DiseaseIncidentReportRequest incidentReq = new DiseaseIncidentReportRequest();
        incidentReq.setCropPlantingId(plantingId);
        incidentReq.setDiseaseName("Sheath Blight");
        incidentReq.setDescription("Water-soaked lesions on lower leaf sheaths");

        MvcResult incidentRes = mockMvc.perform(post("/api/v1/disease-incidents")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID incidentId = UUID.fromString(objectMapper.readTree(incidentRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 7. Plan a Treatment
        TreatmentPlanRequest treatmentReq = new TreatmentPlanRequest();
        treatmentReq.setDiseaseIncidentId(incidentId);
        treatmentReq.setTreatmentName("Hexaconazole 5% EC");
        treatmentReq.setDescription("Apply 2ml per litre of water");
        treatmentReq.setRecommendedDosage("2 ml/L");
        treatmentReq.setApplicationMethod("Foliar Spray");

        MvcResult treatmentRes = mockMvc.perform(post("/api/v1/treatments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(treatmentReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID treatmentId = UUID.fromString(objectMapper.readTree(treatmentRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 8. Create a Treatment-linked Task
        TaskCreateRequest treatTaskReq = new TaskCreateRequest();
        treatTaskReq.setCropPlantingId(plantingId);
        treatTaskReq.setTreatmentId(treatmentId);
        treatTaskReq.setTaskType(TaskType.DISEASE_TREATMENT);
        treatTaskReq.setTitle("Spray Hexaconazole on affected rows");
        treatTaskReq.setDueDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(treatTaskReq)))
                .andExpect(status().isOk());

        // 9. Call Unified Crop Workspace Endpoint
        MvcResult workspaceResult = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/workspace")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(workspaceResult.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertNotNull(data);

        // Verify Crop Details
        assertEquals(plantingId.toString(), data.get("crop").get("id").asText());
        assertEquals("Paddy", data.get("crop").get("cropName").asText());
        assertEquals("Uma (MO-16)", data.get("crop").get("cropVariety").asText());

        // Verify Season Details
        assertEquals(cycleId.toString(), data.get("season").get("id").asText());
        assertEquals("Mundakan Samba 2026", data.get("season").get("name").asText());

        // Verify Farm Details
        assertEquals(farmId.toString(), data.get("farm").get("id").asText());
        assertEquals("Valley Green Farm", data.get("farm").get("name").asText());
        assertEquals("Valley Green Farm", data.get("farmName").asText());
        assertEquals(farmId.toString(), data.get("farmId").asText());

        // Verify Tasks (Both general task and treatment-linked task must be present)
        assertEquals(2, data.get("tasks").size());

        // Verify Disease Incidents
        assertEquals(1, data.get("incidents").size());
        assertEquals("Sheath Blight", data.get("incidents").get(0).get("diseaseName").asText());

        // Verify Open Incidents
        assertEquals(1, data.get("openIncidents").size());

        // Verify Treatments
        assertEquals(1, data.get("treatments").size());
        assertEquals("Hexaconazole 5% EC", data.get("treatments").get(0).get("treatmentName").asText());
    }

    @Test
    void testOpenIncidentsExcludesClosedIncidents() throws Exception {
        String token = registerAndLogin("Farmer ClosedCheck", "9777777702", "closed.test@krishi.com", "Pass@123");

        FarmCreateRequest farmReq = new FarmCreateRequest();
        farmReq.setName("Green Farm");
        farmReq.setSizeHectares(5.0);
        farmReq.setLocation("Palakkad, Kerala");
        MvcResult farmRes = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk()).andReturn();
        UUID farmId = UUID.fromString(objectMapper.readTree(farmRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropCycleCreateRequest cycleReq = new CropCycleCreateRequest();
        cycleReq.setName("Season 1");
        cycleReq.setSeasonType(SeasonType.MONSOON);
        cycleReq.setYear(2026);
        cycleReq.setFarmId(farmId);
        MvcResult cycleRes = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleReq)))
                .andExpect(status().isOk()).andReturn();
        UUID cycleId = UUID.fromString(objectMapper.readTree(cycleRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropPlantingCreateRequest cropReq = new CropPlantingCreateRequest();
        cropReq.setCropCycleId(cycleId);
        cropReq.setCropName("Tomato");
        cropReq.setCropVariety("PKM-1");
        cropReq.setPlantingDate(LocalDateTime.now().minusDays(5));
        MvcResult cropRes = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cropReq)))
                .andExpect(status().isOk()).andReturn();
        UUID plantingId = UUID.fromString(objectMapper.readTree(cropRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // Report incident
        DiseaseIncidentReportRequest incidentReq = new DiseaseIncidentReportRequest();
        incidentReq.setCropPlantingId(plantingId);
        incidentReq.setDiseaseName("Early Blight");
        MvcResult incidentRes = mockMvc.perform(post("/api/v1/disease-incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentReq)))
                .andExpect(status().isOk()).andReturn();
        UUID incidentId = UUID.fromString(objectMapper.readTree(incidentRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // Move to REVIEW -> TREAT -> CLOSE
        mockMvc.perform(post("/api/v1/disease-incidents/" + incidentId + "/review")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/disease-incidents/" + incidentId + "/treat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/disease-incidents/" + incidentId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Workspace call: incidents should have 1, but openIncidents should have 0
        MvcResult workspaceResult = mockMvc.perform(get("/api/v1/crop-plantings/" + plantingId + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(workspaceResult.getResponse().getContentAsString()).get("data");
        assertEquals(1, data.get("incidents").size());
        assertEquals("CLOSED", data.get("incidents").get(0).get("status").asText());
        assertEquals(0, data.get("openIncidents").size());
    }

    @Test
    void testIDORProtectionCrossFarmerAccessForbidden() throws Exception {
        String tokenA = registerAndLogin("Farmer Alice", "9777777711", "alice.ws@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9777777712", "bob.ws@krishi.com", "Pass@123");

        // Alice creates farm, cycle, crop
        FarmCreateRequest farmReq = new FarmCreateRequest();
        farmReq.setName("Alice Farm");
        farmReq.setSizeHectares(4.0);
        farmReq.setLocation("Kerala");
        MvcResult farmRes = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk()).andReturn();
        UUID farmIdA = UUID.fromString(objectMapper.readTree(farmRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropCycleCreateRequest cycleReq = new CropCycleCreateRequest();
        cycleReq.setName("Alice Season");
        cycleReq.setSeasonType(SeasonType.MONSOON);
        cycleReq.setYear(2026);
        cycleReq.setFarmId(farmIdA);
        MvcResult cycleRes = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleReq)))
                .andExpect(status().isOk()).andReturn();
        UUID cycleIdA = UUID.fromString(objectMapper.readTree(cycleRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropPlantingCreateRequest cropReq = new CropPlantingCreateRequest();
        cropReq.setCropCycleId(cycleIdA);
        cropReq.setCropName("Banana");
        cropReq.setCropVariety("Nendran");
        cropReq.setPlantingDate(LocalDateTime.now());
        MvcResult cropRes = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cropReq)))
                .andExpect(status().isOk()).andReturn();
        UUID plantingIdA = UUID.fromString(objectMapper.readTree(cropRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // Bob tries to access Alice's workspace -> 403 Forbidden
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingIdA + "/workspace")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedWorkspaceAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/crop-plantings/" + UUID.randomUUID() + "/workspace"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNonExistentPlantingReturnsNotFound() throws Exception {
        String token = registerAndLogin("Farmer Exists", "9777777713", "exists@krishi.com", "Pass@123");

        mockMvc.perform(get("/api/v1/crop-plantings/" + UUID.randomUUID() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
