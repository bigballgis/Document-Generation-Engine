package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ManagementInvocationSummaryView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ManagementInvocationQueryServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiInvocationRecordRepository invocationRecordRepository;

    private ManagementInvocationQueryService service;
    private UUID templateId;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new ManagementInvocationQueryService(
                templateService,
                apiPolicyRepository,
                invocationRecordRepository
        );
        templateId = UUID.randomUUID();
        session = new ManagementSessionClaims(
                "10000002",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void listRecentInvocations_returnsSummaryWithoutParameters() {
        TemplateEntity template = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        when(templateService.requireReadableTemplate(templateId, session)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000001")));

        Instant now = Instant.now();
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                "INV-TEST001",
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                templateId,
                "TPL-001",
                UUID.randomUUID(),
                "svc-account-prod",
                "req-1",
                "idem-1",
                "EXPLICIT_VERSION",
                "1.0.0",
                "1.0.0",
                "DOCX",
                "SYNC_STREAM",
                "SUCCESS",
                120L,
                "{\"variables\":{}}",
                null,
                null,
                false,
                now.plusSeconds(3600),
                now.plusSeconds(7200),
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                now,
                now
        );
        when(invocationRecordRepository.findByTemplateIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                eq(templateId),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(entity)));

        List<ManagementInvocationSummaryView> result = service.listRecentInvocations(templateId, 10, session);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().invocationId()).isEqualTo("INV-TEST001");
        assertThat(result.getFirst().accessAccountSummary()).isEqualTo("svc***");
    }

    @Test
    void maskAccessAccount_masksLongValues() {
        assertThat(ManagementInvocationQueryService.maskAccessAccount("svc-account")).isEqualTo("svc***");
    }
}
