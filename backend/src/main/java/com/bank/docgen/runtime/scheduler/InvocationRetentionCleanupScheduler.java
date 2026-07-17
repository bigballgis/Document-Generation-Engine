package com.bank.docgen.runtime.scheduler;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.legalhold.service.LegalHoldExemptionService;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ADR-0040 / LR-B2 invocation retention cleanup.
 * PRR-A01: Pageable / LIMIT batch loads; short per-batch deletes (no outer long transaction).
 * CE-G04: skip ACTIVE legal-hold protected rows before delete / artifact cleanup.
 */
@Component
public class InvocationRetentionCleanupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(InvocationRetentionCleanupScheduler.class);

    private static final int MIN_CLEANUP_BATCH_SIZE = 500;
    private static final int MAX_CLEANUP_BATCH_SIZE = 2000;

    private final ApiInvocationRecordRepository repository;
    private final ObjectStoragePort objectStoragePort;
    private final LegalHoldExemptionService legalHoldExemptionService;
    private final int cleanupBatchSize;

    public InvocationRetentionCleanupScheduler(
            ApiInvocationRecordRepository repository,
            ObjectStoragePort objectStoragePort,
            LegalHoldExemptionService legalHoldExemptionService,
            @Value("${docgen.invocation.cleanup-batch-size:1000}") int cleanupBatchSize
    ) {
        this.repository = repository;
        this.objectStoragePort = objectStoragePort;
        this.legalHoldExemptionService = legalHoldExemptionService;
        this.cleanupBatchSize = requireCleanupBatchSize(cleanupBatchSize);
    }

    // LR-B2: lockAtMostFor PT10M >> observed runtime (seconds); lockAtLeastFor PT20S << 1h interval.
    @Scheduled(fixedDelayString = "${docgen.invocation.cleanup-interval-ms:3600000}")
    @SchedulerLock(
            name = "invocation-retention-cleanup-artifacts",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    public void cleanExpiredDocumentArtifacts() {
        int cleaned = 0;
        int skipped = 0;
        int batches = 0;
        int page = 0;
        Instant now = Instant.now();
        while (true) {
            Pageable pageable = PageRequest.of(
                    page,
                    cleanupBatchSize,
                    Sort.by(Sort.Order.asc("documentExpiresAt"), Sort.Order.asc("id"))
            );
            List<ApiInvocationRecordEntity> expiredArtifacts =
                    repository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(now, pageable);
            if (expiredArtifacts.isEmpty()) {
                break;
            }
            batches++;
            int batchCleaned = 0;
            for (ApiInvocationRecordEntity record : expiredArtifacts) {
                if (legalHoldExemptionService.isInvocationExempt(
                        record.getTemplateId(),
                        record.getInvocationExternalId(),
                        record.getCreatedAt()
                )) {
                    skipped++;
                    continue;
                }
                try {
                    deleteArtifact(record.getArtifactStorageKey());
                    record.markDocumentArtifactCleaned(Instant.now());
                    repository.save(record);
                    cleaned++;
                    batchCleaned++;
                } catch (Exception ex) {
                    LOG.warn(
                            "Failed to clean expired invocation artifact {}: {}",
                            record.getInvocationExternalId(),
                            ex.getMessage()
                    );
                }
            }
            if (batchCleaned == 0) {
                page++;
            }
        }
        if (cleaned > 0 || skipped > 0) {
            LOG.info(
                    "[InvocationCleanupScheduler] Cleaned {} expired invocation artifact(s); "
                            + "skippedLegalHold={} batches={}",
                    cleaned,
                    skipped,
                    batches
            );
        }
    }

    @Scheduled(fixedDelayString = "${docgen.invocation.cleanup-interval-ms:3600000}")
    @SchedulerLock(
            name = "invocation-retention-cleanup-records",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    public void cleanExpiredRecords() {
        // ADR-0057 / ADR-0040: hard-delete expired invocation rows — parameters_storage is destroyed
        // with the row (same TTL; no orphan parameter blobs; no longer regenerable).
        // CE-G04: skip ACTIVE legal-hold protected rows before delete.
        int totalDeleted = 0;
        int totalSkipped = 0;
        int batches = 0;
        int page = 0;
        Instant now = Instant.now();
        while (true) {
            Pageable pageable = cleanupPage(page);
            List<ApiInvocationRecordEntity> expiredRecords =
                    repository.findByRecordExpiresAtBefore(now, pageable);
            if (expiredRecords.isEmpty()) {
                break;
            }
            batches++;
            List<ApiInvocationRecordEntity> toDelete = new ArrayList<>();
            int skipped = 0;
            for (ApiInvocationRecordEntity record : expiredRecords) {
                if (legalHoldExemptionService.isInvocationExempt(
                        record.getTemplateId(),
                        record.getInvocationExternalId(),
                        record.getCreatedAt()
                )) {
                    skipped++;
                    continue;
                }
                toDelete.add(record);
            }
            totalSkipped += skipped;
            if (!toDelete.isEmpty()) {
                repository.deleteAll(toDelete);
                totalDeleted += toDelete.size();
            } else {
                page++;
            }
        }
        if (totalDeleted > 0 || totalSkipped > 0) {
            LOG.info(
                    "[InvocationCleanupScheduler] Deleted {} expired invocation record(s); "
                            + "skippedLegalHold={} batches={}",
                    totalDeleted,
                    totalSkipped,
                    batches
            );
        }
    }

    public int cleanupBatchSize() {
        return cleanupBatchSize;
    }

    private Pageable cleanupPage(int page) {
        return PageRequest.of(
                page,
                cleanupBatchSize,
                Sort.by(Sort.Order.asc("recordExpiresAt"), Sort.Order.asc("id"))
        );
    }

    private static int requireCleanupBatchSize(int size) {
        if (size < MIN_CLEANUP_BATCH_SIZE || size > MAX_CLEANUP_BATCH_SIZE) {
            throw new IllegalStateException(
                    "docgen.invocation.cleanup-batch-size must be between "
                            + MIN_CLEANUP_BATCH_SIZE + " and " + MAX_CLEANUP_BATCH_SIZE
                            + " (inclusive); got " + size
            );
        }
        return size;
    }

    private void deleteArtifact(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            if (objectStoragePort.exists(storageKey)) {
                objectStoragePort.delete(storageKey);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to delete artifact '{}': {}", storageKey, ex.getMessage());
        }
    }
}
