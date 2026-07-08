package com.bank.docgen.runtime.persistence;

import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.runtime.domain.InvocationKind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ApiInvocationRecordRepositoryImpl implements ApiInvocationRecordRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public AuditSearchPage<ApiInvocationRecordEntity> searchManagementInvocations(
            UUID templateId,
            Collection<InvocationKind> kinds,
            Instant retentionAfter,
            String outcome,
            InvocationKind invocationKind,
            String requestId,
            Instant createdAfter,
            Instant createdBefore,
            UUID credentialId,
            int page,
            int size
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<ApiInvocationRecordEntity> countRoot = countQuery.from(ApiInvocationRecordEntity.class);
        countQuery.select(criteriaBuilder.count(countRoot));
        countQuery.where(buildPredicates(
                criteriaBuilder,
                countRoot,
                templateId,
                kinds,
                retentionAfter,
                outcome,
                invocationKind,
                requestId,
                createdAfter,
                createdBefore,
                credentialId
        ));
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<ApiInvocationRecordEntity> criteriaQuery =
                criteriaBuilder.createQuery(ApiInvocationRecordEntity.class);
        Root<ApiInvocationRecordEntity> root = criteriaQuery.from(ApiInvocationRecordEntity.class);
        criteriaQuery.where(buildPredicates(
                criteriaBuilder,
                root,
                templateId,
                kinds,
                retentionAfter,
                outcome,
                invocationKind,
                requestId,
                createdAfter,
                createdBefore,
                credentialId
        ));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("createdAt")));

        TypedQuery<ApiInvocationRecordEntity> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<ApiInvocationRecordEntity> content = typedQuery.getResultList();
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AuditSearchPage<>(content, totalElements, totalPages);
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder criteriaBuilder,
            Root<ApiInvocationRecordEntity> root,
            UUID templateId,
            Collection<InvocationKind> kinds,
            Instant retentionAfter,
            String outcome,
            InvocationKind invocationKind,
            String requestId,
            Instant createdAfter,
            Instant createdBefore,
            UUID credentialId
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(root.get("templateId"), templateId));
        predicates.add(root.get("invocationKind").in(kinds));
        predicates.add(criteriaBuilder.greaterThan(root.get("recordExpiresAt"), retentionAfter));
        if (outcome != null && !outcome.isBlank()) {
            predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.upper(root.get("outcome")),
                    outcome.trim().toUpperCase(Locale.ROOT)
            ));
        }
        if (invocationKind != null) {
            predicates.add(criteriaBuilder.equal(root.get("invocationKind"), invocationKind));
        }
        if (requestId != null && !requestId.isBlank()) {
            String pattern = "%" + escapeLike(requestId.trim().toLowerCase(Locale.ROOT)) + "%";
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("requestId")),
                    pattern
            ));
        }
        if (createdAfter != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
        }
        if (createdBefore != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
        }
        if (credentialId != null) {
            predicates.add(criteriaBuilder.equal(root.get("credentialId"), credentialId));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
