package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuntimeGenerationServiceGenerateTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String RELEASE_VERSION = "1.0.0";

    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private EncryptionParameterValidator encryptionParameterValidator;
    @Mock
    private DocumentGenerationEngine documentGenerationEngine;

    private RuntimeGenerationService service;
    private TemplateEntity template;
    private ApiPolicyEntity policy;
    private RuntimeSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new RuntimeGenerationService(
                templateVersionRepository,
                apiPolicyRepository,
                mock(com.bank.docgen.apimgmt.persistence.ApiCredentialRepository.class),
                objectStoragePort,
                idempotencyService,
                encryptionParameterValidator,
                mock(ContractAssemblyService.class),
                documentGenerationEngine,
                new ObjectMapper()
        );
        template = publishedTemplate(TEMPLATE_ID);
        policy = policyForTemplate(TEMPLATE_ID);
        session = new RuntimeSessionClaims(
                UUID.randomUUID(),
                "CRED-1",
                TEMPLATE_ID,
                "TPL-001",
                "svc-caller",
                List.of("grp-a")
        );
    }

    @Test
    void generateSync_replaysCachedArtifactWithoutRendering() throws Exception {
        GenerationIdempotencyEntity existing = completedIdempotency(TEMPLATE_ID);
        TemplateVersionEntity version = publishedVersion(VERSION_ID, TEMPLATE_ID);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, RELEASE_VERSION))
                .thenReturn(Optional.of(version));
        doNothing().when(encryptionParameterValidator).validate(any(), any(), anyString());
        when(idempotencyService.hashRequest(anyString())).thenReturn("hash-a");
        when(idempotencyService.findExisting("idem-1", TEMPLATE_ID, "hash-a")).thenReturn(Optional.of(existing));
        when(objectStoragePort.get("storage/replay.docx")).thenReturn(new ByteArrayInputStream(new byte[]{9, 8, 7}));

        SyncGenerateResult result = service.generateSync(
                template,
                session,
                RELEASE_VERSION,
                generateRequest("idem-1", "DOCX")
        );

        assertThat(result.idempotencyStatus()).isEqualTo(IdempotencyConstants.STATUS_REPLAYED);
        assertThat(result.documentId()).isEqualTo("DOC-REPLAY");
        assertThat(result.artifactBytes()).isNull();
        assertThat(result.artifactStream()).isNotNull();
        assertThat(result.artifactStream().readAllBytes()).containsExactly(9, 8, 7);
        verify(documentGenerationEngine, never()).generate(any(), anyString(), any(), anyString(), any());
    }

    @Test
    void generateSync_replayReturnsStorageStreamWithoutEagerLoad() throws Exception {
        GenerationIdempotencyEntity existing = completedIdempotency(TEMPLATE_ID);
        TemplateVersionEntity version = publishedVersion(VERSION_ID, TEMPLATE_ID);
        TrackingInputStream stream = new TrackingInputStream();
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, RELEASE_VERSION))
                .thenReturn(Optional.of(version));
        doNothing().when(encryptionParameterValidator).validate(any(), any(), anyString());
        when(idempotencyService.hashRequest(anyString())).thenReturn("hash-a");
        when(idempotencyService.findExisting("idem-replay-stream", TEMPLATE_ID, "hash-a"))
                .thenReturn(Optional.of(existing));
        when(objectStoragePort.get("storage/replay.docx")).thenReturn(stream);

        SyncGenerateResult result = service.generateSync(
                template,
                session,
                RELEASE_VERSION,
                generateRequest("idem-replay-stream", "DOCX")
        );

        assertThat(result.artifactStream()).isSameAs(stream);
        assertThat(stream.readCount()).isZero();
        assertThat(result.artifactBytes()).isNull();
    }

    @Test
    void generateSync_createsArtifactAndCompletesIdempotency() throws Exception {
        TemplateVersionEntity version = publishedVersion(VERSION_ID, TEMPLATE_ID);
        GenerationIdempotencyEntity pending = pendingIdempotency(TEMPLATE_ID);
        byte[] finalBytes = new byte[]{4, 5, 6};
        DocumentGenerationEngine.GeneratedDocument generated = new DocumentGenerationEngine.GeneratedDocument(
                "DOC-NEW",
                "generated/DOC-NEW/output.docx",
                finalBytes,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX",
                List.of(FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name())
        );

        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(idempotencyService.hashRequest(anyString())).thenReturn("hash-a");
        when(idempotencyService.findExisting("idem-2", TEMPLATE_ID, "hash-a")).thenReturn(Optional.empty());
        when(idempotencyService.begin("idem-2", TEMPLATE_ID, "hash-a")).thenReturn(pending);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, RELEASE_VERSION))
                .thenReturn(Optional.of(version));
        when(documentGenerationEngine.generate(
                eq(template),
                eq(RELEASE_VERSION),
                any(),
                eq("DOCX"),
                any()
        )).thenReturn(generated);
        doNothing().when(encryptionParameterValidator).validate(any(), any(), anyString());

        SyncGenerateResult result = service.generateSync(
                template,
                session,
                RELEASE_VERSION,
                generateRequest("idem-2", "DOCX")
        );

        assertThat(result.idempotencyStatus()).isEqualTo(IdempotencyConstants.STATUS_NEW);
        assertThat(result.artifactBytes()).containsExactly(4, 5, 6);
        assertThat(result.artifactStream()).isNull();
        assertThat(result.resolvedReleaseVersion()).isEqualTo(RELEASE_VERSION);
        verify(idempotencyService).complete(pending, "generated/DOC-NEW/output.docx", "DOC-NEW");
    }

    @Test
    void generateSync_rejectsOutputFormatNotAllowedByPolicy() {
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> service.generateSync(
                template,
                session,
                RELEASE_VERSION,
                generateRequest("idem-3", "PDF")
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.runtime.outputFormatUnsupported");
    }

    @Test
    void generateSync_rejectsTemplateCredentialMismatch() {
        RuntimeSessionClaims otherTemplateSession = new RuntimeSessionClaims(
                UUID.randomUUID(),
                "CRED-1",
                UUID.randomUUID(),
                "TPL-OTHER",
                "svc-caller",
                List.of("grp-a")
        );

        assertThatThrownBy(() -> service.generateSync(
                template,
                otherTemplateSession,
                RELEASE_VERSION,
                generateRequest("idem-4", "DOCX")
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.runtime.templateCredentialMismatch");
    }

    @Test
    void generateSync_rejectsNonCallableReleaseVersion() {
        TemplateVersionEntity draftVersion = publishedVersion(VERSION_ID, TEMPLATE_ID);
        draftVersion.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);

        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, RELEASE_VERSION))
                .thenReturn(Optional.of(draftVersion));
        doNothing().when(encryptionParameterValidator).validate(any(), any(), anyString());

        assertThatThrownBy(() -> service.generateSync(
                template,
                session,
                RELEASE_VERSION,
                generateRequest("idem-5", "DOCX")
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.runtime.versionNotCallable"        );
    }

    @Test
    void generateSync_rejectsSyncDownloadUrlMode() {
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));

        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_DOWNLOAD_URL"),
                Map.of("customerName", "Alice"),
                new EncryptionOptionsView(false, null, null, List.of()),
                "req-1",
                "idem-download-url"
        );

        assertThatThrownBy(() -> service.generateSync(template, session, RELEASE_VERSION, request))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.runtime.outputModeUnsupported");
    }

    @Test
    void generateSync_hashesEncryptionInIdempotencyPayload() {
        TemplateVersionEntity version = publishedVersion(VERSION_ID, TEMPLATE_ID);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, RELEASE_VERSION))
                .thenReturn(Optional.of(version));
        doNothing().when(encryptionParameterValidator).validate(any(), any(), anyString());
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        when(idempotencyService.hashRequest(payloadCaptor.capture())).thenReturn("hash-a");
        when(idempotencyService.findExisting(any(), any(), any())).thenReturn(Optional.empty());
        when(idempotencyService.begin(any(), any(), any())).thenReturn(pendingIdempotency(TEMPLATE_ID));
        when(documentGenerationEngine.generate(any(), anyString(), any(), anyString(), any()))
                .thenReturn(new DocumentGenerationEngine.GeneratedDocument(
                        "DOC-1",
                        "storage/doc.docx",
                        new byte[]{1},
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "DOCX",
                        List.of()
                ));

        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("customerName", "Alice"),
                new EncryptionOptionsView(true, "user", null, List.of("OPEN_PASSWORD")),
                "req-enc",
                "idem-enc"
        );

        service.generateSync(template, session, RELEASE_VERSION, request);

        assertThat(payloadCaptor.getValue()).contains("\"encryption\"");
    }

    private static final class TrackingInputStream extends InputStream {

        private int readCount;

        @Override
        public int read() throws IOException {
            readCount++;
            return -1;
        }

        int readCount() {
            return readCount;
        }
    }

    private GenerateRequestBody generateRequest(String idempotencyKey, String format) {
        return new GenerateRequestBody(
                new OutputOptionsView(format, "SYNC_STREAM"),
                Map.of("customerName", "Alice"),
                new EncryptionOptionsView(false, null, null, List.of()),
                "req-1",
                idempotencyKey
        );
    }

    private TemplateEntity publishedTemplate(UUID templateId) {
        TemplateEntity entity = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "10000001"
        );
        entity.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        entity.setReleaseVersion(RELEASE_VERSION);
        return entity;
    }

    private TemplateVersionEntity publishedVersion(UUID versionId, UUID templateId) {
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000001");
        version.setReleaseVersion(RELEASE_VERSION);
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        return version;
    }

    private ApiPolicyEntity policyForTemplate(UUID templateId) {
        ApiPolicyEntity entity = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[\"grp-a\"]", "10000001");
        entity.replaceConfiguration(
                "[\"grp-a\"]",
                RELEASE_VERSION,
                "[\"DOCX\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000001"
        );
        return entity;
    }

    private GenerationIdempotencyEntity completedIdempotency(UUID templateId) {
        GenerationIdempotencyEntity entity = new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                "idem-1",
                templateId,
                "hash-a",
                "COMPLETED",
                Instant.now().plusSeconds(3600)
        );
        entity.complete("storage/replay.docx", "DOC-REPLAY");
        return entity;
    }

    private GenerationIdempotencyEntity pendingIdempotency(UUID templateId) {
        return new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                "idem-2",
                templateId,
                "hash-a",
                "IN_PROGRESS",
                Instant.now().plusSeconds(3600)
        );
    }
}
