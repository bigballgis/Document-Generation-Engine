package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.domain.InvocationErrorEnvelope;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.InvocationRecordService;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateValidationException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-U11-IRC-006: runtime contract failures persist a unified error envelope on the
 * invocation record so management detail/drawer can troubleshoot.
 */
@ExtendWith(MockitoExtension.class)
class RuntimeTemplateSyncSupportFailedInvocationTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CREDENTIAL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private InvocationRecordService invocationRecordService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private RuntimeGenerationAuditRecorder auditRecorder;
    @Mock
    private MessageResolver messageResolver;
    @Mock
    private TemplateEntity template;
    @Mock
    private ApiPolicyEntity policy;

    private RuntimeTemplateSyncSupport support;

    @BeforeEach
    void setUp() {
        support = new RuntimeTemplateSyncSupport(
                invocationRecordService,
                apiPolicyRepository,
                new TraceIdProvider(),
                auditRecorder,
                messageResolver
        );
        lenient().when(template.getId()).thenReturn(TEMPLATE_ID);
    }

    @Test
    void recordFailedSingleInvocation_persistsPlatformErrorEnvelope() {
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(messageResolver.resolve("api.error.validation.requestBodyInvalid"))
                .thenReturn("Request body is invalid.");
        when(invocationRecordService.recordSingleSync(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn("INV-FAIL-1");

        RuntimeSessionClaims session = new RuntimeSessionClaims(
                CREDENTIAL_ID,
                "cred-ext",
                TEMPLATE_ID,
                "TPL-001",
                "svc-account",
                java.util.List.of()
        );
        GenerateRequestBody body = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                java.util.Map.of("name", "x"),
                null,
                "req-contract-fail",
                "idem-contract-fail",
                null
        );
        TemplateValidationException failure = new TemplateValidationException(
                "api.error.validation.requestBodyInvalid"
        );

        String invocationId = support.recordFailedSingleInvocation(
                template,
                session,
                "dev",
                "EXPLICIT_VERSION",
                "1.2.0",
                body,
                failure
        );

        assertThat(invocationId).isEqualTo("INV-FAIL-1");
        ArgumentCaptor<InvocationErrorEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(InvocationErrorEnvelope.class);
        verify(invocationRecordService).recordSingleSync(
                eq(template),
                eq(policy),
                eq(session),
                eq("dev"),
                eq("EXPLICIT_VERSION"),
                eq("1.2.0"),
                eq("1.2.0"),
                eq(body),
                isNull(),
                isNull(),
                eq(RuntimeGenerationAuditRecorder.OUTCOME_FAILURE),
                any(),
                envelopeCaptor.capture()
        );
        InvocationErrorEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.code()).isEqualTo(ApiErrorCodes.REQUEST_BODY_INVALID);
        assertThat(envelope.category()).isEqualTo(ApiErrorCategories.VALIDATION);
        assertThat(envelope.messageKey()).isEqualTo("api.error.validation.requestBodyInvalid");
        assertThat(envelope.retryable()).isFalse();
        assertThat(envelope.message()).isEqualTo("Request body is invalid.");
    }

    @Test
    void mapException_requestBodyInvalidUsesValidationEnvelope() {
        when(messageResolver.resolve("api.error.validation.requestBodyInvalid"))
                .thenReturn("Request body is invalid.");

        InvocationErrorEnvelope envelope = FailedSyncInvocationErrorMapper.from(
                new TemplateValidationException("api.error.validation.requestBodyInvalid"),
                messageResolver
        );

        assertThat(envelope).isNotNull();
        assertThat(envelope.code()).isEqualTo(ApiErrorCodes.REQUEST_BODY_INVALID);
        assertThat(envelope.category()).isEqualTo(ApiErrorCategories.VALIDATION);
    }
}
