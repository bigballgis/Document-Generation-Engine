package com.bank.docgen.rendering.scheduler;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PreviewTempCleanupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewTempCleanupScheduler.class);

    private final PreviewRecordRepository previewRecordRepository;
    private final ObjectStoragePort objectStoragePort;

    public PreviewTempCleanupScheduler(
            PreviewRecordRepository previewRecordRepository,
            ObjectStoragePort objectStoragePort
    ) {
        this.previewRecordRepository = previewRecordRepository;
        this.objectStoragePort = objectStoragePort;
    }

    @Scheduled(fixedDelayString = "${docgen.preview.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanExpiredTempPreviews() {
        List<PreviewRecordEntity> expired = previewRecordRepository.findExpiredTempPreviews(Instant.now());
        int cleaned = 0;
        for (PreviewRecordEntity record : expired) {
            try {
                deleteArtifact(record.getTempStorageKey());
                deleteArtifact(record.getPdfArtifactStorageKey());
                deleteArtifact(record.getArtifactStorageKey());
                record.markTempArtifactCleaned();
                record.markExpired();
                previewRecordRepository.save(record);
                cleaned++;
            } catch (Exception ex) {
                LOG.warn("Failed to clean expired preview record {}: {}", record.getId(), ex.getMessage());
            }
        }
        if (cleaned > 0) {
            LOG.info("[PreviewCleanupScheduler] Cleaned {} expired preview record(s)", cleaned);
        }
    }

    private void deleteArtifact(String storageKey) {
        if (storageKey != null && !storageKey.isBlank()) {
            try {
                if (objectStoragePort.exists(storageKey)) {
                    objectStoragePort.delete(storageKey);
                }
            } catch (Exception ex) {
                LOG.warn("Failed to delete artifact '{}': {}", storageKey, ex.getMessage());
            }
        }
    }
}
