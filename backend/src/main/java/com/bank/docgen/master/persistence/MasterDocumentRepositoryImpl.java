package com.bank.docgen.master.persistence;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MasterDocumentRepositoryImpl implements MasterDocumentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CatalogQueryPage<MasterDocumentEntity> searchCatalog(MasterCatalogFilter filter, int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<MasterDocumentEntity> countRoot = countQuery.from(MasterDocumentEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, filter));
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<MasterDocumentEntity> dataQuery = cb.createQuery(MasterDocumentEntity.class);
        Root<MasterDocumentEntity> root = dataQuery.from(MasterDocumentEntity.class);
        dataQuery.where(buildPredicates(cb, root, filter));
        dataQuery.orderBy(buildOrders(cb, root, filter.sort()));

        TypedQuery<MasterDocumentEntity> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<MasterDocumentEntity> content = typedQuery.getResultList();
        return new CatalogQueryPage<>(content, totalElements, CatalogPageSupport.totalPages(totalElements, size));
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder cb,
            Root<MasterDocumentEntity> root,
            MasterCatalogFilter filter
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));
        if (!filter.allGroups()) {
            predicates.add(root.get("groupCode").in(filter.accessibleGroupCodes()));
        }
        if (filter.groupCodeExact() != null) {
            predicates.add(cb.equal(root.get("groupCode"), filter.groupCodeExact()));
        }
        if (filter.status() != null) {
            predicates.add(cb.equal(root.get("status"), filter.status()));
        }
        if (filter.search() != null) {
            String pattern = "%" + filter.search().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("groupCode")), pattern)
            ));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private List<Order> buildOrders(CriteriaBuilder cb, Root<MasterDocumentEntity> root, CatalogSortKey sort) {
        return switch (sort == null ? CatalogSortKey.GROUP_CODE_ASC : sort) {
            case UPDATED_AT_DESC -> List.of(cb.desc(root.get("updatedAt")), cb.asc(root.get("id")));
            case UPDATED_AT_ASC -> List.of(cb.asc(root.get("updatedAt")), cb.asc(root.get("id")));
            case NAME_ASC -> List.of(cb.asc(root.get("name")), cb.asc(root.get("id")));
            case EXTERNAL_ID_ASC, MODULE_CODE_ASC, GROUP_CODE_ASC -> List.of(
                    cb.asc(root.get("groupCode")),
                    cb.desc(root.get("updatedAt")),
                    cb.asc(root.get("id"))
            );
        };
    }
}
