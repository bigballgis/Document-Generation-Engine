package com.bank.docgen.audit.persistence;

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

public class ManagementAuditEventRepositoryImpl implements ManagementAuditEventRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ManagementAuditEventEntity> search(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ManagementAuditEventEntity> criteriaQuery =
                criteriaBuilder.createQuery(ManagementAuditEventEntity.class);
        Root<ManagementAuditEventEntity> root = criteriaQuery.from(ManagementAuditEventEntity.class);
        criteriaQuery.where(buildPredicates(
                criteriaBuilder,
                root,
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupCode
        ));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("eventAt")));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public AuditSearchPage<ManagementAuditEventEntity> searchPaged(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode,
            int page,
            int size
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<ManagementAuditEventEntity> countRoot = countQuery.from(ManagementAuditEventEntity.class);
        countQuery.select(criteriaBuilder.count(countRoot));
        countQuery.where(buildPredicates(
                criteriaBuilder,
                countRoot,
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupCode
        ));
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<ManagementAuditEventEntity> criteriaQuery =
                criteriaBuilder.createQuery(ManagementAuditEventEntity.class);
        Root<ManagementAuditEventEntity> dataRoot = criteriaQuery.from(ManagementAuditEventEntity.class);
        criteriaQuery.where(buildPredicates(
                criteriaBuilder,
                dataRoot,
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupCode
        ));
        criteriaQuery.orderBy(criteriaBuilder.desc(dataRoot.get("eventAt")));

        TypedQuery<ManagementAuditEventEntity> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<ManagementAuditEventEntity> content = typedQuery.getResultList();
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AuditSearchPage<>(content, totalElements, totalPages);
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder criteriaBuilder,
            Root<ManagementAuditEventEntity> root,
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode
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
        return predicates.toArray(Predicate[]::new);
    }
}
