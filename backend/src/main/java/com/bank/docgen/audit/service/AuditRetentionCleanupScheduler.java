package com.bank.docgen.audit.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * LR-D1 / ADR-0048: scheduled audit retention cleanup with LR-B2 ShedLock dual locks.
 *
 * <p>When another instance holds the lock, ShedLock skips the method body (BDD-LRP-D1-005).
 */
@Component
public class AuditRetentionCleanupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AuditRetentionCleanupScheduler.class);

    private final AuditRetentionCleanupService cleanupService;
    private final boolean retentionEnabled;

    public AuditRetentionCleanupScheduler(
            AuditRetentionCleanupService cleanupService,
            @Value("${docgen.audit.retention-enabled:true}") boolean retentionEnabled
    ) {
        this.cleanupService = cleanupService;
        this.retentionEnabled = retentionEnabled;
    }

    @Scheduled(cron = "${docgen.audit.retention-cron:0 0 3 * * *}")
    @SchedulerLock(
            name = "audit-retention-cleanup-management",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    public void cleanupExpiredManagementAudit() {
        if (!retentionEnabled) {
            LOG.debug("LR-D1 management audit retention skipped: retention-enabled=false");
            return;
        }
        cleanupService.purgeManagementAudit();
    }

    @Scheduled(cron = "${docgen.audit.retention-cron:0 0 3 * * *}")
    @SchedulerLock(
            name = "audit-retention-cleanup-runtime",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    public void cleanupExpiredRuntimeAudit() {
        if (!retentionEnabled) {
            LOG.debug("LR-D1 runtime audit retention skipped: retention-enabled=false");
            return;
        }
        cleanupService.purgeRuntimeAudit();
    }
}
