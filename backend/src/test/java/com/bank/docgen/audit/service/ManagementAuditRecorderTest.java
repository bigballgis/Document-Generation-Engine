package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import com.bank.docgen.legalhold.persistence.LegalHoldEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementAuditRecorderTest {

    @Mock
    private ManagementAuditEventRepository repository;

    private ManagementAuditRecorder recorder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ManagementAuditEventWriter eventWriter = new ManagementAuditEventWriter(repository, objectMapper);
        recorder = new ManagementAuditRecorder(
                new ApiPolicyAuditRecorder(eventWriter),
                new IdentityAuditRecorder(eventWriter),
                new CollaborationAuditRecorder(eventWriter),
                new ContentModuleAuditRecorder(eventWriter),
                new TemplateTransferAuditRecorder(eventWriter),
                new TestDataSetAuditRecorder(eventWriter),
                new InvocationRegenerationAuditRecorder(eventWriter),
                new AssetLibraryAuditRecorder(eventWriter),
                new LegalHoldAuditRecorder(eventWriter),
                new TemplateAnnualReviewAuditRecorder(eventWriter)
        );
    }

    @Test
    void policySave_recordsChangedAreasAndPrevNextVersion() throws Exception {
        UUID templateId = UUID.randomUUID();
        PolicyUpdateAuditDetail detail = new PolicyUpdateAuditDetail(
                List.of("DEFAULT_ROUTE_TARGET: 1.0.0 -> 2.0.0"),
                List.of("api.apimgmt.policyImpact.idempotencyDefaultRouteGuard"),
                List.of(),
                List.of("api.apimgmt.policyImpact.defaultRouteChanged"),
                true,
                false,
                null
        );

        recorder.recordPolicyUpdated(
                templateId,
                "RETAIL",
                2,
                3,
                List.of("DEFAULT_ROUTE_TARGET"),
                "10000002",
                "Admin (10000002)",
                detail
        );

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(ManagementAuditEventTypes.API_POLICY_UPDATED);
        assertThat(saved.getPreviousPolicyVersion()).isEqualTo(2);
        assertThat(saved.getPolicyVersion()).isEqualTo(3);
        assertThat(saved.getChangedAreasJson()).contains("DEFAULT_ROUTE_TARGET");
        assertThat(saved.isRollback()).isFalse();
        assertThat(saved.getRollbackSourcePolicyVersion()).isNull();

        JsonNode payload = objectMapper.readTree(saved.getWarningCodesJson());
        assertThat(payload.get("configDiffSummary").get(0).asText()).contains("1.0.0");
        assertThat(payload.get("impactPreviewSummary").get(0).asText())
                .isEqualTo("api.apimgmt.policyImpact.idempotencyDefaultRouteGuard");
        assertThat(payload.get("warningSummary").get(0).asText())
                .isEqualTo("api.apimgmt.policyImpact.defaultRouteChanged");
        assertThat(payload.get("confirmed").asBoolean()).isTrue();
    }

    @Test
    void rollbackSave_recordsRollbackSourceVersion() throws Exception {
        UUID templateId = UUID.randomUUID();
        PolicyUpdateAuditDetail detail = new PolicyUpdateAuditDetail(
                List.of("ROLLBACK: v3 -> snapshot v1 as v4"),
                List.of("api.apimgmt.policyImpact.safe"),
                List.of(),
                List.of(),
                true,
                true,
                1
        );

        recorder.recordPolicyUpdated(
                templateId,
                "RETAIL",
                3,
                4,
                List.of("OUTPUT_POLICY"),
                "10000002",
                "Admin (10000002)",
                detail
        );

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.isRollback()).isTrue();
        assertThat(saved.getRollbackSourcePolicyVersion()).isEqualTo(1);
        assertThat(saved.getPreviousPolicyVersion()).isEqualTo(3);
        assertThat(saved.getPolicyVersion()).isEqualTo(4);

        JsonNode payload = objectMapper.readTree(saved.getWarningCodesJson());
        assertThat(payload.get("rollback").asBoolean()).isTrue();
        assertThat(payload.get("rollbackSourcePolicyVersion").asInt()).isEqualTo(1);
    }

    @Test
    void audit_excludesSecretsAndFullMembers() throws Exception {
        UUID templateId = UUID.randomUUID();
        PolicyUpdateAuditDetail detail = new PolicyUpdateAuditDetail(
                List.of("AD_GROUP_AUTHORIZATION: groupCount=2"),
                List.of(),
                List.of(),
                List.of(),
                true,
                false,
                null
        );

        recorder.recordPolicyUpdated(
                templateId,
                "RETAIL",
                1,
                2,
                List.of("AD_GROUP_AUTHORIZATION"),
                "10000002",
                "Admin (10000002)",
                detail
        );

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();
        String serialized = saved.getWarningCodesJson() + saved.getStatusSummary() + saved.getChangedAreasJson();

        assertThat(serialized).doesNotContain("super-secret-password");
        assertThat(serialized).doesNotContain("RETAIL_API");
        assertThat(serialized).doesNotContain("WHOLESALE_API");
        assertThat(serialized).contains("groupCount=2");
    }

    @Test
    void contentModuleLifecycleOperation_persistsImpactPayload() throws Exception {
        UUID moduleId = UUID.randomUUID();
        ContentModuleLifecycleAuditDetail detail = new ContentModuleLifecycleAuditDetail(
                2,
                "TPL-LOAN-NOTICE,TPL-RENEWAL-NOTICE",
                "v1.0.0,v1.1.0",
                true,
                "recentCalls=12/7d",
                "migrate callers to MOD-LOAN-DISCLOSURE-V3",
                true,
                true
        );

        recorder.recordContentModuleLifecycleOperation(
                moduleId,
                "RETAIL",
                "MOD-LOAN-DISCLOSURE",
                "STOP_USE",
                "1.0.0",
                "STOPPED",
                "10000002",
                "Admin (10000002)",
                detail
        );

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(ManagementAuditEventTypes.CONTENT_MODULE_LIFECYCLE_OPERATION);
        JsonNode payload = objectMapper.readTree(saved.getWarningCodesJson());
        assertThat(payload.get("referenceTemplateCount").asInt()).isEqualTo(2);
        assertThat(payload.get("recentCallSummary").asText()).isEqualTo("recentCalls=12/7d");
        assertThat(payload.get("templateStopRequired").asBoolean()).isTrue();
        assertThat(payload.get("releaseStopRequired").asBoolean()).isTrue();
    }

    @Test
    void collaborationWorkItemCreated_recordsNonSensitiveSummary() {
        UUID templateId = UUID.randomUUID();
        UUID workItemId = UUID.randomUUID();

        recorder.recordCollaborationWorkItemCreated(
                templateId,
                "RETAIL",
                workItemId,
                CollaborationWorkItemQueue.REMEDIATION,
                CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT,
                "10000003",
                "Tester (10000003)"
        );

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(ManagementAuditEventTypes.COLLABORATION_WORK_ITEM_CREATED);
        assertThat(saved.getTemplateId()).isEqualTo(templateId);
        assertThat(saved.getGroupCode()).isEqualTo("RETAIL");
        assertThat(saved.getActorUsername()).isEqualTo("10000003");
        assertThat(saved.getStatusSummary()).contains("REMEDIATION");
        assertThat(saved.getStatusSummary()).contains("TEST_FAILURE_OR_RETURN_TO_DRAFT");
        assertThat(saved.getChangedAreasJson()).contains(workItemId.toString());
    }

    @Test
    void collaborationWorkItemResolved_recordsNonSensitiveSummary() {
        UUID templateId = UUID.randomUUID();
        UUID workItemId = UUID.randomUUID();

        recorder.recordCollaborationWorkItemResolved(
                templateId,
                "RETAIL",
                workItemId,
                CollaborationWorkItemQueue.TEST,
                "10000003",
                "Tester (10000003)"
        );

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(ManagementAuditEventTypes.COLLABORATION_WORK_ITEM_RESOLVED);
        assertThat(saved.getTemplateId()).isEqualTo(templateId);
        assertThat(saved.getGroupCode()).isEqualTo("RETAIL");
        assertThat(saved.getStatusSummary()).contains("TEST");
        assertThat(saved.getChangedAreasJson()).contains(workItemId.toString());
    }

    @Test
    void testDataPiiExplicitConfirm_recordsKeysCategoriesReasonWithoutVariablePlaintext() throws Exception {
        UUID templateId = UUID.randomUUID();
        String secretValue = "RealCustomer-SECRET-VALUE";
        String variablesHash = "a".repeat(64);

        recorder.recordTestDataPiiExplicitConfirm(
                templateId,
                "RETAIL",
                "TDS-1",
                2,
                variablesHash,
                List.of("customerName"),
                Map.of("customerName", "PERSONAL_NAME"),
                "Approved fixture for QA",
                "10000003",
                "Author (10000003)"
        );

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(ManagementAuditEventTypes.TEMPLATE_TEST_DATA_PII_EXPLICIT_CONFIRM);
        assertThat(saved.getTemplateId()).isEqualTo(templateId);
        assertThat(saved.getGroupCode()).isEqualTo("RETAIL");
        assertThat(saved.getActorUsername()).isEqualTo("10000003");
        assertThat(saved.getStatusSummary()).contains("TDS-1");
        assertThat(saved.getWarningCodesJson()).doesNotContain(secretValue);
        assertThat(saved.getStatusSummary()).doesNotContain(secretValue);

        JsonNode payload = objectMapper.readTree(saved.getWarningCodesJson());
        assertThat(payload.get("testDataSetId").asText()).isEqualTo("TDS-1");
        assertThat(payload.get("datasetVersion").asInt()).isEqualTo(2);
        assertThat(payload.get("variablesHash").asText()).isEqualTo(variablesHash);
        assertThat(payload.get("piiFieldKeys").get(0).asText()).isEqualTo("customerName");
        assertThat(payload.get("piiCategories").get("customerName").asText()).isEqualTo("PERSONAL_NAME");
        assertThat(payload.get("piiHandling").asText()).isEqualTo("EXPLICIT_SENSITIVE");
        assertThat(payload.get("piiConfirmReason").asText()).isEqualTo("Approved fixture for QA");
        assertThat(payload.has("variables")).isFalse();
        assertThat(payload.has("customerName")).isFalse();
    }

    @Test
    void invocationRegenerated_scopesTemplateAndGroupWithoutVariables() throws Exception {
        UUID templateId = UUID.randomUUID();
        recorder.recordInvocationRegenerated(new com.bank.docgen.apimgmt.api.InvocationRegeneratedAuditDetail(
                "INV-1",
                "regen-1",
                templateId.toString(),
                "b".repeat(64),
                "PDF",
                "SUCCESS",
                null,
                "10000002",
                false,
                templateId,
                "GRP-A",
                false,
                true,
                null
        ));

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(ManagementAuditEventTypes.INVOCATION_REGENERATED);
        assertThat(saved.getTemplateId()).isEqualTo(templateId);
        assertThat(saved.getGroupCode()).isEqualTo("GRP-A");
        assertThat(saved.getActorUsername()).isEqualTo("10000002");
        assertThat(saved.getWarningCodesJson()).doesNotContain("Alice");
        JsonNode payload = objectMapper.readTree(saved.getWarningCodesJson());
        assertThat(payload.get("sourceInvocationId").asText()).isEqualTo("INV-1");
        assertThat(payload.get("regenerationId").asText()).isEqualTo("regen-1");
        assertThat(payload.get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(payload.get("encryptionReapplied").asBoolean()).isFalse();
        assertThat(payload.get("productionReissue").asBoolean()).isFalse();
        assertThat(payload.get("specimen").asBoolean()).isTrue();
        assertThat(payload.has("reason")).isFalse();
        assertThat(payload.has("variables")).isFalse();
    }

    @Test
    void invocationRegenerated_productionReissueRecordsReasonAndSpecimenFalse() throws Exception {
        UUID templateId = UUID.randomUUID();
        recorder.recordInvocationRegenerated(new com.bank.docgen.apimgmt.api.InvocationRegeneratedAuditDetail(
                "INV-1",
                "regen-prod",
                templateId.toString(),
                "b".repeat(64),
                "PDF",
                "SUCCESS",
                null,
                "10000002",
                false,
                templateId,
                "GRP-A",
                true,
                false,
                "Customer reprint after courier loss"
        ));

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getWarningCodesJson());
        assertThat(payload.get("productionReissue").asBoolean()).isTrue();
        assertThat(payload.get("specimen").asBoolean()).isFalse();
        assertThat(payload.get("reason").asText()).isEqualTo("Customer reprint after courier loss");
        assertThat(payload.has("variables")).isFalse();
    }

    @Test
    void legalHoldCreated_recordsSummaryWithoutSensitivePayload() throws Exception {
        UUID holdId = UUID.randomUUID();
        LegalHoldEntity hold = new LegalHoldEntity(
                holdId,
                "HOLD-AB12CD34",
                LegalHoldScopeType.INVOCATION_SET,
                LegalHoldStatus.ACTIVE,
                "litigation matter",
                null,
                null,
                null,
                null,
                Instant.parse("2026-07-01T00:00:00Z"),
                "10000001",
                Set.of("INV-SECRET-1", "INV-SECRET-2")
        );

        recorder.recordLegalHoldCreated(hold, "10000001", "Admin (10000001)");

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(ManagementAuditEventTypes.LEGAL_HOLD_CREATED);
        assertThat(saved.getActorUsername()).isEqualTo("10000001");
        assertThat(saved.getChangedAreasJson()).doesNotContain("INV-SECRET");
        assertThat(saved.getChangedAreasJson()).doesNotContain("variables");
        assertThat(saved.getStatusSummary()).doesNotContain("INV-SECRET");
        assertThat(saved.getWarningCodesJson()).isEqualTo("[]");

        JsonNode payload = objectMapper.readTree(saved.getChangedAreasJson());
        assertThat(payload.get("holdId").asText()).isEqualTo(holdId.toString());
        assertThat(payload.get("holdExternalId").asText()).isEqualTo("HOLD-AB12CD34");
        assertThat(payload.get("scopeType").asText()).isEqualTo("INVOCATION_SET");
        assertThat(payload.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(payload.get("invocationCount").asInt()).isEqualTo(2);
        assertThat(payload.get("reason").asText()).isEqualTo("litigation matter");
        assertThat(payload.get("actorUsername").asText()).isEqualTo("10000001");
        assertThat(payload.has("templateId")).isFalse();
        assertThat(payload.has("variables")).isFalse();
        assertThat(payload.has("invocationExternalIds")).isFalse();
    }
}
