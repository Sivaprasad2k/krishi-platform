package com.krishi.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class JwtAuthenticationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void testJwtAuthenticationPipeline() throws Exception {
        // 1. Register a user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setPhoneNumber("9876543210");
        registerRequest.setPassword("Password@123");
        registerRequest.setEmail("john.doe@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 2. Login the user
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhoneNumber("9876543210");
        loginRequest.setPassword("Password@123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = loginResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseString, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        String accessToken = (String) data.get("accessToken");
        assertNotNull(accessToken);

        // 3. Try to access protected endpoint /api/v1/auth/principal
        MvcResult principalResult = mockMvc.perform(get("/api/v1/auth/principal")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String username = principalResult.getResponse().getContentAsString();
        assertEquals("9876543210", username);
    }
}
