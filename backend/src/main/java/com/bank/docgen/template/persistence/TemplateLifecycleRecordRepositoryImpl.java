package com.bank.docgen.template.persistence;

import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.template.domain.LifecycleAction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TemplateLifecycleRecordRepositoryImpl implements TemplateLifecycleRecordRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public AuditSearchPage<TemplateLifecycleRecordEntity> searchPaged(
            UUID templateId,
            String eventType,
            Instant eventAtFrom,
            Instant eventAtTo,
            int page,
            int size
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<TemplateLifecycleRecordEntity> countRoot = countQuery.from(TemplateLifecycleRecordEntity.class);
        countQuery.select(criteriaBuilder.count(countRoot));
        countQuery.where(buildPredicates(
                criteriaBuilder,
                countRoot,
                templateId,
                eventType,
                eventAtFrom,
                eventAtTo
        ));
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<TemplateLifecycleRecordEntity> criteriaQuery =
                criteriaBuilder.createQuery(TemplateLifecycleRecordEntity.class);
        Root<TemplateLifecycleRecordEntity> root = criteriaQuery.from(TemplateLifecycleRecordEntity.class);
        criteriaQuery.where(buildPredicates(
                criteriaBuilder,
                root,
                templateId,
                eventType,
                eventAtFrom,
                eventAtTo
        ));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("createdAt")));

        TypedQuery<TemplateLifecycleRecordEntity> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<TemplateLifecycleRecordEntity> content = typedQuery.getResultList();
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AuditSearchPage<>(content, totalElements, totalPages);
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder criteriaBuilder,
            Root<TemplateLifecycleRecordEntity> root,
            UUID templateId,
            String eventType,
            Instant eventAtFrom,
            Instant eventAtTo
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (templateId != null) {
            predicates.add(criteriaBuilder.equal(root.get("templateId"), templateId));
        }
        if (eventType != null) {
            predicates.add(criteriaBuilder.equal(root.get("action"), LifecycleAction.valueOf(eventType)));
        }
        if (eventAtFrom != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), eventAtFrom));
        }
        if (eventAtTo != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), eventAtTo));
        }
        return predicates.toArray(Predicate[]::new);
    }
}
