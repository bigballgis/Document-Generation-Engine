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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * LR-D1 / ADR-0048: hard-delete aged management + runtime audit rows and write purge evidence.
 * CE-G04: filter ACTIVE legal-hold exemptions before delete (G04-C12…C16).
 * PRR-A01: Pageable / LIMIT batch loads; short per-batch deletes (no outer long transaction).
 */
@Service
public class AuditRetentionCleanupService {

    public static final String MANAGEMENT_TABLE = "management_audit_event";
    public static final String RUNTIME_TABLE = "runtime_generation_audit_event";

    public static final int DEFAULT_CLEANUP_BATCH_SIZE = 1000;
    public static final int MIN_CLEANUP_BATCH_SIZE = 500;
    public static final int MAX_CLEANUP_BATCH_SIZE = 2000;

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
    private final int cleanupBatchSize;

    public AuditRetentionCleanupService(
            ManagementAuditEventCleanupRepository managementCleanup,
            RuntimeGenerationAuditEventCleanupRepository runtimeCleanup,
            AuditRetentionPurgeEvidenceWriter purgeEvidenceWriter,
            LegalHoldExemptionService legalHoldExemptionService,
            Clock clock,
            @Value("${docgen.audit.management-retention-days:90}") int managementRetentionDays,
            @Value("${docgen.audit.runtime-retention-days:365}") int runtimeRetentionDays,
            @Value("${docgen.audit.retention-enabled:true}") boolean retentionEnabled,
            @Value("${docgen.audit.cleanup-batch-size:1000}") int cleanupBatchSize
    ) {
        this.managementCleanup = managementCleanup;
        this.runtimeCleanup = runtimeCleanup;
        this.purgeEvidenceWriter = purgeEvidenceWriter;
        this.legalHoldExemptionService = legalHoldExemptionService;
        this.clock = clock;
        this.managementRetentionDays = requireRetentionDays(managementRetentionDays, "management");
        this.runtimeRetentionDays = requireRetentionDays(runtimeRetentionDays, "runtime");
        this.retentionEnabled = retentionEnabled;
        this.cleanupBatchSize = requireCleanupBatchSize(cleanupBatchSize, "docgen.audit.cleanup-batch-size");
    }

    public int purgeManagementAudit() {
        if (!retentionEnabled) {
            return 0;
        }
        Instant cutoff = cutoff(managementRetentionDays);
        int totalDeleted = 0;
        int totalSkipped = 0;
        int batches = 0;
        int page = 0;
        while (true) {
            Pageable pageable = cleanupPage(page);
            List<ManagementAuditEventEntity> candidates =
                    managementCleanup.findByEventAtBefore(cutoff, pageable);
            if (candidates.isEmpty()) {
                break;
            }
            batches++;
            List<ManagementAuditEventEntity> toDelete = new ArrayList<>();
            int skipped = 0;
            for (ManagementAuditEventEntity event : candidates) {
                if (legalHoldExemptionService.isManagementAuditExempt(event.getTemplateId(), event.getEventAt())) {
                    skipped++;
                    continue;
                }
                toDelete.add(event);
            }
            totalSkipped += skipped;
            if (!toDelete.isEmpty()) {
                // Repository @Transactional provides a short transaction per batch (PS-C2).
                managementCleanup.deleteAll(toDelete);
                totalDeleted += toDelete.size();
            } else {
                page++;
            }
        }
        if (totalDeleted > 0) {
            purgeEvidenceWriter.write(MANAGEMENT_TABLE, managementRetentionDays, cutoff, totalDeleted);
        }
        if (totalDeleted > 0 || totalSkipped > 0) {
            LOG.info(
                    "LR-D1 management audit retention purge: deleted={} skippedLegalHold={} batches={} "
                            + "retentionDays={} cutoff={}",
                    totalDeleted,
                    totalSkipped,
                    batches,
                    managementRetentionDays,
                    cutoff
            );
        }
        return totalDeleted;
    }

    public int purgeRuntimeAudit() {
        if (!retentionEnabled) {
            return 0;
        }
        Instant cutoff = cutoff(runtimeRetentionDays);
        int totalDeleted = 0;
        int totalSkipped = 0;
        int batches = 0;
        int page = 0;
        while (true) {
            Pageable pageable = cleanupPage(page);
            List<RuntimeGenerationAuditEventEntity> candidates =
                    runtimeCleanup.findByEventAtBefore(cutoff, pageable);
            if (candidates.isEmpty()) {
                break;
            }
            batches++;
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
            totalSkipped += skipped;
            if (!toDelete.isEmpty()) {
                runtimeCleanup.deleteAll(toDelete);
                totalDeleted += toDelete.size();
            } else {
                page++;
            }
        }
        if (totalDeleted > 0) {
            purgeEvidenceWriter.write(RUNTIME_TABLE, runtimeRetentionDays, cutoff, totalDeleted);
        }
        if (totalDeleted > 0 || totalSkipped > 0) {
            LOG.info(
                    "LR-D1 runtime audit retention purge: deleted={} skippedLegalHold={} batches={} "
                            + "retentionDays={} cutoff={}",
                    totalDeleted,
                    totalSkipped,
                    batches,
                    runtimeRetentionDays,
                    cutoff
            );
        }
        return totalDeleted;
    }

    public int cleanupBatchSize() {
        return cleanupBatchSize;
    }

    private Pageable cleanupPage(int page) {
        return PageRequest.of(
                page,
                cleanupBatchSize,
                Sort.by(Sort.Order.asc("eventAt"), Sort.Order.asc("id"))
        );
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

    static int requireCleanupBatchSize(int size, String propertyName) {
        if (size < MIN_CLEANUP_BATCH_SIZE || size > MAX_CLEANUP_BATCH_SIZE) {
            throw new IllegalStateException(
                    propertyName + " must be between " + MIN_CLEANUP_BATCH_SIZE
                            + " and " + MAX_CLEANUP_BATCH_SIZE + " (inclusive); got " + size
            );
        }
        return size;
    }
}
