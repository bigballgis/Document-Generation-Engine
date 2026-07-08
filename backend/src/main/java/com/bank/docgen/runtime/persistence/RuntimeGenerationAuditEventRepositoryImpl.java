package com.bank.docgen.runtime.persistence;

import com.bank.docgen.audit.persistence.AuditSearchPage;
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

public class RuntimeGenerationAuditEventRepositoryImpl implements RuntimeGenerationAuditEventRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RuntimeGenerationAuditEventEntity> search(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode,
            String requestId
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RuntimeGenerationAuditEventEntity> criteriaQuery =
                criteriaBuilder.createQuery(RuntimeGenerationAuditEventEntity.class);
        Root<RuntimeGenerationAuditEventEntity> root = criteriaQuery.from(RuntimeGenerationAuditEventEntity.class);
        criteriaQuery.where(buildPredicates(
                criteriaBuilder,
                root,
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupCode,
                requestId
        ));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("eventAt")));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public AuditSearchPage<RuntimeGenerationAuditEventEntity> searchPaged(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode,
            String requestId,
            int page,
            int size
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<RuntimeGenerationAuditEventEntity> countRoot = countQuery.from(RuntimeGenerationAuditEventEntity.class);
        countQuery.select(criteriaBuilder.count(countRoot));
        countQuery.where(buildPredicates(
                criteriaBuilder,
                countRoot,
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupCode,
                requestId
        ));
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<RuntimeGenerationAuditEventEntity> criteriaQuery =
                criteriaBuilder.createQuery(RuntimeGenerationAuditEventEntity.class);
        Root<RuntimeGenerationAuditEventEntity> root = criteriaQuery.from(RuntimeGenerationAuditEventEntity.class);
        criteriaQuery.where(buildPredicates(
                criteriaBuilder,
                root,
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupCode,
                requestId
        ));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("eventAt")));

        TypedQuery<RuntimeGenerationAuditEventEntity> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<RuntimeGenerationAuditEventEntity> content = typedQuery.getResultList();
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AuditSearchPage<>(content, totalElements, totalPages);
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder criteriaBuilder,
            Root<RuntimeGenerationAuditEventEntity> root,
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode,
            String requestId
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (templateId != null) {
            predicates.add(criteriaBuilder.equal(root.get("templateId"), templateId));
        }
        if (eventType != null) {
            predicates.add(criteriaBuilder.equal(root.get("eventType"), eventType));
        }
        if (credentialId != null) {
            predicates.add(criteriaBuilder.equal(root.get("credentialId"), credentialId));
        }
        if (eventAtFrom != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventAt"), eventAtFrom));
        }
        if (eventAtTo != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("eventAt"), eventAtTo));
        }
        if (groupCode != null) {
            predicates.add(criteriaBuilder.equal(root.get("groupCode"), groupCode));
        }
        if (requestId != null) {
            predicates.add(criteriaBuilder.equal(root.get("requestId"), requestId));
        }
        return predicates.toArray(Predicate[]::new);
    }
}
