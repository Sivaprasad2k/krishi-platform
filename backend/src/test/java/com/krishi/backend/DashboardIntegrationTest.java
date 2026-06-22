package com.krishi.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishi.backend.cropcycle.repository.CropCycleRepository;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.diseaseincident.repository.DiseaseIncidentRepository;
import com.krishi.backend.farm.dto.FarmCreateRequest;
import com.krishi.backend.farm.repository.FarmRepository;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.repository.UserRepository;
import com.krishi.backend.notification.repository.NotificationRepository;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class DashboardIntegrationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        cleanAll();
    }

    @AfterEach
    void cleanUp() {
        cleanAll();
    }

    private void cleanAll() {
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

    @Test
    void testDashboardSummaryCounts() throws Exception {
        // 1. Register and Login Farmer
        String token = registerAndLogin("Farmer Dan", "9000000003", "dan@farm.com", "Pass@123");

        // 2. Fetch initial dashboard summary
        MvcResult initialResult = mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String initialResponseStr = initialResult.getResponse().getContentAsString();
        JsonNode initialNode = objectMapper.readTree(initialResponseStr);
        assertTrue(initialNode.get("success").asBoolean());
        JsonNode data = initialNode.get("data");
        assertEquals(0, data.get("farmCount").asLong());
        assertEquals(0, data.get("activeCropCycles").asLong());
        assertEquals(0, data.get("pendingTasks").asLong());
        assertEquals(0, data.get("openProblems").asLong());
        assertEquals(0, data.get("unreadNotifications").asLong());

        // 3. Create a Farm
        FarmCreateRequest createRequest = new FarmCreateRequest();
        createRequest.setName("Dan Valley");
        createRequest.setSizeHectares(5.0);
        createRequest.setLocation("Tamil Nadu, IN");

        mockMvc.perform(post("/api/v1/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // 4. Fetch dashboard summary again
        MvcResult updatedResult = mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String updatedResponseStr = updatedResult.getResponse().getContentAsString();
        JsonNode updatedNode = objectMapper.readTree(updatedResponseStr);
        assertTrue(updatedNode.get("success").asBoolean());
        JsonNode updatedData = updatedNode.get("data");
        assertEquals(1, updatedData.get("farmCount").asLong());
        assertEquals(0, updatedData.get("activeCropCycles").asLong());
        assertEquals(0, updatedData.get("pendingTasks").asLong());
        assertEquals(0, updatedData.get("openProblems").asLong());
        assertEquals(0, updatedData.get("unreadNotifications").asLong());
    }

    @Test
    void testDashboardSummaryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isForbidden());
    }
}
