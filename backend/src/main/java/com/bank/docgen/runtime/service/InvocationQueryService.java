package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.InvocationDetailView;
import com.bank.docgen.runtime.api.InvocationListResultView;
import com.bank.docgen.runtime.api.InvocationSummaryView;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationListView;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvocationQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<InvocationKind> LOGICAL_KINDS = EnumSet.of(
            InvocationKind.SINGLE,
            InvocationKind.BATCH_ROOT,
            InvocationKind.ASYNC_TASK
    );

    private static final Set<InvocationKind> FLAT_KINDS = EnumSet.of(
            InvocationKind.SINGLE,
            InvocationKind.BATCH_ITEM
    );

    private final ApiInvocationRecordRepository repository;
    private final ObjectMapper objectMapper;

    public InvocationQueryService(
            ApiInvocationRecordRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public InvocationListResultView listInvocations(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String view,
            String requestId,
            int page,
            int size
    ) {
        assertTemplateAccess(template, session);
        InvocationListView listView = InvocationListView.parse(view);
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = normalizeSize(size);
        Instant now = Instant.now();
        Set<InvocationKind> kinds = listView == InvocationListView.LOGICAL ? LOGICAL_KINDS : FLAT_KINDS;
        Page<ApiInvocationRecordEntity> pageResult = queryPage(
                template.getId(),
                session.credentialId(),
                kinds,
                requestId,
                now,
                PageRequest.of(resolvedPage, resolvedSize)
        );
        List<InvocationSummaryView> items = pageResult.getContent().stream()
                .map(entity -> toSummary(entity, listView == InvocationListView.LOGICAL, session.credentialId()))
                .toList();
        return new InvocationListResultView(
                listView.name().toLowerCase(Locale.ROOT),
                items,
                resolvedPage,
                resolvedSize,
                pageResult.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public InvocationDetailView getInvocationDetail(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String invocationId
    ) {
        assertTemplateAccess(template, session);
        ApiInvocationRecordEntity entity = repository.findByInvocationExternalId(invocationId)
                .orElseThrow(InvocationNotFoundException::new);
        if (!entity.getTemplateId().equals(template.getId())) {
            throw new InvocationNotFoundException();
        }
        if (!entity.getCredentialId().equals(session.credentialId())) {
            throw new RuntimeAccessDeniedException();
        }
        if (!entity.getRecordExpiresAt().isAfter(Instant.now())) {
            throw new InvocationRecordExpiredException();
        }
        boolean includeChildCount = entity.getInvocationKind() == InvocationKind.BATCH_ROOT;
        InvocationSummaryView summary = toSummary(entity, includeChildCount, session.credentialId());
        List<InvocationSummaryView> childItems = List.of();
        if (entity.getInvocationKind() == InvocationKind.BATCH_ROOT && entity.getBatchExternalId() != null) {
            childItems = repository.findByBatchExternalIdAndInvocationKindAndCredentialId(
                            entity.getBatchExternalId(),
                            InvocationKind.BATCH_ITEM,
                            session.credentialId()
                    ).stream()
                    .filter(item -> item.getRecordExpiresAt().isAfter(Instant.now()))
                    .map(item -> toSummary(item, false, session.credentialId()))
                    .toList();
        }
        return new InvocationDetailView(summary, readParameters(entity.getParametersStorage()), childItems);
    }

    private Page<ApiInvocationRecordEntity> queryPage(
            UUID templateId,
            UUID credentialId,
            Set<InvocationKind> kinds,
            String requestId,
            Instant now,
            PageRequest pageable
    ) {
        if (requestId != null && !requestId.isBlank()) {
            return repository.findByTemplateIdAndCredentialIdAndInvocationKindInAndRequestIdAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                    templateId,
                    credentialId,
                    kinds,
                    requestId,
                    now,
                    pageable
            );
        }
        return repository.findByTemplateIdAndCredentialIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                templateId,
                credentialId,
                kinds,
                now,
                pageable
        );
    }

    private InvocationSummaryView toSummary(
            ApiInvocationRecordEntity entity,
            boolean includeChildCount,
            UUID credentialId
    ) {
        Integer childItemCount = null;
        if (includeChildCount && entity.getInvocationKind() == InvocationKind.BATCH_ROOT
                && entity.getBatchExternalId() != null) {
            childItemCount = Math.toIntExact(repository.countByBatchExternalIdAndInvocationKindAndCredentialId(
                    entity.getBatchExternalId(),
                    InvocationKind.BATCH_ITEM,
                    credentialId
            ));
        }
        return new InvocationSummaryView(
                entity.getInvocationExternalId(),
                entity.getInvocationKind().name(),
                entity.getTemplateExternalId(),
                entity.getResolvedReleaseVersion(),
                entity.getRouteType(),
                entity.getStatus().name(),
                entity.getRequestId(),
                entity.getIdempotencyKey(),
                entity.getBatchExternalId(),
                entity.getTaskExternalId(),
                entity.getParentInvocationExternalId(),
                entity.getItemId(),
                entity.isArtifactSaved(),
                entity.getDocumentId(),
                entity.getDocumentExpiresAt(),
                entity.getRecordExpiresAt(),
                entity.getCreatedAt(),
                childItemCount
        );
    }

    private Map<String, Object> readParameters(String parametersStorage) {
        try {
            return objectMapper.readValue(parametersStorage, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void assertTemplateAccess(TemplateEntity template, RuntimeSessionClaims session) {
        if (!template.getId().equals(session.templateId())) {
            throw new TemplateValidationException("api.error.runtime.templateCredentialMismatch");
        }
    }
}
