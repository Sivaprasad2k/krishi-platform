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
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import com.krishi.backend.notification.repository.NotificationRepository;
import com.krishi.backend.task.dto.TaskCreateRequest;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SecurityRegressionTest {

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
    void testComprehensiveIDORProtectionAcrossAllDomains() throws Exception {
        // 1. Setup Farmer A and Farmer B
        String tokenA = registerAndLogin("Farmer Alice", "9888888801", "alice.sec@krishi.com", "Pass@123");
        String tokenB = registerAndLogin("Farmer Bob", "9888888802", "bob.sec@krishi.com", "Pass@123");

        // 2. Farmer A creates resources: Farm -> Season -> Crop -> Incident -> Treatment -> Task
        FarmCreateRequest farmReq = new FarmCreateRequest();
        farmReq.setName("Alice Secure Farm");
        farmReq.setSizeHectares(10.0);
        farmReq.setLocation("Punjab, IN");

        MvcResult farmRes = mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID farmIdA = UUID.fromString(objectMapper.readTree(farmRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropCycleCreateRequest cycleReq = new CropCycleCreateRequest();
        cycleReq.setName("Alice Wheat Season");
        cycleReq.setSeasonType(com.krishi.backend.cropcycle.entity.SeasonType.WINTER);
        cycleReq.setYear(2026);
        cycleReq.setFarmId(farmIdA);

        MvcResult cycleRes = mockMvc.perform(post("/api/v1/crop-cycles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cycleReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID cycleIdA = UUID.fromString(objectMapper.readTree(cycleRes.getResponse().getContentAsString()).get("data").get("id").asText());

        CropPlantingCreateRequest cropReq = new CropPlantingCreateRequest();
        cropReq.setCropCycleId(cycleIdA);
        cropReq.setCropName("Wheat");
        cropReq.setCropVariety("PBW 550");
        cropReq.setPlantingDate(LocalDateTime.now().minusDays(10));
        cropReq.setExpectedHarvestDate(LocalDateTime.now().plusDays(110));

        MvcResult cropRes = mockMvc.perform(post("/api/v1/crop-plantings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cropReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID plantingIdA = UUID.fromString(objectMapper.readTree(cropRes.getResponse().getContentAsString()).get("data").get("id").asText());

        DiseaseIncidentReportRequest incidentReq = new DiseaseIncidentReportRequest();
        incidentReq.setCropPlantingId(plantingIdA);
        incidentReq.setDiseaseName("Yellow Rust");
        incidentReq.setDescription("Yellow pustules on leaves");

        MvcResult incidentRes = mockMvc.perform(post("/api/v1/disease-incidents")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID incidentIdA = UUID.fromString(objectMapper.readTree(incidentRes.getResponse().getContentAsString()).get("data").get("id").asText());

        TreatmentPlanRequest treatmentReq = new TreatmentPlanRequest();
        treatmentReq.setDiseaseIncidentId(incidentIdA);
        treatmentReq.setTreatmentName("Tebuconazole Spray");
        treatmentReq.setDescription("Apply fungicide");
        treatmentReq.setRecommendedDosage("1ml/L");
        treatmentReq.setApplicationMethod("Foliar Spray");

        MvcResult treatmentRes = mockMvc.perform(post("/api/v1/treatments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(treatmentReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID treatmentIdA = UUID.fromString(objectMapper.readTree(treatmentRes.getResponse().getContentAsString()).get("data").get("id").asText());

        TaskCreateRequest taskReq = new TaskCreateRequest();
        taskReq.setCropPlantingId(plantingIdA);
        taskReq.setTreatmentId(treatmentIdA);
        taskReq.setTitle("Spray Tebuconazole");

        MvcResult taskRes = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andExpect(status().isOk())
                .andReturn();
        UUID taskIdA = UUID.fromString(objectMapper.readTree(taskRes.getResponse().getContentAsString()).get("data").get("id").asText());

        // 3. IDOR Protections Verification: Farmer B attempts access on Farmer A's resources -> 403 Forbidden
        // A. Farm GET
        mockMvc.perform(get("/api/v1/farms/" + farmIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // B. Season Start
        mockMvc.perform(post("/api/v1/crop-cycles/" + cycleIdA + "/start")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // C. Crop Planting GET
        mockMvc.perform(get("/api/v1/crop-plantings/" + plantingIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // D. Disease Incident GET
        mockMvc.perform(get("/api/v1/disease-incidents/" + incidentIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // E. Treatment Start
        mockMvc.perform(post("/api/v1/treatments/" + treatmentIdA + "/start")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // F. Task Complete
        mockMvc.perform(post("/api/v1/tasks/" + taskIdA + "/complete")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 4. Missing Token Test -> 403 / 401 Unauthorized
        mockMvc.perform(get("/api/v1/farms/" + farmIdA))
                .andExpect(status().isForbidden());
    }
}
