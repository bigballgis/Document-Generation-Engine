package com.bank.docgen.audit.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

        criteriaQuery.where(predicates.toArray(Predicate[]::new));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("eventAt")));

        return entityManager.createQuery(criteriaQuery).getResultList();
    }
}
