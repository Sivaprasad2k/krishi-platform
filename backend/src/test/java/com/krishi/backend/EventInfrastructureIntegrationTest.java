package com.krishi.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishi.backend.common.event.entity.DomainEvent;
import com.krishi.backend.common.event.repository.DomainEventRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class EventInfrastructureIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainEventRepository domainEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        domainEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        domainEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testRegistrationAndLoginEventPersistenceWorkflow() throws Exception {
        // Assert no initial events
        assertEquals(0, domainEventRepository.count());

        // 1. Perform Registration
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName("Krishi Tester");
        registerRequest.setPhoneNumber("9999999999");
        registerRequest.setPassword("TestPass@123");
        registerRequest.setEmail("tester@krishi.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Verify that a UserRegisteredEvent has been persisted
        List<DomainEvent> eventsAfterRegister = domainEventRepository.findAll();
        assertEquals(1, eventsAfterRegister.size());

        DomainEvent registerEvent = eventsAfterRegister.get(0);
        assertNotNull(registerEvent.getId());
        assertEquals("USER", registerEvent.getAggregateType());
        assertEquals("USER_REGISTERED", registerEvent.getEventType());
        assertNotNull(registerEvent.getAggregateId());
        assertNotNull(registerEvent.getOccurredAt());
        assertEquals(registerEvent.getAggregateId(), registerEvent.getCreatedBy());

        // Parse serialized event data JSON
        JsonNode registerDataNode = objectMapper.readTree(registerEvent.getEventData());
        assertEquals("9999999999", registerDataNode.get("phoneNumber").asText());
        assertEquals("Krishi Tester", registerDataNode.get("fullName").asText());

        // 2. Perform Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhoneNumber("9999999999");
        loginRequest.setPassword("TestPass@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Verify that a UserLoggedInEvent has been persisted
        List<DomainEvent> eventsAfterLogin = domainEventRepository.findAll();
        assertEquals(2, eventsAfterLogin.size());

        // Find the UserLoggedInEvent (should be the second one by occurredAt or eventType)
        DomainEvent loginEvent = eventsAfterLogin.stream()
                .filter(e -> "USER_LOGGED_IN".equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("UserLoggedInEvent not found"));

        assertNotNull(loginEvent.getId());
        assertEquals("USER", loginEvent.getAggregateType());
        assertEquals(registerEvent.getAggregateId(), loginEvent.getAggregateId());
        assertNotNull(loginEvent.getOccurredAt());
        assertEquals(registerEvent.getAggregateId(), loginEvent.getCreatedBy());

        // Parse serialized event data JSON for Login
        JsonNode loginDataNode = objectMapper.readTree(loginEvent.getEventData());
        assertEquals("9999999999", loginDataNode.get("phoneNumber").asText());
    }
}
