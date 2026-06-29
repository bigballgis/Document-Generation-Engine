package com.bank.docgen.audit.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import java.time.Instant;
import java.util.UUID;

final class ManagementAuditEventQueryPredicates {

    private ManagementAuditEventQueryPredicates() {
    }

    static Predicate build(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode
    ) {
        QManagementAuditEventEntity event = QManagementAuditEventEntity.managementAuditEventEntity;
        BooleanBuilder builder = new BooleanBuilder();
        if (templateId != null) {
            builder.and(event.templateId.eq(templateId));
        }
        if (eventType != null) {
            builder.and(event.eventType.eq(eventType));
        }
        if (credentialId != null) {
            builder.and(event.credentialId.eq(credentialId));
        }
        if (eventAtFrom != null) {
            builder.and(event.eventAt.goe(eventAtFrom));
        }
        if (eventAtTo != null) {
            builder.and(event.eventAt.loe(eventAtTo));
        }
        if (groupCode != null) {
            builder.and(event.groupCode.eq(groupCode));
        }
        return builder;
    }
}
