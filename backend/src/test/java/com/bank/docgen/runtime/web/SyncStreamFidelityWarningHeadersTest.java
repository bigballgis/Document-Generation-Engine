package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.service.InvocationRecordService;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * BDD-CE-C03-004/005: SYNC_STREAM returns header summary only; body is the file bytes.
 */
@ExtendWith(MockitoExtension.class)
class SyncStreamFidelityWarningHeadersTest {

    @Mock
    private InvocationRecordService invocationRecordService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private RuntimeGenerationAuditRecorder auditRecorder;

    private RuntimeTemplateSyncSupport support;

    @BeforeEach
    void setUp() {
        support = new RuntimeTemplateSyncSupport(
                invocationRecordService,
                apiPolicyRepository,
                new TraceIdProvider(),
                auditRecorder
        );
    }

    @Test
    void bddCeC03_004_syncStreamExposesHeaderSummaryOnly() throws Exception {
        byte[] artifact = "DOCX-BYTES".getBytes(StandardCharsets.UTF_8);
        SyncGenerateResult result = new SyncGenerateResult(
                artifact,
                null,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOC-1",
                "1.0.0",
                List.of(FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name()),
                "IDEMPOTENCY_NEW"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Trace-Id")).thenReturn("TRACE-1");

        GenerateRequestBody body = new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                null,
                null,
                "req-1",
                "idem-1",
                null
        );

        support.writeSyncResponse(request, response, "TPL-1", "EXPLICIT_VERSION", body, result, "INV-1");

        assertThat(response.getHeader("fidelityWarningCount")).isEqualTo("1");
        assertThat(response.getHeader("fidelityWarningCodes"))
                .isEqualTo(FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name());
        assertThat(response.getContentAsByteArray()).isEqualTo(artifact);
        String bodyText = response.getContentAsString();
        assertThat(bodyText).doesNotContain("warningCode");
        assertThat(bodyText).doesNotContain("fidelityWarnings");
        assertThat(bodyText).doesNotContain("sensitiveDataExcluded");
    }

    @Test
    void bddCeC03_005_syncStreamZeroWarningsClearHeaders() throws Exception {
        SyncGenerateResult result = new SyncGenerateResult(
                new byte[]{1, 2, 3},
                null,
                "application/pdf",
                "DOC-2",
                "1.0.0",
                List.of(),
                "IDEMPOTENCY_NEW"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        GenerateRequestBody body = new GenerateRequestBody(
                new OutputOptionsView("PDF", "SYNC_STREAM"),
                null,
                null,
                "req-2",
                "idem-2",
                null
        );

        support.writeSyncResponse(request, response, "TPL-1", "DEFAULT_ROUTE", body, result, null);

        assertThat(response.getHeader("fidelityWarningCount")).isEqualTo("0");
        assertThat(response.getHeader("fidelityWarningCodes")).isEmpty();
    }
}
