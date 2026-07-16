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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InvocationRetentionCleanupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(InvocationRetentionCleanupScheduler.class);

    private final ApiInvocationRecordRepository repository;
    private final ObjectStoragePort objectStoragePort;
    private final LegalHoldExemptionService legalHoldExemptionService;

    public InvocationRetentionCleanupScheduler(
            ApiInvocationRecordRepository repository,
            ObjectStoragePort objectStoragePort,
            LegalHoldExemptionService legalHoldExemptionService
    ) {
        this.repository = repository;
        this.objectStoragePort = objectStoragePort;
        this.legalHoldExemptionService = legalHoldExemptionService;
    }

    // LR-B2: lockAtMostFor PT10M >> observed runtime (seconds); lockAtLeastFor PT20S << 1h interval.
    @Scheduled(fixedDelayString = "${docgen.invocation.cleanup-interval-ms:3600000}")
    @SchedulerLock(
            name = "invocation-retention-cleanup-artifacts",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    @Transactional
    public void cleanExpiredDocumentArtifacts() {
        List<ApiInvocationRecordEntity> expiredArtifacts =
                repository.findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(Instant.now());
        int cleaned = 0;
        int skipped = 0;
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
            } catch (Exception ex) {
                LOG.warn(
                        "Failed to clean expired invocation artifact {}: {}",
                        record.getInvocationExternalId(),
                        ex.getMessage()
                );
            }
        }
        if (cleaned > 0 || skipped > 0) {
            LOG.info(
                    "[InvocationCleanupScheduler] Cleaned {} expired invocation artifact(s); skipped {} under legal hold",
                    cleaned,
                    skipped
            );
        }
    }

    @Scheduled(fixedDelayString = "${docgen.invocation.cleanup-interval-ms:3600000}")
    @SchedulerLock(
            name = "invocation-retention-cleanup-records",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    @Transactional
    public void cleanExpiredRecords() {
        // ADR-0057 / ADR-0040: hard-delete expired invocation rows — parameters_storage is destroyed
        // with the row (same TTL; no orphan parameter blobs; no longer regenerable).
        // CE-G04: skip ACTIVE legal-hold protected rows before delete.
        List<ApiInvocationRecordEntity> expiredRecords = repository.findByRecordExpiresAtBefore(Instant.now());
        if (expiredRecords.isEmpty()) {
            return;
        }
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
        if (toDelete.isEmpty()) {
            if (skipped > 0) {
                LOG.info(
                        "[InvocationCleanupScheduler] Skipped {} expired invocation record(s) under legal hold",
                        skipped
                );
            }
            return;
        }
        repository.deleteAll(toDelete);
        LOG.info(
                "[InvocationCleanupScheduler] Deleted {} expired invocation record(s); skipped {} under legal hold",
                toDelete.size(),
                skipped
        );
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
