package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.contentmodule.domain.ContentModuleCatalogDisplayStatus;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.domain.ContentModuleSearchMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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
import java.util.UUID;

public class ContentModuleRepositoryImpl implements ContentModuleRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CatalogQueryPage<ContentModuleEntity> searchCatalog(
            ContentModuleCatalogFilter filter,
            int page,
            int size
    ) {
        if (filter.isFullTextSearch()) {
            return searchCatalogFullText(filter, page, size);
        }
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

    /**
     * CE-G05 FULL_TEXT — match catalog-filter version tsvector (config {@code simple}).
     */
    @SuppressWarnings("unchecked")
    private CatalogQueryPage<ContentModuleEntity> searchCatalogFullText(
            ContentModuleCatalogFilter filter,
            int page,
            int size
    ) {
        StringBuilder fromWhere = new StringBuilder("""
                FROM content_module m
                WHERE m.deleted_at IS NULL
                  AND EXISTS (
                    SELECT 1
                    FROM content_module_version v
                    WHERE v.module_id = m.id
                      AND v.content_search_vector @@ plainto_tsquery('simple', :q)
                      AND (
                        (
                          EXISTS (
                            SELECT 1 FROM content_module_version aa
                            WHERE aa.module_id = m.id
                              AND aa.review_state = 'APPROVED'
                              AND aa.lifecycle_state = 'ACTIVE'
                          )
                          AND v.review_state = 'APPROVED'
                          AND v.lifecycle_state = 'ACTIVE'
                          AND v.semantic_version = (
                            SELECT MAX(aa2.semantic_version)
                            FROM content_module_version aa2
                            WHERE aa2.module_id = m.id
                              AND aa2.review_state = 'APPROVED'
                              AND aa2.lifecycle_state = 'ACTIVE'
                          )
                        )
                        OR (
                          NOT EXISTS (
                            SELECT 1 FROM content_module_version aa
                            WHERE aa.module_id = m.id
                              AND aa.review_state = 'APPROVED'
                              AND aa.lifecycle_state = 'ACTIVE'
                          )
                          AND v.semantic_version = (
                            SELECT MAX(aa3.semantic_version)
                            FROM content_module_version aa3
                            WHERE aa3.module_id = m.id
                          )
                        )
                      )
                """);
        // close EXISTS for FTS; append access / legal / status via JPQL-equivalent SQL below
        fromWhere.append(")");
        appendFullTextAccessAndFilters(fromWhere, filter);

        // OpenAPI: default ts_rank when FULL_TEXT + search unless explicit whitelist sort supplied.
        String orderSql;
        if (filter.sort() == null) {
            orderSql = """
                    ORDER BY (
                      SELECT MAX(ts_rank(v2.content_search_vector, plainto_tsquery('simple', :q)))
                      FROM content_module_version v2
                      WHERE v2.module_id = m.id
                    ) DESC NULLS LAST, m.updated_at DESC, m.id ASC
                    """;
        } else {
            orderSql = switch (filter.sort()) {
                case UPDATED_AT_DESC -> " ORDER BY m.updated_at DESC, m.id ASC";
                case UPDATED_AT_ASC -> " ORDER BY m.updated_at ASC, m.id ASC";
                case NAME_ASC -> " ORDER BY m.name ASC, m.id ASC";
                case MODULE_CODE_ASC -> " ORDER BY m.module_code ASC, m.id ASC";
                case EXTERNAL_ID_ASC, GROUP_CODE_ASC -> " ORDER BY m.group_code ASC, m.updated_at DESC, m.id ASC";
            };
        }

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) " + fromWhere);
        bindFullTextParams(countQuery, filter);
        Number totalNumber = (Number) countQuery.getSingleResult();
        long totalElements = totalNumber == null ? 0L : totalNumber.longValue();

        Query dataQuery = entityManager.createNativeQuery(
                "SELECT m.id " + fromWhere + orderSql
        );
        bindFullTextParams(dataQuery, filter);
        dataQuery.setFirstResult(page * size);
        dataQuery.setMaxResults(size);
        List<UUID> ids = dataQuery.getResultList().stream()
                .map(id -> id instanceof UUID uuid ? uuid : UUID.fromString(id.toString()))
                .toList();
        if (ids.isEmpty()) {
            return new CatalogQueryPage<>(List.of(), totalElements, CatalogPageSupport.totalPages(totalElements, size));
        }
        List<ContentModuleEntity> loaded = entityManager.createQuery(
                        "SELECT m FROM ContentModuleEntity m WHERE m.id IN :ids",
                        ContentModuleEntity.class
                )
                .setParameter("ids", ids)
                .getResultList();
        List<ContentModuleEntity> ordered = ids.stream()
                .map(id -> loaded.stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null))
                .filter(m -> m != null)
                .toList();
        return new CatalogQueryPage<>(ordered, totalElements, CatalogPageSupport.totalPages(totalElements, size));
    }

    private static void appendFullTextAccessAndFilters(
            StringBuilder fromWhere,
            ContentModuleCatalogFilter filter
    ) {
        if (filter.groupCodeExact() != null) {
            fromWhere.append(" AND (m.group_code = :groupExact OR m.shared_group_codes_json LIKE :sharedExact)");
        } else if (!filter.allGroups()) {
            fromWhere.append(" AND (m.group_code IN :accessGroups");
            int i = 0;
            for (String ignored : filter.accessibleGroupCodes()) {
                fromWhere.append(" OR m.shared_group_codes_json LIKE :shared").append(i);
                i++;
            }
            fromWhere.append(")");
        }
        if (filter.locale() != null) {
            fromWhere.append(" AND m.locale = :locale");
        }
        if (filter.status() != null) {
            fromWhere.append("""
                     AND EXISTS (
                      SELECT 1 FROM content_module_version hv
                      WHERE hv.module_id = m.id
                        AND NOT EXISTS (
                          SELECT 1 FROM content_module_version newer
                          WHERE newer.module_id = m.id
                            AND (
                              newer.updated_at > hv.updated_at
                              OR (newer.updated_at = hv.updated_at AND newer.semantic_version > hv.semantic_version)
                            )
                        )
                """);
            fromWhere.append(statusSqlPredicate(filter.status()));
            fromWhere.append(")");
        }
        if (filter.hasLegalFilters()) {
            appendFullTextLegalFilters(fromWhere, filter);
        }
    }

    private static void appendFullTextLegalFilters(
            StringBuilder fromWhere,
            ContentModuleCatalogFilter filter
    ) {
        fromWhere.append("""
                 AND EXISTS (
                  SELECT 1 FROM content_module_version lv
                  WHERE lv.module_id = m.id
                    AND (
                      (
                        EXISTS (
                          SELECT 1 FROM content_module_version aa
                          WHERE aa.module_id = m.id
                            AND aa.review_state = 'APPROVED'
                            AND aa.lifecycle_state = 'ACTIVE'
                        )
                        AND lv.review_state = 'APPROVED'
                        AND lv.lifecycle_state = 'ACTIVE'
                        AND lv.semantic_version = (
                          SELECT MAX(aa2.semantic_version)
                          FROM content_module_version aa2
                          WHERE aa2.module_id = m.id
                            AND aa2.review_state = 'APPROVED'
                            AND aa2.lifecycle_state = 'ACTIVE'
                        )
                      )
                      OR (
                        NOT EXISTS (
                          SELECT 1 FROM content_module_version aa
                          WHERE aa.module_id = m.id
                            AND aa.review_state = 'APPROVED'
                            AND aa.lifecycle_state = 'ACTIVE'
                        )
                        AND lv.semantic_version = (
                          SELECT MAX(aa3.semantic_version)
                          FROM content_module_version aa3
                          WHERE aa3.module_id = m.id
                        )
                      )
                    )
            """);
        if (filter.jurisdiction() != null) {
            fromWhere.append(" AND LOWER(lv.jurisdiction) = :jurisdiction");
        }
        if (filter.legalReviewRef() != null) {
            fromWhere.append(" AND LOWER(lv.legal_review_ref) = :legalReviewRef");
        }
        if (filter.effectiveFrom() != null) {
            fromWhere.append(" AND lv.effective_from IS NOT NULL AND lv.effective_from >= :effectiveFrom");
        }
        if (filter.effectiveTo() != null) {
            fromWhere.append(" AND lv.effective_to IS NOT NULL AND lv.effective_to <= :effectiveTo");
        }
        fromWhere.append(")");
    }

    private static String statusSqlPredicate(ContentModuleCatalogDisplayStatus status) {
        return switch (status) {
            case STOPPED -> " AND hv.lifecycle_state = 'STOPPED'";
            case DEPRECATED -> " AND hv.lifecycle_state = 'DEPRECATED'";
            case DRAFT, SUBMITTED, APPROVED ->
                    " AND (hv.lifecycle_state IS NULL OR hv.lifecycle_state = 'ACTIVE')"
                            + " AND hv.review_state = '" + status.name() + "'";
        };
    }

    private void bindFullTextParams(Query query, ContentModuleCatalogFilter filter) {
        query.setParameter("q", filter.search());
        if (filter.groupCodeExact() != null) {
            query.setParameter("groupExact", filter.groupCodeExact());
            query.setParameter("sharedExact", "%\"" + filter.groupCodeExact() + "\"%");
        } else if (!filter.allGroups()) {
            query.setParameter("accessGroups", filter.accessibleGroupCodes());
            int i = 0;
            for (String group : filter.accessibleGroupCodes()) {
                query.setParameter("shared" + i, "%\"" + group + "\"%");
                i++;
            }
        }
        if (filter.jurisdiction() != null) {
            query.setParameter("jurisdiction", filter.jurisdiction().toLowerCase(Locale.ROOT));
        }
        if (filter.legalReviewRef() != null) {
            query.setParameter("legalReviewRef", filter.legalReviewRef().toLowerCase(Locale.ROOT));
        }
        if (filter.effectiveFrom() != null) {
            query.setParameter("effectiveFrom", filter.effectiveFrom());
        }
        if (filter.effectiveTo() != null) {
            query.setParameter("effectiveTo", filter.effectiveTo());
        }
        if (filter.locale() != null) {
            query.setParameter("locale", filter.locale());
        }
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

        if (filter.search() != null
                && filter.searchMode() != ContentModuleSearchMode.FULL_TEXT) {
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

        if (filter.locale() != null) {
            predicates.add(cb.equal(root.get("locale"), filter.locale()));
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
