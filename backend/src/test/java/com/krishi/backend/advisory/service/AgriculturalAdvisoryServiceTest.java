package com.krishi.backend.advisory.service;

import com.krishi.backend.advisory.dto.AdvisoryEvaluationSummaryResponse;
import com.krishi.backend.advisory.dto.AgriculturalAdvisoryResponse;
import com.krishi.backend.advisory.dto.RuleEvaluationReportItem;
import com.krishi.backend.advisory.entity.AdvisoryStatus;
import com.krishi.backend.advisory.entity.AgriculturalAdvisory;
import com.krishi.backend.advisory.mapper.AgriculturalAdvisoryMapper;
import com.krishi.backend.advisory.repository.AgriculturalAdvisoryRepository;
import com.krishi.backend.advisory.rule.AdvisoryProposal;
import com.krishi.backend.advisory.rule.RuleEvaluationOutcome;
import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.cropcycle.entity.CropCycle;
import com.krishi.backend.cropplanting.dto.context.AgriculturalContextResponse;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.cropplanting.service.AgriculturalContextService;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.notification.entity.NotificationType;
import com.krishi.backend.notification.service.NotificationService;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.identity.entity.Role;
import com.krishi.backend.identity.entity.User;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Agricultural Advisory Service Unit Tests")
class AgriculturalAdvisoryServiceTest {

    @Mock
    private CropPlantingRepository cropPlantingRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private AgriculturalAdvisoryRepository advisoryRepository;
    @Mock
    private AgriculturalContextService agriculturalContextService;
    @Mock
    private AgriculturalAdvisoryEngine advisoryEngine;
    @Mock
    private AgriculturalAdvisoryMapper advisoryMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private AgriculturalAdvisoryService advisoryService;

    private UUID userId;
    private UUID farmId;
    private UUID plantingId;
    private User owner;
    private Farm farm;
    private CropPlanting planting;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        farmId = UUID.randomUUID();
        plantingId = UUID.randomUUID();

        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("FARMER");

        owner = new User();
        owner.setId(userId);
        owner.setPhoneNumber("9876543210");
        owner.setFullName("Test Farmer");
        owner.setRole(role);
        owner.setPasswordHash("hashed_pwd");
        owner.setActive(true);

        farm = new Farm();
        farm.setId(farmId);
        farm.setOwner(owner);

        CropCycle cycle = new CropCycle();
        cycle.setId(UUID.randomUUID());
        cycle.setFarm(farm);

        planting = new CropPlanting();
        planting.setId(plantingId);
        planting.setCropName("Paddy");
        planting.setCropCycle(cycle);

