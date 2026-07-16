package com.bank.docgen.audit.service;

import com.bank.docgen.audit.persistence.ManagementAuditEventCleanupRepository;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.legalhold.service.LegalHoldExemptionService;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventCleanupRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LR-D1 / ADR-0048: hard-delete aged management + runtime audit rows and write purge evidence.
 * CE-G04: filter ACTIVE legal-hold exemptions before delete (G04-C12…C16).
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
    private final LegalHoldExemptionService legalHoldExemptionService;
    private final Clock clock;
    private final int managementRetentionDays;
    private final int runtimeRetentionDays;
    private final boolean retentionEnabled;

    public AuditRetentionCleanupService(
            ManagementAuditEventCleanupRepository managementCleanup,
            RuntimeGenerationAuditEventCleanupRepository runtimeCleanup,
            AuditRetentionPurgeEvidenceWriter purgeEvidenceWriter,
            LegalHoldExemptionService legalHoldExemptionService,
            Clock clock,
            @Value("${docgen.audit.management-retention-days:90}") int managementRetentionDays,
            @Value("${docgen.audit.runtime-retention-days:365}") int runtimeRetentionDays,
            @Value("${docgen.audit.retention-enabled:true}") boolean retentionEnabled
    ) {
        this.managementCleanup = managementCleanup;
        this.runtimeCleanup = runtimeCleanup;
        this.purgeEvidenceWriter = purgeEvidenceWriter;
        this.legalHoldExemptionService = legalHoldExemptionService;
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
        List<ManagementAuditEventEntity> candidates = managementCleanup.findByEventAtBefore(cutoff);
        List<ManagementAuditEventEntity> toDelete = new ArrayList<>();
        int skipped = 0;
        for (ManagementAuditEventEntity event : candidates) {
            if (legalHoldExemptionService.isManagementAuditExempt(event.getTemplateId(), event.getEventAt())) {
                skipped++;
                continue;
            }
            toDelete.add(event);
        }
        if (toDelete.isEmpty()) {
            if (skipped > 0) {
                LOG.info(
                        "LR-D1 management audit retention purge: deleted=0 skippedLegalHold={} cutoff={}",
                        skipped,
                        cutoff
                );
            }
            return 0;
        }
        managementCleanup.deleteAll(toDelete);
        int deleted = toDelete.size();
        purgeEvidenceWriter.write(MANAGEMENT_TABLE, managementRetentionDays, cutoff, deleted);
        LOG.info(
                "LR-D1 management audit retention purge: deleted={} skippedLegalHold={} retentionDays={} cutoff={}",
                deleted,
                skipped,
                managementRetentionDays,
                cutoff
        );
        return deleted;
    }

    @Transactional
    public int purgeRuntimeAudit() {
        if (!retentionEnabled) {
            return 0;
        }
        Instant cutoff = cutoff(runtimeRetentionDays);
        List<RuntimeGenerationAuditEventEntity> candidates = runtimeCleanup.findByEventAtBefore(cutoff);
        List<RuntimeGenerationAuditEventEntity> toDelete = new ArrayList<>();
        int skipped = 0;
        for (RuntimeGenerationAuditEventEntity event : candidates) {
            if (legalHoldExemptionService.isRuntimeAuditExempt(
                    event.getTemplateId(),
                    event.getEventAt(),
                    event.getTaskExternalId(),
                    event.getDocumentId()
            )) {
                skipped++;
                continue;
            }
            toDelete.add(event);
        }
        if (toDelete.isEmpty()) {
            if (skipped > 0) {
                LOG.info(
                        "LR-D1 runtime audit retention purge: deleted=0 skippedLegalHold={} cutoff={}",
                        skipped,
                        cutoff
                );
            }
            return 0;
        }
        runtimeCleanup.deleteAll(toDelete);
        int deleted = toDelete.size();
        purgeEvidenceWriter.write(RUNTIME_TABLE, runtimeRetentionDays, cutoff, deleted);
        LOG.info(
                "LR-D1 runtime audit retention purge: deleted={} skippedLegalHold={} retentionDays={} cutoff={}",
                deleted,
                skipped,
                runtimeRetentionDays,
                cutoff
        );
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
