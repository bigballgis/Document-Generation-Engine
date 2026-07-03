package com.bank.docgen.rendering.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreviewTempCleanupSchedulerTest {

    @Mock
    private PreviewRecordRepository previewRecordRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;

    private PreviewTempCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PreviewTempCleanupScheduler(previewRecordRepository, objectStoragePort);
    }

    @Test
    void cleanExpired_withExpiredRecord_deletesArtifactAndMarksExpired() {
        PreviewRecordEntity record = expiredPreviewRecord("preview-temp/abc123.docx");
        when(previewRecordRepository.findExpiredTempPreviews(any(Instant.class)))
                .thenReturn(List.of(record));
        when(objectStoragePort.exists(anyString())).thenReturn(true);
        when(previewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.cleanExpiredTempPreviews();

        verify(objectStoragePort).delete("preview-temp/abc123.docx");
        ArgumentCaptor<PreviewRecordEntity> captor = ArgumentCaptor.forClass(PreviewRecordEntity.class);
        verify(previewRecordRepository).save(captor.capture());
        assertThat(captor.getValue().isTempArtifactCleaned()).isTrue();
        assertThat(captor.getValue().getStatus()).isEqualTo(PreviewStatus.EXPIRED);
    }

    @Test
    void cleanExpired_withNoExpiredRecords_doesNothing() {
        when(previewRecordRepository.findExpiredTempPreviews(any(Instant.class)))
                .thenReturn(List.of());

        scheduler.cleanExpiredTempPreviews();

        verify(objectStoragePort, never()).delete(anyString());
        verify(previewRecordRepository, never()).save(any());
    }

    @Test
    void cleanExpired_storageDeleteFails_continuesProcessing() {
        PreviewRecordEntity record = expiredPreviewRecord("preview-temp/broken.docx");
        when(previewRecordRepository.findExpiredTempPreviews(any(Instant.class)))
                .thenReturn(List.of(record));
        when(objectStoragePort.exists("preview-temp/broken.docx")).thenThrow(new RuntimeException("Storage error"));

        // Should not throw - errors per record are caught
        scheduler.cleanExpiredTempPreviews();
    }

    private PreviewRecordEntity expiredPreviewRecord(String tempStorageKey) {
        PreviewRecordEntity record = new PreviewRecordEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "DOCX", "hash", "author", "TDS-001", null
        );
        record.markProcessing();
        record.markSucceeded(tempStorageKey, null, null);
        record.setExpiresAt(Instant.now().minusSeconds(3600));
        record.setTempStorageKey(tempStorageKey);
        return record;
    }
}
