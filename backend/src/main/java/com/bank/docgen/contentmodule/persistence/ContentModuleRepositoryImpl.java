package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.contentmodule.domain.ContentModuleCatalogDisplayStatus;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
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
import java.util.List;
import java.util.Locale;

public class ContentModuleRepositoryImpl implements ContentModuleRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CatalogQueryPage<ContentModuleEntity> searchCatalog(
            ContentModuleCatalogFilter filter,
            int page,
            int size
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ContentModuleEntity> countRoot = countQuery.from(ContentModuleEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countQuery, countRoot, filter));
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<ContentModuleEntity> dataQuery = cb.createQuery(ContentModuleEntity.class);
        Root<ContentModuleEntity> root = dataQuery.from(ContentModuleEntity.class);
        dataQuery.where(buildPredicates(cb, dataQuery, root, filter));
        dataQuery.orderBy(buildOrders(cb, root, filter.sort()));

        TypedQuery<ContentModuleEntity> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<ContentModuleEntity> content = typedQuery.getResultList();
        return new CatalogQueryPage<>(content, totalElements, CatalogPageSupport.totalPages(totalElements, size));
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<ContentModuleEntity> root,
            ContentModuleCatalogFilter filter
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));

        if (filter.groupCodeExact() != null) {
            String group = filter.groupCodeExact();
            String sharedToken = "%\"" + group + "\"%";
            predicates.add(cb.or(
                    cb.equal(root.get("groupCode"), group),
                    cb.like(root.get("sharedGroupCodesJson"), sharedToken)
            ));
        } else if (!filter.allGroups()) {
            List<Predicate> accessPredicates = new ArrayList<>();
            accessPredicates.add(root.get("groupCode").in(filter.accessibleGroupCodes()));
            for (String group : filter.accessibleGroupCodes()) {
                accessPredicates.add(cb.like(root.get("sharedGroupCodesJson"), "%\"" + group + "\"%"));
            }
            predicates.add(cb.or(accessPredicates.toArray(Predicate[]::new)));
        }

        if (filter.search() != null) {
            String pattern = "%" + filter.search().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("moduleCode")), pattern),
                    cb.like(cb.lower(root.get("groupCode")), pattern)
            ));
        }

        if (filter.status() != null) {
            predicates.add(buildHeadDisplayStatusPredicate(cb, query, root, filter.status()));
        }

        if (filter.hasLegalFilters()) {
            predicates.add(buildCatalogFilterVersionLegalPredicate(cb, query, root, filter));
        }
        return predicates.toArray(Predicate[]::new);
    }

    /**
     * CE-U20 — match badge-aligned display status of the module head version
     * (max {@code updatedAt}, tie → greater {@code semanticVersion}).
     */
    private Predicate buildHeadDisplayStatusPredicate(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<ContentModuleEntity> moduleRoot,
            ContentModuleCatalogDisplayStatus status
    ) {
        Subquery<Integer> exists = query.subquery(Integer.class);
        Root<ContentModuleVersionEntity> version = exists.from(ContentModuleVersionEntity.class);
        exists.select(cb.literal(1));
        exists.where(
                cb.equal(version.get("moduleId"), moduleRoot.get("id")),
                isHeadVersion(cb, exists, moduleRoot, version),
                matchesDisplayStatus(cb, version, status)
        );
        return cb.exists(exists);
    }

    private Predicate isHeadVersion(
            CriteriaBuilder cb,
            Subquery<?> outer,
            Root<ContentModuleEntity> moduleRoot,
            Root<ContentModuleVersionEntity> version
    ) {
        Subquery<Integer> newer = outer.subquery(Integer.class);
        Root<ContentModuleVersionEntity> other = newer.from(ContentModuleVersionEntity.class);
        newer.select(cb.literal(1));
        newer.where(
                cb.equal(other.get("moduleId"), moduleRoot.get("id")),
                cb.or(
                        cb.greaterThan(other.get("updatedAt"), version.get("updatedAt")),
                        cb.and(
                                cb.equal(other.get("updatedAt"), version.get("updatedAt")),
                                cb.greaterThan(other.get("semanticVersion"), version.get("semanticVersion"))
                        )
                )
        );
        return cb.not(cb.exists(newer));
    }

    private Predicate matchesDisplayStatus(
            CriteriaBuilder cb,
            Root<ContentModuleVersionEntity> version,
            ContentModuleCatalogDisplayStatus status
    ) {
        return switch (status) {
            case STOPPED -> cb.equal(version.get("lifecycleState"), ContentModuleLifecycleState.STOPPED);
            case DEPRECATED -> cb.equal(version.get("lifecycleState"), ContentModuleLifecycleState.DEPRECATED);
            case DRAFT, SUBMITTED, APPROVED -> cb.and(
                    cb.or(
                            cb.isNull(version.get("lifecycleState")),
                            cb.equal(version.get("lifecycleState"), ContentModuleLifecycleState.ACTIVE)
                    ),
                    cb.equal(version.get("reviewState"), ContentModuleReviewState.valueOf(status.name()))
            );
        };
    }

    /**
     * Matches legal filters against the module's catalog filter version (K08-C7):
     * latest APPROVED+ACTIVE by semanticVersion, else latest version overall.
     */
    private Predicate buildCatalogFilterVersionLegalPredicate(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<ContentModuleEntity> moduleRoot,
            ContentModuleCatalogFilter filter
    ) {
        Subquery<Integer> exists = query.subquery(Integer.class);
        Root<ContentModuleVersionEntity> version = exists.from(ContentModuleVersionEntity.class);
        exists.select(cb.literal(1));

        List<Predicate> versionPredicates = new ArrayList<>();
        versionPredicates.add(cb.equal(version.get("moduleId"), moduleRoot.get("id")));
        versionPredicates.add(isCatalogFilterVersion(cb, exists, moduleRoot, version));

        if (filter.jurisdiction() != null) {
            versionPredicates.add(cb.equal(
                    cb.lower(version.get("jurisdiction")),
                    filter.jurisdiction().toLowerCase(Locale.ROOT)
            ));
        }
        if (filter.legalReviewRef() != null) {
            versionPredicates.add(cb.equal(
                    cb.lower(version.get("legalReviewRef")),
                    filter.legalReviewRef().toLowerCase(Locale.ROOT)
            ));
        }
        if (filter.effectiveFrom() != null) {
            versionPredicates.add(cb.isNotNull(version.get("effectiveFrom")));
            versionPredicates.add(cb.greaterThanOrEqualTo(version.get("effectiveFrom"), filter.effectiveFrom()));
        }
        if (filter.effectiveTo() != null) {
            versionPredicates.add(cb.isNotNull(version.get("effectiveTo")));
            versionPredicates.add(cb.lessThanOrEqualTo(version.get("effectiveTo"), filter.effectiveTo()));
        }

        exists.where(versionPredicates.toArray(Predicate[]::new));
        return cb.exists(exists);
    }

    private Predicate isCatalogFilterVersion(
            CriteriaBuilder cb,
            Subquery<?> outer,
            Root<ContentModuleEntity> moduleRoot,
            Root<ContentModuleVersionEntity> version
    ) {
        Subquery<Long> approvedActiveCount = outer.subquery(Long.class);
        Root<ContentModuleVersionEntity> approvedCountRoot = approvedActiveCount.from(ContentModuleVersionEntity.class);
        approvedActiveCount.select(cb.count(approvedCountRoot));
        approvedActiveCount.where(
                cb.equal(approvedCountRoot.get("moduleId"), moduleRoot.get("id")),
                cb.equal(approvedCountRoot.get("reviewState"), ContentModuleReviewState.APPROVED),
                cb.equal(approvedCountRoot.get("lifecycleState"), ContentModuleLifecycleState.ACTIVE)
        );

        Subquery<String> maxApprovedSemver = outer.subquery(String.class);
        Root<ContentModuleVersionEntity> maxApprovedRoot = maxApprovedSemver.from(ContentModuleVersionEntity.class);
        maxApprovedSemver.select(cb.greatest(maxApprovedRoot.<String>get("semanticVersion")));
        maxApprovedSemver.where(
                cb.equal(maxApprovedRoot.get("moduleId"), moduleRoot.get("id")),
                cb.equal(maxApprovedRoot.get("reviewState"), ContentModuleReviewState.APPROVED),
                cb.equal(maxApprovedRoot.get("lifecycleState"), ContentModuleLifecycleState.ACTIVE)
        );

        Subquery<String> maxAnySemver = outer.subquery(String.class);
        Root<ContentModuleVersionEntity> maxAnyRoot = maxAnySemver.from(ContentModuleVersionEntity.class);
        maxAnySemver.select(cb.greatest(maxAnyRoot.<String>get("semanticVersion")));
        maxAnySemver.where(cb.equal(maxAnyRoot.get("moduleId"), moduleRoot.get("id")));

        Predicate useApprovedActive = cb.and(
                cb.greaterThan(approvedActiveCount, 0L),
                cb.equal(version.get("reviewState"), ContentModuleReviewState.APPROVED),
                cb.equal(version.get("lifecycleState"), ContentModuleLifecycleState.ACTIVE),
                cb.equal(version.get("semanticVersion"), maxApprovedSemver)
        );
        Predicate useLatestAny = cb.and(
                cb.equal(approvedActiveCount, 0L),
                cb.equal(version.get("semanticVersion"), maxAnySemver)
        );
        return cb.or(useApprovedActive, useLatestAny);
    }

    private List<Order> buildOrders(CriteriaBuilder cb, Root<ContentModuleEntity> root, CatalogSortKey sort) {
        return switch (sort == null ? CatalogSortKey.GROUP_CODE_ASC : sort) {
            case UPDATED_AT_DESC -> List.of(cb.desc(root.get("updatedAt")), cb.asc(root.get("id")));
            case UPDATED_AT_ASC -> List.of(cb.asc(root.get("updatedAt")), cb.asc(root.get("id")));
            case NAME_ASC -> List.of(cb.asc(root.get("name")), cb.asc(root.get("id")));
            case MODULE_CODE_ASC -> List.of(cb.asc(root.get("moduleCode")), cb.asc(root.get("id")));
            case EXTERNAL_ID_ASC, GROUP_CODE_ASC -> List.of(
                    cb.asc(root.get("groupCode")),
                    cb.desc(root.get("updatedAt")),
                    cb.asc(root.get("id"))
            );
        };
    }
}
