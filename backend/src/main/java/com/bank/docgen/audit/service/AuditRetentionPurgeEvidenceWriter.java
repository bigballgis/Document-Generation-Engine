package com.bank.docgen.audit.service;

import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LR-D1: writes platform-level {@code AUDIT_RETENTION_PURGE} evidence rows.
 */
@Service
public class AuditRetentionPurgeEvidenceWriter {

    public static final String AUDIT_RETENTION_PURGE = "AUDIT_RETENTION_PURGE";
    public static final String ACTOR_USERNAME = "SYSTEM";
    public static final String ACTOR_SUMMARY = "Audit retention cleanup scheduler";

    private static final int STATUS_SUMMARY_MAX = 512;

    private final ManagementAuditEventRepository repository;
    private final Clock clock;

    public AuditRetentionPurgeEvidenceWriter(ManagementAuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void write(String targetTable, int retentionDays, Instant cutoff, int deletedCount) {
        String statusSummary = truncate(
                "table=" + targetTable
                        + "; retentionDays=" + retentionDays
                        + "; cutoff=" + cutoff
                        + "; deletedCount=" + deletedCount
        );
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                clock.instant(),
                AUDIT_RETENTION_PURGE,
                null,
                null,
                null,
                null,
                null,
                "[]",
                false,
                null,
                ACTOR_USERNAME,
                ACTOR_SUMMARY,
                null,
                statusSummary,
                "[]"
        ));
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= STATUS_SUMMARY_MAX ? value : value.substring(0, STATUS_SUMMARY_MAX);
    }
}
