package com.bank.docgen.audit.service;

import com.bank.docgen.audit.persistence.ManagementAuditEventCleanupRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventCleanupRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LR-D1 / ADR-0048: hard-delete aged management + runtime audit rows and write purge evidence.
 */
@Service
public class AuditRetentionCleanupService {

    public static final String MANAGEMENT_TABLE = "management_audit_event";
    public static final String RUNTIME_TABLE = "runtime_generation_audit_event";

    private static final int MIN_RETENTION_DAYS = 1;
    private static final int MAX_RETENTION_DAYS = 2555;

    private static final Logger LOG = LoggerFactory.getLogger(AuditRetentionCleanupService.class);

    private final ManagementAuditEventCleanupRepository managementCleanup;
    private final RuntimeGenerationAuditEventCleanupRepository runtimeCleanup;
    private final AuditRetentionPurgeEvidenceWriter purgeEvidenceWriter;
    private final Clock clock;
    private final int managementRetentionDays;
    private final int runtimeRetentionDays;
    private final boolean retentionEnabled;

    public AuditRetentionCleanupService(
            ManagementAuditEventCleanupRepository managementCleanup,
            RuntimeGenerationAuditEventCleanupRepository runtimeCleanup,
            AuditRetentionPurgeEvidenceWriter purgeEvidenceWriter,
            Clock clock,
            @Value("${docgen.audit.management-retention-days:90}") int managementRetentionDays,
            @Value("${docgen.audit.runtime-retention-days:365}") int runtimeRetentionDays,
            @Value("${docgen.audit.retention-enabled:true}") boolean retentionEnabled
    ) {
        this.managementCleanup = managementCleanup;
        this.runtimeCleanup = runtimeCleanup;
        this.purgeEvidenceWriter = purgeEvidenceWriter;
        this.clock = clock;
        this.managementRetentionDays = requireRetentionDays(managementRetentionDays, "management");
        this.runtimeRetentionDays = requireRetentionDays(runtimeRetentionDays, "runtime");
        this.retentionEnabled = retentionEnabled;
    }

    @Transactional
    public int purgeManagementAudit() {
        if (!retentionEnabled) {
            return 0;
        }
        Instant cutoff = cutoff(managementRetentionDays);
        int deleted = managementCleanup.deleteOlderThan(cutoff);
        if (deleted > 0) {
            purgeEvidenceWriter.write(MANAGEMENT_TABLE, managementRetentionDays, cutoff, deleted);
            LOG.info(
                    "LR-D1 management audit retention purge: deleted={} retentionDays={} cutoff={}",
                    deleted,
                    managementRetentionDays,
                    cutoff
            );
        }
        return deleted;
    }

    @Transactional
    public int purgeRuntimeAudit() {
        if (!retentionEnabled) {
            return 0;
        }
        Instant cutoff = cutoff(runtimeRetentionDays);
        int deleted = runtimeCleanup.deleteOlderThan(cutoff);
        if (deleted > 0) {
            purgeEvidenceWriter.write(RUNTIME_TABLE, runtimeRetentionDays, cutoff, deleted);
            LOG.info(
                    "LR-D1 runtime audit retention purge: deleted={} retentionDays={} cutoff={}",
                    deleted,
                    runtimeRetentionDays,
                    cutoff
            );
        }
        return deleted;
    }

    private Instant cutoff(int retentionDays) {
        return clock.instant().minus(retentionDays, ChronoUnit.DAYS);
    }

    private static int requireRetentionDays(int days, String label) {
        if (days < MIN_RETENTION_DAYS || days > MAX_RETENTION_DAYS) {
            throw new IllegalStateException(
                    "docgen.audit." + label + "-retention-days must be between "
                            + MIN_RETENTION_DAYS + " and " + MAX_RETENTION_DAYS + " (inclusive); got " + days
            );
        }
        return days;
    }
}
