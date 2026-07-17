package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.service.MasterCurrentRevisionUnavailableException;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CE-K01 BDD-CE-K01-001..005 + 015: publish flow pins master revision + hash + metadata,
 * and fail-closes when the current revision cannot be resolved.
 */
@ExtendWith(MockitoExtension.class)
class TemplateLifecyclePublishPinningTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VERSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MASTER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID REVISION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock private TemplateService templateService;
    @Mock private TemplateRepository templateRepository;
    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private PublishGateService publishGateService;
    @Mock private DecisionFormService decisionFormService;
    @Mock private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock private CollaborationWorkItemWriter collaborationWorkItemWriter;
    @Mock private RenderProfileService renderProfileService;
    @Mock private ApiPolicyMaterializationService apiPolicyMaterializationService;
    @Mock private VersionFidelityWarningService versionFidelityWarningService;
    @Mock private MessageResolver messageResolver;
    @Mock private TemplateLifecycleTransitionSupport transitions;
    @Mock private TemplateLifecycleDecisionCommentSupport decisionComments;
    @Mock private TemplateLifecycleEligibilitySupport eligibility;
    @Mock private MasterDocumentRepository masterDocumentRepository;
    @Mock private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock private ObjectStoragePort objectStoragePort;

    private TemplateLifecycleApprovalFlowSupport publishFlow;
    private ManagementSessionClaims publisher;
    private TemplateEntity template;
    private TemplateVersionEntity version;
    private MasterDocumentEntity master;
    private MasterRevisionLineEntity revision;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        publishFlow = new TemplateLifecycleApprovalFlowSupport(
                templateService,
                templateRepository,
                templateVersionRepository,
                publishGateService,
                decisionFormService,
                contentModuleReferenceService,
                collaborationWorkItemWriter,
                renderProfileService,
                apiPolicyMaterializationService,
                versionFidelityWarningService,
                messageResolver,
                transitions,
                decisionComments,
                eligibility,
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                objectMapper,
                new SelfApprovalGuard(),
                new TemplateAnnualReviewSupport(java.time.Clock.systemUTC())
        );
        publisher = new ManagementSessionClaims(
                "10000009", "Publisher", "pub@example.com",
                com.bank.docgen.authorization.management.domain.AuthSource.LOCAL,
                List.of("TEAM_LEAD"), List.of("RETAIL"),
                "route.template-authoring-home", List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
        template = new TemplateEntity(TEMPLATE_ID, "TPL-001", "RETAIL", "Sample", null, MASTER_ID, "10000009");
        template.setLifecycleStatus(TemplateLifecycleStatus.PENDING_RELEASE);
        version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000009");
        master = new MasterDocumentEntity(
                MASTER_ID, "RETAIL", "Retail Master", "desc",
                "masters/legacy.docx", "master.docx", "10000009"
        );
        master.setStatus(MasterDocumentStatus.APPROVED);
        // MasterDocumentEntity assigns a random currentRevisionLineId in its constructor;
        // overwrite it with the deterministic revision id under test.
        setField(master, "currentRevisionLineId", REVISION_ID);
        revision = new MasterRevisionLineEntity(
                REVISION_ID, MASTER_ID, "masters/R1.docx", "master.docx",
                1, MasterDocumentStatus.APPROVED, 1, true, "initial", "10000009"
        );
    }

    @Test
    void publish_pinsMasterRevisionIdHashAndMetadata_bddK01_001_002_004() throws Exception {
        byte[] docxBytes = new byte[]{10, 20, 30, 40, 50};
        String expectedHash = sha256Hex(docxBytes);

        stubPublishBasics();
        master.setStatus(MasterDocumentStatus.APPROVED);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_ID, MASTER_ID))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get("masters/R1.docx")).thenReturn(new ByteArrayInputStream(docxBytes));
        when(messageResolver.resolve(eq("api.audit.lifecycle.publishedRelease"), eq("1.0.0")))
                .thenReturn("Published release 1.0.0");

        publishFlow.publish(TEMPLATE_ID, new PublishTemplateRequest("1.0.0", true), publisher);

        assertThat(version.getMasterRevisionId()).isEqualTo(REVISION_ID);
        assertThat(version.getMasterFileHash()).isEqualTo(expectedHash);
        assertThat(version.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED);
        assertThat(version.getReleaseVersion()).isEqualTo("1.0.0");

        JsonNode meta = objectMapper.readTree(version.getPinMetadataJson());
        assertThat(meta.get("pinOrigin").asText()).isEqualTo("PUBLISHED");
        assertThat(meta.has("pinnedAt")).isTrue();
        assertThat(meta.get("pinnedBy").asText()).isEqualTo("10000009");
    }

    @Test
    void publish_renderProfileSnapshotRemains_bddK01_003() {
        stubPublishBasics();
        master.setStatus(MasterDocumentStatus.APPROVED);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_ID, MASTER_ID))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get("masters/R1.docx"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(messageResolver.resolve(any(), any())).thenReturn("ok");

        publishFlow.publish(TEMPLATE_ID, new PublishTemplateRequest("1.0.0", true), publisher);

        verify(renderProfileService).lockForPublish(version);
        assertThat(version.getRenderProfileJson()).isNull(); // lockForPublish is mocked; just assert it was invoked
    }

    @Test
    void publish_currentRevisionMissing_failClosed_bddK01_005_015() {
        stubPublishBasics();
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_ID, MASTER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> publishFlow.publish(
                TEMPLATE_ID, new PublishTemplateRequest("1.0.0", true), publisher))
                .isInstanceOf(MasterCurrentRevisionUnavailableException.class);

        assertThat(version.getMasterRevisionId()).isNull();
        assertThat(version.getMasterFileHash()).isNull();
        assertThat(version.getLifecycleStatus()).isNotEqualTo(TemplateLifecycleStatus.PUBLISHED);
        verify(templateVersionRepository, never()).save(any(TemplateVersionEntity.class));
    }

    @Test
    void publish_currentRevisionStorageMissing_failClosed_bddK01_005() {
        stubPublishBasics();
        master.setStatus(MasterDocumentStatus.APPROVED);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_ID, MASTER_ID))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get("masters/R1.docx")).thenThrow(new RuntimeException("404"));

        assertThatThrownBy(() -> publishFlow.publish(
                TEMPLATE_ID, new PublishTemplateRequest("1.0.0", true), publisher))
                .isInstanceOf(MasterCurrentRevisionUnavailableException.class);
        verify(templateVersionRepository, never()).save(any(TemplateVersionEntity.class));
    }

    @Test
    void publish_currentRevisionNotApproved_failClosed_bddK01_015() {
        stubPublishBasics();
        master.setStatus(MasterDocumentStatus.REJECTED);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_ID, MASTER_ID))
                .thenReturn(Optional.of(revision));

        assertThatThrownBy(() -> publishFlow.publish(
                TEMPLATE_ID, new PublishTemplateRequest("1.0.0", true), publisher))
                .isInstanceOf(MasterCurrentRevisionUnavailableException.class);
        assertThat(version.getMasterRevisionId()).isNull();
        verify(templateVersionRepository, never()).save(any(TemplateVersionEntity.class));
        verify(objectStoragePort, never()).get(any());
    }

    private void stubPublishBasics() {
        when(eligibility.requirePublishableTemplate(TEMPLATE_ID, publisher)).thenReturn(template);
        // Called only after successful pinning — lenient for fail-closed pin tests.
        org.mockito.Mockito.lenient().when(eligibility.requireReleaseCandidateVersion(TEMPLATE_ID))
                .thenReturn(version);
        // Pinning runs before post-pin side effects; use lenient so fail-closed tests that
        // abort inside resolvePinnedMaster do not trip UnnecessaryStubbingException.
        org.mockito.Mockito.lenient().doAnswer(inv -> null).when(apiPolicyMaterializationService)
                .ensureApiPolicyOnPublish(TEMPLATE_ID, "1.0.0", publisher.username());
        org.mockito.Mockito.lenient().doAnswer(inv -> null).when(renderProfileService).lockForPublish(version);
        org.mockito.Mockito.lenient().doAnswer(inv -> null).when(versionFidelityWarningService)
                .snapshotOnPublish(version, MASTER_ID);
        org.mockito.Mockito.lenient().doAnswer(inv -> null).when(contentModuleReferenceService)
                .lockReferencesForPublish(version.getId());
        org.mockito.Mockito.lenient().when(templateService.toDetail(template)).thenReturn(new TemplateDetailView(
                TEMPLATE_ID.toString(), null, "RETAIL", "Sample", null,
                MASTER_ID.toString(), TemplateLifecycleStatus.PUBLISHED, null,
                "1.0.0", null, 1, List.of(), List.of(), List.of(),
                Instant.now(), Instant.now(), publisher.username(), null, true, null,
                null));
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
