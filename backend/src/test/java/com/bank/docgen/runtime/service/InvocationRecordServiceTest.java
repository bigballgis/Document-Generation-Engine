package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.domain.ApiPolicyPlatformDefaults;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.domain.InvocationErrorEnvelope;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvocationRecordServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CREDENTIAL_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private ApiInvocationRecordRepository repository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private ReleaseBundleFingerprintSupport fingerprintSupport;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;

    @Captor
    private ArgumentCaptor<ApiInvocationRecordEntity> savedRecords;

    private InvocationRecordService service;
    private TemplateEntity template;
    private ApiPolicyEntity policy;
    private RuntimeSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new InvocationRecordService(
                repository,
                new InvocationParameterSanitizer(
                        new ObjectMapper(),
                        templateVersionRepository,
                        variableSchemaRepository,
                        org.mockito.Mockito.mock(
                                com.bank.docgen.template.service.CompositionInclusionRuleService.class
                        ),
                        org.mockito.Mockito.mock(
                                com.bank.docgen.template.service.TemplateContentModuleReferenceService.class
                        )
                ),
                idempotencyService,
                fingerprintSupport
        );
        lenient().when(fingerprintSupport.resolve(any(), any())).thenReturn(Optional.empty());
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "U0000001");
        version.setReleaseVersion("1.0.0");
        lenient().when(templateVersionRepository.findByTemplateIdAndReleaseVersion(eq(TEMPLATE_ID), any()))
                .thenReturn(Optional.of(version));
        lenient().when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(new VariableSchemaEntity(
                        UUID.randomUUID(),
                        VERSION_ID,
                        "name",
                        VariableType.TEXT,
                        false,
                        null,
                        null,
                        null,
                        null,
                        VariablePiiCategory.NONE
                )));
        template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                "GRP",
                "Demo",
                null,
                null,
                "U0000001"
        );
        policy = ApiPolicyEntity.createSkeleton(TEMPLATE_ID, "U0000001");
        session = new RuntimeSessionClaims(
                CREDENTIAL_ID,
                "CRED-001",
                TEMPLATE_ID,
                "TPL-001",
                "svc-account",
                List.of("grp-a")
        );
    }

    @Test
    void findExistingInvocationIdReturnsLiveRootRecord() {
        ApiInvocationRecordEntity existing = rootRecord("INV-EXIST01", InvocationKind.SINGLE);
        when(repository.findFirstByIdempotencyKeyAndTemplateIdAndCredentialIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                eq("idem-1"),
                eq(TEMPLATE_ID),
                eq(CREDENTIAL_ID),
                eq(List.of(InvocationKind.SINGLE, InvocationKind.BATCH_ROOT, InvocationKind.ASYNC_TASK)),
                any(Instant.class)
        )).thenReturn(Optional.of(existing));

        Optional<String> invocationId = service.findExistingInvocationId(TEMPLATE_ID, CREDENTIAL_ID, "idem-1");

        assertThat(invocationId).contains("INV-EXIST01");
    }

    @Test
    void recordSingleSync_persistsSanitizedParametersAndRetentionTtls() {
        when(idempotencyService.findLiveRecord("idem-1", TEMPLATE_ID))
                .thenReturn(Optional.of(new GenerationIdempotencyEntity(
                        UUID.randomUUID(),
                        "idem-1",
                        TEMPLATE_ID,
                        "hash",
                        "COMPLETED",
                        Instant.now().plusSeconds(3600)
                )));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("name", "Bob"),
                new EncryptionOptionsView(true, "open-secret", "owner-secret", List.of("PRINT")),
                "req-1",
                "idem-1",
                null
        );

        String invocationId = service.recordSingleSync(
                template,
                policy,
                session,
                "dev",
                "EXPLICIT_VERSION",
                "1.0.0",
                "1.0.0",
                request,
                "DOC-1",
                "storage/doc-1.docx",
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                "audit-1"
        );

        verify(repository).save(savedRecords.capture());
        ApiInvocationRecordEntity saved = savedRecords.getValue();
        assertThat(invocationId).startsWith("INV-");
        assertThat(saved.getInvocationKind()).isEqualTo(InvocationKind.SINGLE);
        assertThat(saved.getStatus()).isEqualTo(InvocationStatus.SUCCEEDED);
        assertThat(saved.getParametersStorage()).doesNotContain("open-secret");
        assertThat(saved.getParametersStorage()).contains("openPasswordProvided");
        assertThat(saved.isArtifactSaved()).isTrue();
        assertThat(saved.getDocumentExpiresAt()).isNotNull();
        assertThat(saved.getRecordExpiresAt()).isAfter(Instant.now().plus(
                ApiPolicyPlatformDefaults.INVOCATION_RECORD_RETENTION_DAYS - 1L,
                java.time.temporal.ChronoUnit.DAYS
        ));
    }

    @Test
    void recordSingleSync_persistsReleaseBundleFingerprintWhenResolved() {
        UUID snapshotId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        String hash = "d".repeat(64);
        when(idempotencyService.findLiveRecord("idem-fp", TEMPLATE_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fingerprintSupport.resolve(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(new ReleaseBundleFingerprint(snapshotId, hash)));

        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("PDF", "SYNC_STREAM"),
                Map.of("name", "Bob"),
                null,
                "req-fp",
                "idem-fp",
                null
        );

        service.recordSingleSync(
                template,
                policy,
                session,
                "dev",
                "EXPLICIT_VERSION",
                "1.0.0",
                "1.0.0",
                request,
                "DOC-FP",
                "storage/doc-fp.pdf",
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                "audit-fp"
        );

        verify(repository).save(savedRecords.capture());
        ApiInvocationRecordEntity saved = savedRecords.getValue();
        assertThat(saved.getReleaseBundleSnapshotId()).isEqualTo(snapshotId);
        assertThat(saved.getReleaseBundleHash()).isEqualTo(hash);
        assertThat(saved.getParametersStorage()).contains("\"name\"");
        assertThat(saved.getParametersStorage()).contains("Bob");
    }

    @Test
    void recordSingleSync_persistsErrorEnvelopeOnFailure() {
        when(idempotencyService.findLiveRecord("idem-fail", TEMPLATE_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateRequestBody request = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("name", "Bob"),
                null,
                "req-fail",
                "idem-fail",
                null
        );

        String invocationId = service.recordSingleSync(
                template,
                policy,
                session,
                "dev",
                "EXPLICIT_VERSION",
                "1.2.0",
                "1.2.0",
                request,
                null,
                null,
                RuntimeGenerationAuditRecorder.OUTCOME_FAILURE,
                "audit-fail",
                new InvocationErrorEnvelope(
                        "REQUEST_BODY_INVALID",
                        "RUNTIME",
                        "api.error.validation.requestBodyInvalid",
                        false,
                        "Request body is invalid."
                )
        );

        verify(repository).save(savedRecords.capture());
        ApiInvocationRecordEntity saved = savedRecords.getValue();
        assertThat(invocationId).startsWith("INV-");
        assertThat(saved.getStatus()).isEqualTo(InvocationStatus.FAILED);
        assertThat(saved.getErrorCode()).isEqualTo("REQUEST_BODY_INVALID");
        assertThat(saved.getErrorCategory()).isEqualTo("RUNTIME");
        assertThat(saved.getErrorMessageKey()).isEqualTo("api.error.validation.requestBodyInvalid");
        assertThat(saved.getErrorRetryable()).isFalse();
        assertThat(saved.getErrorMessage()).isEqualTo("Request body is invalid.");
    }

    @Test
    void recordBatchSync_persistsRootAndItemRows() {
        when(idempotencyService.findLiveRecord("idem-batch", TEMPLATE_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BatchGenerateRequestBody request = batchRequest();
        BatchResultView batchResult = new BatchResultView(
                "BATCH-001",
                new BatchSummaryView(2, 2, 2, 0, 0),
                List.of(
                        batchItem("item-a", "DOC-A"),
                        batchItem("item-b", "DOC-B")
                )
        );

        String rootId = service.recordBatchSync(
                template,
                policy,
                session,
                "dev",
                "DEFAULT_ROUTE",
                null,
                "1.0.0",
                request,
                batchResult,
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                "audit-batch"
        );

        verify(repository, org.mockito.Mockito.times(3)).save(savedRecords.capture());
        List<ApiInvocationRecordEntity> saved = savedRecords.getAllValues();
        assertThat(rootId).startsWith("INV-");
        assertThat(saved.stream().filter(record -> record.getInvocationKind() == InvocationKind.BATCH_ROOT).count())
                .isEqualTo(1);
        assertThat(saved.stream().filter(record -> record.getInvocationKind() == InvocationKind.BATCH_ITEM).count())
                .isEqualTo(2);
        assertThat(saved.stream().filter(record -> record.getInvocationKind() == InvocationKind.BATCH_ROOT).findFirst())
                .get()
                .extracting(ApiInvocationRecordEntity::getBatchExternalId)
                .isEqualTo("BATCH-001");
    }

    @Test
    void recordAsyncAccepted_persistsAsyncTaskRow() {
        BatchGenerateRequestBody asyncRequest = new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "ASYNC_TASK"),
                List.of(new BatchGenerateRequestBody.BatchGenerateItemBody(
                        "item-a",
                        Map.of("x", 1),
                        null,
                        null
                )),
                null,
                "req-async",
                "idem-async",
                null,
                null
        );
        when(idempotencyService.findLiveRecord("idem-async", TEMPLATE_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String invocationId = service.recordAsyncAccepted(
                template,
                policy,
                session,
                "dev",
                "EXPLICIT_VERSION",
                "1.0.0",
                "1.0.0",
                asyncRequest,
                "TASK-001",
                "BATCH-ASYNC",
                "audit-async"
        );

        verify(repository).save(savedRecords.capture());
        ApiInvocationRecordEntity saved = savedRecords.getValue();
        assertThat(invocationId).startsWith("INV-");
        assertThat(saved.getInvocationKind()).isEqualTo(InvocationKind.ASYNC_TASK);
        assertThat(saved.getStatus()).isEqualTo(InvocationStatus.ACCEPTED);
        assertThat(saved.getTaskExternalId()).isEqualTo("TASK-001");
    }

    @Test
    void findExistingInvocationId_doesNotPersistNewRow() {
        when(repository.findFirstByIdempotencyKeyAndTemplateIdAndCredentialIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(Optional.empty());

        service.findExistingInvocationId(TEMPLATE_ID, CREDENTIAL_ID, "idem-missing");

        verify(repository, never()).save(any());
    }

    private ApiInvocationRecordEntity rootRecord(String externalId, InvocationKind kind) {
        Instant now = Instant.now();
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                externalId,
                kind,
                InvocationStatus.SUCCEEDED,
                "dev",
                TEMPLATE_ID,
                "TPL-001",
                CREDENTIAL_ID,
                "svc-account",
                "req-1",
                "idem-1",
                "DEFAULT_ROUTE",
                null,
                "1.0.0",
                "DOCX",
                "SYNC_STREAM",
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                null,
                "{}",
                "DOC-1",
                null,
                true,
                now.plusSeconds(3600),
                now.plusSeconds(1800),
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                false,
                now,
                now
        );
    }

    private BatchGenerateRequestBody batchRequest() {
        return new BatchGenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                List.of(
                        new BatchGenerateRequestBody.BatchGenerateItemBody(
                                "item-a",
                                Map.of("x", 1),
                                null,
                                null
                        ),
                        new BatchGenerateRequestBody.BatchGenerateItemBody(
                                "item-b",
                                Map.of("x", 2),
                                null,
                                null
                        )
                ),
                null,
                "req-batch",
                "idem-batch",
                null,
                null
        );
    }

    private BatchResultItemView batchItem(String itemId, String documentId) {
        return new BatchResultItemView(
                itemId,
                "SUCCEEDED",
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                EncryptionSummaryView.disabled("DOCX"),
                documentId,
                List.of()
        );
    }
}
