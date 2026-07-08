package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ManagementInvocationAuditLinkHintView;
import com.bank.docgen.apimgmt.api.ManagementInvocationDetailView;
import com.bank.docgen.apimgmt.api.ManagementInvocationSummaryView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagementInvocationQueryService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<InvocationKind> LOGICAL_KINDS = EnumSet.of(
            InvocationKind.SINGLE,
            InvocationKind.BATCH_ROOT,
            InvocationKind.ASYNC_TASK
    );

    private final TemplateService templateService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiInvocationRecordRepository invocationRecordRepository;

    public ManagementInvocationQueryService(
            TemplateService templateService,
            ApiPolicyRepository apiPolicyRepository,
            ApiInvocationRecordRepository invocationRecordRepository
    ) {
        this.templateService = templateService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.invocationRecordRepository = invocationRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<ManagementInvocationSummaryView> listRecentInvocations(
            UUID templateId,
            int limit,
            ManagementSessionClaims session
    ) {
        PageView<ManagementInvocationSummaryView> page = listInvocations(
                templateId,
                session,
                0,
                normalizeLimit(limit),
                ManagementInvocationFilters.empty()
        );
        return page.content();
    }

    @Transactional(readOnly = true)
    public PageView<ManagementInvocationSummaryView> listInvocations(
            UUID templateId,
            ManagementSessionClaims session,
            int page,
            int size,
            ManagementInvocationFilters filters
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        apiPolicyRepository.findByTemplateId(templateId).orElseThrow(ApiManagementNotFoundException::new);
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = normalizePageSize(size);
        Instant now = Instant.now();
        InvocationKind invocationKind = parseInvocationKind(filters.invocationKind());
        AuditSearchPage<ApiInvocationRecordEntity> searchPage = invocationRecordRepository.searchManagementInvocations(
                template.getId(),
                LOGICAL_KINDS,
                now,
                filters.status(),
                invocationKind,
                filters.requestId(),
                filters.createdAfter(),
                filters.createdBefore(),
                filters.credentialId(),
                resolvedPage,
                resolvedSize
        );
        List<ManagementInvocationSummaryView> content = searchPage.content().stream()
                .map(this::toSummary)
                .toList();
        return new PageView<>(
                content,
                resolvedPage,
                resolvedSize,
                searchPage.totalElements(),
                searchPage.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public ManagementInvocationDetailView getInvocationDetail(
            UUID templateId,
            String invocationId,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        apiPolicyRepository.findByTemplateId(templateId).orElseThrow(ApiManagementNotFoundException::new);
        ApiInvocationRecordEntity entity = invocationRecordRepository.findByInvocationExternalId(invocationId)
                .orElseThrow(ApiManagementNotFoundException::new);
        if (!entity.getTemplateId().equals(templateId)) {
            throw new ApiManagementNotFoundException();
        }
        if (!entity.getRecordExpiresAt().isAfter(Instant.now())) {
            throw new ApiManagementNotFoundException();
        }
        return toDetail(entity);
    }

    private ManagementInvocationSummaryView toSummary(ApiInvocationRecordEntity entity) {
        return new ManagementInvocationSummaryView(
                entity.getInvocationExternalId(),
                entity.getInvocationKind().name(),
                entity.getStatus().name(),
                entity.getRequestId(),
                entity.getResolvedReleaseVersion(),
                entity.getRouteType(),
                entity.getCreatedAt(),
                maskAccessAccount(entity.getAccessAccount())
        );
    }

    private ManagementInvocationDetailView toDetail(ApiInvocationRecordEntity entity) {
        return new ManagementInvocationDetailView(
                entity.getInvocationExternalId(),
                entity.getRequestId(),
                entity.getRouteType(),
                entity.getResolvedReleaseVersion(),
                entity.getOutcome(),
                entity.getDurationMs(),
                maskAccessAccount(entity.getAccessAccount()),
                entity.getCredentialId(),
                entity.getBatchExternalId(),
                entity.getParentInvocationExternalId(),
                entity.getCreatedAt(),
                entity.getDocumentId() != null && !entity.getDocumentId().isBlank(),
                new ManagementInvocationAuditLinkHintView(entity.getRequestId(), entity.getAuditId())
        );
    }

    static String maskAccessAccount(String accessAccount) {
        if (accessAccount == null || accessAccount.isBlank()) {
            return "—";
        }
        if (accessAccount.length() <= 3) {
            return accessAccount.charAt(0) + "***";
        }
        return accessAccount.substring(0, 3) + "***";
    }

    private static InvocationKind parseInvocationKind(String invocationKind) {
        if (invocationKind == null || invocationKind.isBlank()) {
            return null;
        }
        return InvocationKind.valueOf(invocationKind.trim().toUpperCase(Locale.ROOT));
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
