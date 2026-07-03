package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ManagementInvocationSummaryView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagementInvocationQueryService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

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
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        apiPolicyRepository.findByTemplateId(templateId).orElseThrow(ApiManagementNotFoundException::new);
        int resolvedLimit = normalizeLimit(limit);
        Instant now = Instant.now();
        return invocationRecordRepository
                .findByTemplateIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                        template.getId(),
                        LOGICAL_KINDS,
                        now,
                        PageRequest.of(0, resolvedLimit)
                )
                .getContent()
                .stream()
                .map(this::toSummary)
                .toList();
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

    static String maskAccessAccount(String accessAccount) {
        if (accessAccount == null || accessAccount.isBlank()) {
            return "—";
        }
        if (accessAccount.length() <= 3) {
            return accessAccount.charAt(0) + "***";
        }
        return accessAccount.substring(0, 3) + "***";
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
