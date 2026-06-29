package com.bank.docgen.audit.persistence;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ManagementAuditEventRepositoryImpl implements ManagementAuditEventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ManagementAuditEventRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<ManagementAuditEventEntity> search(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode
    ) {
        QManagementAuditEventEntity event = QManagementAuditEventEntity.managementAuditEventEntity;
        Predicate where = ManagementAuditEventQueryPredicates.build(
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupCode
        );
        return queryFactory
                .selectFrom(event)
                .where(where)
                .orderBy(event.eventAt.desc())
                .fetch();
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
        QManagementAuditEventEntity event = QManagementAuditEventEntity.managementAuditEventEntity;
        Predicate where = ManagementAuditEventQueryPredicates.build(
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupCode
        );

        Long totalElementsResult = queryFactory
                .select(event.count())
                .from(event)
                .where(where)
                .fetchOne();
        long totalElements = totalElementsResult == null ? 0L : totalElementsResult;

        List<ManagementAuditEventEntity> content = queryFactory
                .selectFrom(event)
                .where(where)
                .orderBy(event.eventAt.desc())
                .offset((long) page * size)
                .limit(size)
                .fetch();

        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AuditSearchPage<>(content, totalElements, totalPages);
    }
}
