package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditRetentionPurgeEvidenceWriterTest {

    private static final Instant NOW = Instant.parse("2026-07-11T12:00:00Z");

    @Mock
    private ManagementAuditEventRepository repository;

    private AuditRetentionPurgeEvidenceWriter writer;

    @BeforeEach
    void setUp() {
        writer = new AuditRetentionPurgeEvidenceWriter(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void write_recordsPlatformEvidenceWithoutGroupOrSecrets() {
        Instant cutoff = Instant.parse("2026-04-12T12:00:00Z");

        writer.write("management_audit_event", 90, cutoff, 4);

        ArgumentCaptor<ManagementAuditEventEntity> captor = ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        ManagementAuditEventEntity saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(AuditRetentionPurgeEvidenceWriter.AUDIT_RETENTION_PURGE);
        assertThat(saved.getActorUsername()).isEqualTo(AuditRetentionPurgeEvidenceWriter.ACTOR_USERNAME);
        assertThat(saved.getEventAt()).isEqualTo(NOW);
        assertThat(saved.getGroupCode()).isNull();
        assertThat(saved.getTemplateId()).isNull();
        assertThat(saved.getCredentialId()).isNull();
        assertThat(saved.getStatusSummary()).contains("management_audit_event");
        assertThat(saved.getStatusSummary()).contains("retentionDays=90");
        assertThat(saved.getStatusSummary()).contains("deletedCount=4");
        assertThat(saved.getStatusSummary()).contains(cutoff.toString());
    }
}
