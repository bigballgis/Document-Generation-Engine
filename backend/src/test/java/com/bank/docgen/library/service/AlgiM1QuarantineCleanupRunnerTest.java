package com.bank.docgen.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlgiM1QuarantineCleanupRunnerTest {

    private static final Instant NOW = Instant.parse("2026-07-22T00:00:00Z");

    @Mock
    private LibraryAssetRepository repository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private AlgiM1QuarantineCleanupRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AlgiM1QuarantineCleanupRunner(
                repository,
                objectStoragePort,
                auditRecorder,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void migrate_legacyRow_quarantineDisabled_bareKeyRemoved() {
        LibraryAssetEntity quarantined = new LibraryAssetEntity(
                "CORP",
                "IMG-ALGI-012",
                AssetLibraryAssetClass.IMAGE,
                AssetLibraryAssetStatus.DISABLED,
                "image/png",
                12L,
                "a".repeat(64),
                "img.png",
                "legacy",
                NOW.minusSeconds(3600)
        );
        // Reflect migration stamp via markObjectPurgeCompleted absence; entity already DISABLED/CORP.
        when(repository.findByMigratedQuarantineAtIsNotNullAndObjectPurgeCompletedAtIsNullAndDeletedAtIsNull())
                .thenReturn(List.of(quarantined));
        when(objectStoragePort.exists(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Force migratedQuarantineAt non-null via reflection-free path: repository returns the entity
        // as pending; entity needs migratedQuarantineAt set — use package field via constructor only.
        // For this unit test we simulate pending list membership (query already filtered).
        setMigratedQuarantineAt(quarantined, NOW.minusSeconds(10));

        runner.purgePendingQuarantines();

        ArgumentCaptor<String> deleted = ArgumentCaptor.forClass(String.class);
        verify(objectStoragePort, org.mockito.Mockito.atLeastOnce()).delete(deleted.capture());
        assertThat(deleted.getAllValues()).contains("IMG-ALGI-012", "CORP/IMG-ALGI-012");
        verify(auditRecorder).recordAssetLibraryMigrateQuarantine(
                eq("CORP"), eq("IMG-ALGI-012"), eq(AlgiM1QuarantineCleanupRunner.MIGRATION_ID)
        );
        assertThat(quarantined.getObjectPurgeCompletedAt()).isEqualTo(NOW);
        assertThat(quarantined.getStatus()).isEqualTo(AssetLibraryAssetStatus.DISABLED);
    }

    private static void setMigratedQuarantineAt(LibraryAssetEntity entity, Instant at) {
        try {
            var field = LibraryAssetEntity.class.getDeclaredField("migratedQuarantineAt");
            field.setAccessible(true);
            field.set(entity, at);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
