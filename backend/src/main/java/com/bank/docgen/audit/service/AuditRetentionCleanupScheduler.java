package com.bank.docgen.audit.service;

import com.bank.docgen.audit.persistence.ManagementAuditEventCleanupRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventCleanupRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * LR-D1: scheduled cleanup of management + runtime audit tables that would otherwise grow
 * unbounded (CD-PIT-15). Mirrors the ADR-0040 invocation-record retention pattern.
 *
 * <p>Retention baselines (pending user confirmation per ADR): management audit 90d, runtime
 * generation audit 365d. The cleanup is guarded by the LR-B2 distributed mutex (ShedLock) so
 * it runs once across the cluster.
 */
@Service
@Profile("!test")
@ConditionalOnProperty(name = "docgen.audit.retention-enabled", havingValue = "true", matchIfMissing = false)
public class AuditRetentionCleanupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AuditRetentionCleanupScheduler.class);

    private final ManagementAuditEventCleanupRepository managementCleanup;
    private final RuntimeGenerationAuditEventCleanupRepository runtimeCleanup;
    private final int managementRetentionDays;
    private final int runtimeRetentionDays;

    public AuditRetentionCleanupScheduler(
            ManagementAuditEventCleanupRepository managementCleanup,
            RuntimeGenerationAuditEventCleanupRepository runtimeCleanup,
            @Value("${docgen.audit.management-retention-days:90}") int managementRetentionDays,
            @Value("${docgen.audit.runtime-retention-days:365}") int runtimeRetentionDays
    ) {
        this.managementCleanup = managementCleanup;
        this.runtimeCleanup = runtimeCleanup;
        this.managementRetentionDays = managementRetentionDays;
        this.runtimeRetentionDays = runtimeRetentionDays;
    }

    @Scheduled(cron = "${docgen.audit.retention-cron:0 0 3 * * *}")
    @SchedulerLock(name = "auditRetentionCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void cleanupExpiredAuditRecords() {
        int managementDeleted = managementCleanup.deleteOlderThanDays(managementRetentionDays);
        int runtimeDeleted = runtimeCleanup.deleteOlderThanDays(runtimeRetentionDays);
        LOG.info(
                "LR-D1 audit retention cleanup: management deleted {} (older than {}d), runtime deleted {} (older than {}d)",
                managementDeleted, managementRetentionDays, runtimeDeleted, runtimeRetentionDays
        );
    }
}