        userDetails = new CustomUserDetails(owner);
    }

    @Test
    @DisplayName("Should create new active advisory and dispatch notification on first trigger")
    void testCreateNewAdvisoryAndNotify() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agriculturalContextService.getAgriculturalContext(eq(plantingId), any(LocalDate.class), eq(userDetails)))
                .thenReturn(AgriculturalContextResponse.builder().build());

        AdvisoryProposal proposal = AdvisoryProposal.builder()
                .fingerprint("fp:1")
                .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                .severity(AdvisorySeverity.HIGH)
                .title("Heavy Rain Irrigation Alert")
                .message("Heavy rainfall expected")
                .recommendedAction("Withhold irrigation")
                .targetDate(LocalDate.now())
                .provenance("Standard ICAR")
                .build();

        AgriculturalAdvisoryEngine.EngineExecutionResult execResult = AgriculturalAdvisoryEngine.EngineExecutionResult.builder()
                .reportItems(List.of(RuleEvaluationReportItem.builder()
                        .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                        .outcome(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS)
                        .proposalsCount(1)
                        .build()))
                .proposals(List.of(proposal))
                .build();

        when(advisoryEngine.evaluate(any())).thenReturn(execResult);
        when(advisoryRepository.findByAdvisoryFingerprintAndStatus("fp:1", AdvisoryStatus.ACTIVE)).thenReturn(Optional.empty());
        when(advisoryRepository.findFirstByAdvisoryFingerprintOrderByCreatedAtDesc("fp:1")).thenReturn(Optional.empty());

        AgriculturalAdvisory saved = AgriculturalAdvisory.builder()
                .id(UUID.randomUUID())
                .advisoryFingerprint("fp:1")
                .farm(farm)
                .cropPlanting(planting)
                .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                .severity(AdvisorySeverity.HIGH)
                .status(AdvisoryStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        when(advisoryRepository.saveAndFlush(any(AgriculturalAdvisory.class))).thenReturn(saved);
        when(advisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(plantingId, AdvisoryStatus.ACTIVE))
                .thenReturn(List.of(saved));
        when(advisoryMapper.toResponse(saved)).thenReturn(AgriculturalAdvisoryResponse.builder().id(saved.getId()).build());

        AdvisoryEvaluationSummaryResponse summary = advisoryService.evaluateAdvisories(plantingId, userDetails);

        assertNotNull(summary);
        assertEquals(1, summary.getActiveAdvisoriesCount());
        verify(advisoryRepository).saveAndFlush(any(AgriculturalAdvisory.class));
        verify(notificationService).createNotification(eq(userId), eq("Heavy Rain Irrigation Alert"), anyString(), eq(NotificationType.AGRICULTURAL_ADVISORY));
        verify(domainEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should be idempotent on repeated evaluation: update lastEvaluatedAt without re-notifying")
    void testRepeatedEvaluationIdempotency() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agriculturalContextService.getAgriculturalContext(eq(plantingId), any(LocalDate.class), eq(userDetails)))
                .thenReturn(AgriculturalContextResponse.builder().build());

        AdvisoryProposal proposal = AdvisoryProposal.builder()
                .fingerprint("fp:existing")
                .ruleCode("OVERDUE_CULTIVATION_TASK")
                .severity(AdvisorySeverity.MEDIUM)
                .title("Overdue Task Alert")
                .message("Task is overdue")
                .targetDate(LocalDate.now())
                .build();

        AgriculturalAdvisoryEngine.EngineExecutionResult execResult = AgriculturalAdvisoryEngine.EngineExecutionResult.builder()
                .reportItems(List.of(RuleEvaluationReportItem.builder()
                        .ruleCode("OVERDUE_CULTIVATION_TASK")
                        .outcome(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS)
                        .proposalsCount(1)
                        .build()))
                .proposals(List.of(proposal))
                .build();

        when(advisoryEngine.evaluate(any())).thenReturn(execResult);

        AgriculturalAdvisory existing = AgriculturalAdvisory.builder()
                .id(UUID.randomUUID())
                .advisoryFingerprint("fp:existing")
                .farm(farm)
                .cropPlanting(planting)
                .ruleCode("OVERDUE_CULTIVATION_TASK")
                .severity(AdvisorySeverity.MEDIUM)
                .status(AdvisoryStatus.ACTIVE)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .lastEvaluatedAt(OffsetDateTime.now().minusHours(2))
                .build();

        when(advisoryRepository.findByAdvisoryFingerprintAndStatus("fp:existing", AdvisoryStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(advisoryRepository.save(existing)).thenReturn(existing);
        when(advisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(plantingId, AdvisoryStatus.ACTIVE))
                .thenReturn(List.of(existing));

        advisoryService.evaluateAdvisories(plantingId, userDetails);

        // Saved existing with updated timestamp, but did NOT create notification or publish created event!
        verify(advisoryRepository).save(existing);
        verify(notificationService, never()).createNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should escalate severity: mark prior SUPERSEDED, create new ACTIVE, and notify with [ESCALATION]")
    void testSeverityEscalation() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agriculturalContextService.getAgriculturalContext(eq(plantingId), any(LocalDate.class), eq(userDetails)))
                .thenReturn(AgriculturalContextResponse.builder().build());

        // New evaluation has HIGH severity (escalation from MEDIUM)
        AdvisoryProposal escalatedProposal = AdvisoryProposal.builder()
                .fingerprint("fp:escalate")
                .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                .severity(AdvisorySeverity.HIGH)
                .title("Heavy Rain Irrigation Alert")
                .message("Heavy rainfall expected")
                .targetDate(LocalDate.now())
                .build();

        AgriculturalAdvisoryEngine.EngineExecutionResult execResult = AgriculturalAdvisoryEngine.EngineExecutionResult.builder()
                .reportItems(List.of(RuleEvaluationReportItem.builder()
                        .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                        .outcome(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS)
                        .proposalsCount(1)
                        .build()))
                .proposals(List.of(escalatedProposal))
                .build();

        when(advisoryEngine.evaluate(any())).thenReturn(execResult);

        AgriculturalAdvisory existingMedium = AgriculturalAdvisory.builder()
                .id(UUID.randomUUID())
                .advisoryFingerprint("fp:escalate")
                .farm(farm)
                .cropPlanting(planting)
                .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                .severity(AdvisorySeverity.MEDIUM)
                .status(AdvisoryStatus.ACTIVE)
                .createdAt(OffsetDateTime.now().minusHours(4))
                .build();

        when(advisoryRepository.findByAdvisoryFingerprintAndStatus("fp:escalate", AdvisoryStatus.ACTIVE))
                .thenReturn(Optional.of(existingMedium));

        AgriculturalAdvisory escalatedSaved = AgriculturalAdvisory.builder()
                .id(UUID.randomUUID())
                .advisoryFingerprint("fp:escalate")
                .farm(farm)
                .cropPlanting(planting)
                .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                .severity(AdvisorySeverity.HIGH)
                .status(AdvisoryStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        when(advisoryRepository.saveAndFlush(any(AgriculturalAdvisory.class))).thenReturn(escalatedSaved);
        when(advisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(plantingId, AdvisoryStatus.ACTIVE))
                .thenReturn(List.of(escalatedSaved));

        advisoryService.evaluateAdvisories(plantingId, userDetails);

        assertEquals(AdvisoryStatus.SUPERSEDED, existingMedium.getStatus());
        verify(advisoryRepository).saveAndFlush(existingMedium);
        verify(notificationService).createNotification(
                eq(userId),
                eq("[ESCALATION] Heavy Rain Irrigation Alert"),
                anyString(),
                eq(NotificationType.AGRICULTURAL_ADVISORY)
        );
    }

    @Test
    @DisplayName("Should acknowledge active advisory and publish event")
    void testAcknowledgeAdvisory() {
        UUID advisoryId = UUID.randomUUID();
        AgriculturalAdvisory advisory = AgriculturalAdvisory.builder()
                .id(advisoryId)
                .farm(farm)
                .cropPlanting(planting)
                .status(AdvisoryStatus.ACTIVE)
                .ruleCode("STAGE_TASK_MISALIGNMENT")
                .build();

        when(advisoryRepository.findById(advisoryId)).thenReturn(Optional.of(advisory));
        when(advisoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(advisoryMapper.toResponse(any())).thenReturn(AgriculturalAdvisoryResponse.builder().id(advisoryId).status(AdvisoryStatus.ACKNOWLEDGED).build());

        AgriculturalAdvisoryResponse response = advisoryService.acknowledgeAdvisory(advisoryId, userDetails);

        assertEquals(AdvisoryStatus.ACKNOWLEDGED, response.getStatus());
        assertEquals(AdvisoryStatus.ACKNOWLEDGED, advisory.getStatus());
        assertNotNull(advisory.getAcknowledgedAt());
        verify(domainEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should resolve advisory and publish event")
    void testResolveAdvisory() {
        UUID advisoryId = UUID.randomUUID();
        AgriculturalAdvisory advisory = AgriculturalAdvisory.builder()
                .id(advisoryId)
                .farm(farm)
                .cropPlanting(planting)
                .status(AdvisoryStatus.ACTIVE)
                .ruleCode("OVERDUE_CULTIVATION_TASK")
                .build();

        when(advisoryRepository.findById(advisoryId)).thenReturn(Optional.of(advisory));
        when(advisoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(advisoryMapper.toResponse(any())).thenReturn(AgriculturalAdvisoryResponse.builder().id(advisoryId).status(AdvisoryStatus.RESOLVED).build());

        AgriculturalAdvisoryResponse response = advisoryService.resolveAdvisory(advisoryId, userDetails);

        assertEquals(AdvisoryStatus.RESOLVED, response.getStatus());
        assertEquals(AdvisoryStatus.RESOLVED, advisory.getStatus());
        assertNotNull(advisory.getResolvedAt());
        verify(domainEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should reject access when another farmer attempts to evaluate or view planting")
    void testTenantIsolationCheck() {
        UUID otherFarmerId = UUID.randomUUID();
        User otherOwner = new User();
        otherOwner.setId(otherFarmerId);
        Farm otherFarm = new Farm();
        otherFarm.setId(UUID.randomUUID());
        otherFarm.setOwner(otherOwner);
        CropCycle otherCycle = new CropCycle();
        otherCycle.setFarm(otherFarm);
        CropPlanting otherPlanting = new CropPlanting();
        otherPlanting.setId(UUID.randomUUID());
        otherPlanting.setCropCycle(otherCycle);

        when(cropPlantingRepository.findById(otherPlanting.getId())).thenReturn(Optional.of(otherPlanting));

        assertThrows(AccessDeniedException.class, () -> advisoryService.evaluateAdvisories(otherPlanting.getId(), userDetails));
        assertThrows(AccessDeniedException.class, () -> advisoryService.getAdvisories(otherPlanting.getId(), null, userDetails));
    }

    @Test
    @DisplayName("Should persist advisory and publish domain event even if notification dispatch fails")
    void testAdvisoryPersistedWhenNotificationFails() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agriculturalContextService.getAgriculturalContext(eq(plantingId), any(LocalDate.class), eq(userDetails)))
                .thenReturn(AgriculturalContextResponse.builder().build());

        AdvisoryProposal proposal = AdvisoryProposal.builder()
                .fingerprint("fp:notif-fail")
                .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                .severity(AdvisorySeverity.HIGH)
                .title("Heavy Rain Alert")
                .message("Heavy rainfall expected")
                .recommendedAction("Withhold irrigation")
                .targetDate(LocalDate.now())
                .provenance("Standard ICAR")
                .build();

        AgriculturalAdvisoryEngine.EngineExecutionResult execResult = AgriculturalAdvisoryEngine.EngineExecutionResult.builder()
                .reportItems(List.of(RuleEvaluationReportItem.builder()
                        .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                        .outcome(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS)
                        .proposalsCount(1)
                        .build()))
                .proposals(List.of(proposal))
                .build();

        when(advisoryEngine.evaluate(any())).thenReturn(execResult);
        when(advisoryRepository.findByAdvisoryFingerprintAndStatus("fp:notif-fail", AdvisoryStatus.ACTIVE)).thenReturn(Optional.empty());
        when(advisoryRepository.findFirstByAdvisoryFingerprintOrderByCreatedAtDesc("fp:notif-fail")).thenReturn(Optional.empty());

        AgriculturalAdvisory saved = AgriculturalAdvisory.builder()
                .id(UUID.randomUUID())
                .advisoryFingerprint("fp:notif-fail")
                .farm(farm)
                .cropPlanting(planting)
                .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                .severity(AdvisorySeverity.HIGH)
                .status(AdvisoryStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        when(advisoryRepository.saveAndFlush(any(AgriculturalAdvisory.class))).thenReturn(saved);
        when(advisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(plantingId, AdvisoryStatus.ACTIVE))
                .thenReturn(List.of(saved));
        when(advisoryMapper.toResponse(saved)).thenReturn(AgriculturalAdvisoryResponse.builder().id(saved.getId()).build());

        // Simulate notification service throwing an exception (e.g., notification DB failure)
        doThrow(new RuntimeException("Notification DB outage"))
                .when(notificationService)
                .createNotification(any(), any(), any(), any());

        // Advisory evaluation should succeed despite notification failure
        AdvisoryEvaluationSummaryResponse summary = advisoryService.evaluateAdvisories(plantingId, userDetails);

        assertNotNull(summary);
        assertEquals(1, summary.getActiveAdvisoriesCount());
        // Verify advisory persistence and event publication still occurred
        verify(advisoryRepository).saveAndFlush(any(AgriculturalAdvisory.class));
        verify(domainEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should auto-resolve advisory and publish event when linked task is completed")
    void testAutoResolutionOnLinkedTaskCompletion() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agriculturalContextService.getAgriculturalContext(eq(plantingId), any(LocalDate.class), eq(userDetails)))
                .thenReturn(AgriculturalContextResponse.builder().build());

        // Empty proposals from rules engine
        AgriculturalAdvisoryEngine.EngineExecutionResult execResult = AgriculturalAdvisoryEngine.EngineExecutionResult.builder()
                .reportItems(List.of(RuleEvaluationReportItem.builder()
                        .ruleCode("OVERDUE_CULTIVATION_TASK")
                        .outcome(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS)
                        .proposalsCount(0)
                        .build()))
                .proposals(Collections.emptyList())
                .build();
        when(advisoryEngine.evaluate(any())).thenReturn(execResult);

        com.krishi.backend.task.entity.Task completedTask = new com.krishi.backend.task.entity.Task();
        completedTask.setId(UUID.randomUUID());
        completedTask.setTitle("Completed Weeding");
        completedTask.setStatus(com.krishi.backend.task.entity.TaskStatus.COMPLETED);

        AgriculturalAdvisory activeAdv = AgriculturalAdvisory.builder()
                .id(UUID.randomUUID())
                .advisoryFingerprint("fp:task-completed")
                .farm(farm)
                .cropPlanting(planting)
                .task(completedTask)
                .ruleCode("OVERDUE_CULTIVATION_TASK")
                .severity(AdvisorySeverity.MEDIUM)
                .status(AdvisoryStatus.ACTIVE)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();

        when(advisoryRepository.findByCropPlantingIdAndStatusInOrderByCreatedAtDesc(
                eq(plantingId), anyList()
        )).thenReturn(List.of(activeAdv));
        when(advisoryRepository.save(activeAdv)).thenReturn(activeAdv);
        when(advisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(plantingId, AdvisoryStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        AdvisoryEvaluationSummaryResponse summary = advisoryService.evaluateAdvisories(plantingId, userDetails);

        assertNotNull(summary);
        assertEquals(0, summary.getActiveAdvisoriesCount());
        assertEquals(AdvisoryStatus.RESOLVED, activeAdv.getStatus());
        assertNotNull(activeAdv.getResolvedAt());
        verify(domainEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should auto-resolve advisory and publish event when condition clears")
    void testAutoResolutionWhenConditionClears() {
        when(cropPlantingRepository.findById(plantingId)).thenReturn(Optional.of(planting));
        when(agriculturalContextService.getAgriculturalContext(eq(plantingId), any(LocalDate.class), eq(userDetails)))
                .thenReturn(AgriculturalContextResponse.builder().build());

        // Empty proposals from rules engine, but rule evaluated successfully clean
        AgriculturalAdvisoryEngine.EngineExecutionResult execResult = AgriculturalAdvisoryEngine.EngineExecutionResult.builder()
                .reportItems(List.of(RuleEvaluationReportItem.builder()
                        .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                        .outcome(RuleEvaluationOutcome.RULE_EVALUATION_SUCCESS)
                        .proposalsCount(0)
                        .build()))
                .proposals(Collections.emptyList())
                .build();
        when(advisoryEngine.evaluate(any())).thenReturn(execResult);

        AgriculturalAdvisory activeAdv = AgriculturalAdvisory.builder()
                .id(UUID.randomUUID())
                .advisoryFingerprint("fp:rain-cleared")
                .farm(farm)
                .cropPlanting(planting)
                .ruleCode("IRRIGATION_RAINFALL_CONFLICT")
                .severity(AdvisorySeverity.HIGH)
                .status(AdvisoryStatus.ACTIVE)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();

        when(advisoryRepository.findByCropPlantingIdAndStatusInOrderByCreatedAtDesc(
                eq(plantingId), anyList()
        )).thenReturn(List.of(activeAdv));
        when(advisoryRepository.save(activeAdv)).thenReturn(activeAdv);
        when(advisoryRepository.findByCropPlantingIdAndStatusOrderByCreatedAtDesc(plantingId, AdvisoryStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        AdvisoryEvaluationSummaryResponse summary = advisoryService.evaluateAdvisories(plantingId, userDetails);

        assertNotNull(summary);
        assertEquals(0, summary.getActiveAdvisoriesCount());
        assertEquals(AdvisoryStatus.RESOLVED, activeAdv.getStatus());
        assertNotNull(activeAdv.getResolvedAt());
        verify(domainEventPublisher).publish(any());
    }
}
