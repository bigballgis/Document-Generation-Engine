package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.InvocationDetailResultView;
import com.bank.docgen.runtime.api.InvocationDetailView;
import com.bank.docgen.runtime.api.InvocationListResultView;
import com.bank.docgen.runtime.api.InvocationSummaryView;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.BatchGenerationService;
import com.bank.docgen.runtime.service.InvocationQueryService;
import com.bank.docgen.runtime.service.InvocationRecordService;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.runtime.service.RuntimeGenerationService;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RuntimeInvocationControllerTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private TemplateService templateService;
    @Mock
    private RuntimeGenerationService runtimeGenerationService;
    @Mock
    private BatchGenerationService batchGenerationService;
    @Mock
    private RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    @Mock
    private InvocationRecordService invocationRecordService;
    @Mock
    private InvocationQueryService invocationQueryService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private MessageResolver messageResolver;

    private RuntimeTemplateController controller;
    private TemplateEntity template;
    private RuntimeSessionClaims session;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new RuntimeTemplateController(
                templateService,
                runtimeGenerationService,
                batchGenerationService,
                new TraceIdProvider(),
                runtimeGenerationAuditRecorder,
                invocationRecordService,
                invocationQueryService,
                apiPolicyRepository,
                messageResolver
        );
        template = new TemplateEntity(TEMPLATE_ID, "TPL-001", "GRP", "Demo", null, null, "U0000001");
        session = new RuntimeSessionClaims(
                UUID.randomUUID(),
                "CRED-001",
                TEMPLATE_ID,
                "TPL-001",
                "svc-account",
                List.of("grp-a")
        );
        request = new MockHttpServletRequest("GET", "/api/dev/v1/templates/TPL-001/invocations");
    }

    @Test
    void listInvocations_delegatesToQueryService() {
        InvocationListResultView listResult = new InvocationListResultView(
                "logical",
                List.of(new InvocationSummaryView(
                        "INV-001",
                        "SINGLE",
                        "TPL-001",
                        "1.0.0",
                        "DEFAULT_ROUTE",
                        "SUCCEEDED",
                        "req-1",
                        "idem-1",
                        null,
                        null,
                        null,
                        null,
                        true,
                        "DOC-1",
                        Instant.now().plusSeconds(1800),
                        Instant.now().plusSeconds(3600),
                        Instant.now(),
                        null
                )),
                0,
                20,
                1
        );
        when(templateService.requireTemplateByExternalId("TPL-001")).thenReturn(template);
        when(invocationQueryService.listInvocations(template, session, "logical", "req-trace", 0, 20))
                .thenReturn(listResult);

        SuccessEnvelope<InvocationListResultView> response = controller.listInvocations(
                "dev",
                "TPL-001",
                "logical",
                "req-trace",
                0,
                20,
                session,
                request
        );

        verify(invocationQueryService).listInvocations(eq(template), eq(session), eq("logical"), eq("req-trace"), eq(0), eq(20));
        assertThat(response.result().totalElements()).isEqualTo(1);
        assertThat(response.result().items().getFirst().invocationId()).isEqualTo("INV-001");
    }

    @Test
    void getInvocation_delegatesToQueryService() {
        InvocationDetailView detail = new InvocationDetailView(
                new InvocationSummaryView(
                        "INV-DETAIL",
                        "SINGLE",
                        "TPL-001",
                        "1.0.0",
                        "DEFAULT_ROUTE",
                        "SUCCEEDED",
                        "req-1",
                        "idem-1",
                        null,
                        null,
                        null,
                        null,
                        true,
                        "DOC-1",
                        Instant.now().plusSeconds(1800),
                        Instant.now().plusSeconds(3600),
                        Instant.now(),
                        null
                ),
                java.util.Map.of("variables", java.util.Map.of("name", "Alice")),
                List.of()
        );
        when(templateService.requireTemplateByExternalId("TPL-001")).thenReturn(template);
        when(invocationQueryService.getInvocationDetail(template, session, "INV-DETAIL")).thenReturn(detail);

        SuccessEnvelope<InvocationDetailResultView> response = controller.getInvocation(
                "dev",
                "TPL-001",
                "INV-DETAIL",
                session,
                request
        );

        verify(invocationQueryService).getInvocationDetail(template, session, "INV-DETAIL");
        assertThat(response.result().invocation().parameters()).containsKey("variables");
    }
}
