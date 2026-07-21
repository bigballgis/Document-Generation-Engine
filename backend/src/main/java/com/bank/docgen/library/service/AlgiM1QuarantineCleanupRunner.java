package com.bank.docgen.library.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.infrastructure.storage.ObjectStorageException;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ALGI-M1 post-Flyway cleanup: delete legacy bare (+ namespaced) resolvable MinIO keys for
 * quarantined rows and emit {@code ASSET_LIBRARY_MIGRATE_QUARANTINE}. Idempotent via
 * {@code object_purge_completed_at}.
 */
@Component
@Order(12)
public class AlgiM1QuarantineCleanupRunner implements ApplicationRunner {

    static final String MIGRATION_ID = "ALGI-M1";

    private static final Logger LOG = LoggerFactory.getLogger(AlgiM1QuarantineCleanupRunner.class);

    private final LibraryAssetRepository repository;
    private final ObjectStoragePort objectStoragePort;
    private final ManagementAuditRecorder auditRecorder;
    private final Clock clock;

    public AlgiM1QuarantineCleanupRunner(
            LibraryAssetRepository repository,
            ObjectStoragePort objectStoragePort,
            ManagementAuditRecorder auditRecorder,
            Clock clock
    ) {
        this.repository = repository;
        this.objectStoragePort = objectStoragePort;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        purgePendingQuarantines();
    }

    void purgePendingQuarantines() {
        List<LibraryAssetEntity> pending =
                repository.findByMigratedQuarantineAtIsNotNullAndObjectPurgeCompletedAtIsNullAndDeletedAtIsNull();
        for (LibraryAssetEntity entity : pending) {
            purgeOne(entity);
        }
    }

    private void purgeOne(LibraryAssetEntity entity) {
        String groupCode = entity.getGroupCode();
        String assetKey = entity.getAssetKey();
        try {
            for (String key : AssetLibraryStorageKeys.bareResolvableKeys(assetKey)) {
                ensureObjectRemoved(key);
            }
            for (String key : AssetLibraryStorageKeys.namespacedResolvableKeys(groupCode, assetKey)) {
                ensureObjectRemoved(key);
            }
        } catch (ObjectStorageException ex) {
            LOG.error(
                    "ALGI-M1 object purge failed for groupCode={} assetKey={}: {}",
                    groupCode,
                    assetKey,
                    ex.getMessage()
            );
            throw ex;
        }
        Instant now = clock.instant();
        entity.markObjectPurgeCompleted(now);
        repository.save(entity);
        auditRecorder.recordAssetLibraryMigrateQuarantine(groupCode, assetKey, MIGRATION_ID);
        LOG.info("ALGI-M1 quarantined asset purged groupCode={} assetKey={}", groupCode, assetKey);
    }

    private void ensureObjectRemoved(String objectKey) {
        objectStoragePort.delete(objectKey);
        if (objectStoragePort.exists(objectKey)) {
            throw new ObjectStorageException(
                    "Object still present after delete",
                    new IllegalStateException("exists returned true after delete")
            );
        }
    }
}
