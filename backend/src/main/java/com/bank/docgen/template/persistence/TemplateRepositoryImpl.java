package com.bank.docgen.template.persistence;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TemplateRepositoryImpl implements TemplateRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CatalogQueryPage<TemplateEntity> searchCatalog(TemplateCatalogFilter filter, int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<TemplateEntity> countRoot = countQuery.from(TemplateEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countQuery, countRoot, filter));
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<TemplateEntity> dataQuery = cb.createQuery(TemplateEntity.class);
        Root<TemplateEntity> root = dataQuery.from(TemplateEntity.class);
        dataQuery.where(buildPredicates(cb, dataQuery, root, filter));
        dataQuery.orderBy(buildOrders(cb, root, filter.sort()));

        TypedQuery<TemplateEntity> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<TemplateEntity> content = typedQuery.getResultList();
        return new CatalogQueryPage<>(content, totalElements, CatalogPageSupport.totalPages(totalElements, size));
    }

    @Override
    public Map<TemplateLifecycleStatus, Long> countGroupedByLifecycleStatus(
            List<String> accessibleGroupCodes,
            boolean allGroups
    ) {
        if (!allGroups && (accessibleGroupCodes == null || accessibleGroupCodes.isEmpty())) {
            return Map.of();
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<TemplateEntity> root = query.from(TemplateEntity.class);
        query.multiselect(root.get("lifecycleStatus"), cb.count(root));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));
        if (!allGroups) {
            predicates.add(root.get("groupCode").in(accessibleGroupCodes));
        }
        query.where(predicates.toArray(Predicate[]::new));
        query.groupBy(root.get("lifecycleStatus"));
        Map<TemplateLifecycleStatus, Long> counts = new EnumMap<>(TemplateLifecycleStatus.class);
        for (Object[] row : entityManager.createQuery(query).getResultList()) {
            counts.put((TemplateLifecycleStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<TemplateEntity> root,
            TemplateCatalogFilter filter
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));

        if (!filter.allGroups()) {
            predicates.add(root.get("groupCode").in(filter.accessibleGroupCodes()));
        }
        if (filter.groupCodeExact() != null) {
            predicates.add(cb.equal(root.get("groupCode"), filter.groupCodeExact()));
        }
        if (filter.lifecycleStatus() != null) {
            predicates.add(cb.equal(root.get("lifecycleStatus"), filter.lifecycleStatus()));
        }
        if (filter.search() != null) {
            String pattern = "%" + filter.search().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("externalId")), pattern),
                    cb.like(cb.lower(root.get("groupCode")), pattern)
            ));
        }
        if (filter.approvalSubState() != null) {
            predicates.add(cb.equal(root.get("lifecycleStatus"), TemplateLifecycleStatus.APPROVAL));
            Subquery<UUID> submitted = query.subquery(UUID.class);
            Root<TemplateLifecycleRecordEntity> recordRoot = submitted.from(TemplateLifecycleRecordEntity.class);
            submitted.select(recordRoot.get("templateId"));
            submitted.where(
                    cb.equal(recordRoot.get("templateId"), root.get("id")),
                    cb.equal(recordRoot.get("action"), LifecycleAction.SUBMIT_FOR_APPROVAL)
            );
            if (filter.approvalSubState() == ApprovalSubState.PENDING_DECISION) {
                predicates.add(cb.exists(submitted));
            } else if (filter.approvalSubState() == ApprovalSubState.PENDING_SUBMIT) {
                predicates.add(cb.not(cb.exists(submitted)));
            }
        }
        return predicates.toArray(Predicate[]::new);
    }

    private List<Order> buildOrders(CriteriaBuilder cb, Root<TemplateEntity> root, CatalogSortKey sort) {
        return switch (sort == null ? CatalogSortKey.GROUP_CODE_ASC : sort) {
            case UPDATED_AT_DESC -> List.of(cb.desc(root.get("updatedAt")), cb.asc(root.get("id")));
            case UPDATED_AT_ASC -> List.of(cb.asc(root.get("updatedAt")), cb.asc(root.get("id")));
            case NAME_ASC -> List.of(cb.asc(root.get("name")), cb.asc(root.get("id")));
            case EXTERNAL_ID_ASC -> List.of(cb.asc(root.get("externalId")), cb.asc(root.get("id")));
            case MODULE_CODE_ASC, GROUP_CODE_ASC -> List.of(
                    cb.asc(root.get("groupCode")),
                    cb.desc(root.get("updatedAt")),
                    cb.asc(root.get("id"))
            );
        };
    }
}
