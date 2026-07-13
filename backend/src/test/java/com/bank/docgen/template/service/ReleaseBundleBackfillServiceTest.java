package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CE-K01 BDD-CE-K01-016..019: retroactive pin backfill — writes master_revision_id +
 * master_file_hash + pin_metadata_json(PINNED_RETROACTIVELY), is idempotent, and reports
 * (does not fake) rows whose master/revision is unavailable.
 */
@ExtendWith(MockitoExtension.class)
class ReleaseBundleBackfillServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MASTER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID REVISION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private TemplateRepository templateRepository;
    @Mock private MasterDocumentRepository masterDocumentRepository;
    @Mock private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock private ObjectStoragePort objectStoragePort;

    private ReleaseBundleBackfillService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new ReleaseBundleBackfillService(
                templateVersionRepository,
                templateRepository,
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                objectMapper
        );
    }

    @Test
    void backfill_writesRetroactivePin_bddK01_016() throws Exception {
        TemplateVersionEntity version = publishedVersion(null, null);
        TemplateEntity template = templateEntity();
        MasterDocumentEntity master = masterEntity(REVISION_ID);
        MasterRevisionLineEntity revision = revision(REVISION_ID, "masters/R.docx");
        byte[] docx = new byte[]{1, 2, 3, 4, 5};
        String expectedHash = sha256Hex(docx);

        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdNotNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of());
        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdIsNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(version));
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_ID, MASTER_ID))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get("masters/R.docx")).thenReturn(new ByteArrayInputStream(docx));

        ReleaseBundleBackfillResult result = service.runBackfill();

        assertThat(version.getMasterRevisionId()).isEqualTo(REVISION_ID);
        assertThat(version.getMasterFileHash()).isEqualTo(expectedHash);
        JsonNode meta = objectMapper.readTree(version.getPinMetadataJson());
        assertThat(meta.get("pinOrigin").asText()).isEqualTo("PINNED_RETROACTIVELY");
        assertThat(result.pinnedCount()).isEqualTo(1);
        assertThat(result.anomalies()).isEmpty();
        verify(templateVersionRepository).save(version);
    }

    @Test
    void backfill_idempotent_doesNotOverwrite_bddK01_017() {
        UUID existingRevision = UUID.fromString("11111111-2222-3333-4444-555555555555");
        TemplateVersionEntity alreadyPinned = publishedVersion(existingRevision, "existinghash");
        alreadyPinned.setPinMetadataJson("{\"pinOrigin\":\"PINNED_RETROACTIVELY\"}");

        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdNotNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(alreadyPinned));
        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdIsNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of());

        ReleaseBundleBackfillResult result = service.runBackfill();

        assertThat(alreadyPinned.getMasterRevisionId()).isEqualTo(existingRevision);
        assertThat(alreadyPinned.getMasterFileHash()).isEqualTo("existinghash");
        assertThat(result.pinnedCount()).isEqualTo(0);
        verify(templateVersionRepository, never()).save(any(TemplateVersionEntity.class));
        verify(objectStoragePort, never()).get(any());
    }

    @Test
    void backfill_masterDeleted_reportsAnomalyWithoutFaking_bddK01_019() {
        TemplateVersionEntity version = publishedVersion(null, null);
        TemplateEntity template = templateEntity();

        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdNotNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of());
        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdIsNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(version));
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.empty());

        ReleaseBundleBackfillResult result = service.runBackfill();

        assertThat(version.getMasterRevisionId()).isNull();
        assertThat(version.getMasterFileHash()).isNull();
        assertThat(result.pinnedCount()).isZero();
        assertThat(result.anomalies()).hasSize(1);
        assertThat(result.anomalies().get(0).templateId()).isEqualTo(TEMPLATE_ID);
        verify(templateVersionRepository, never()).save(any(TemplateVersionEntity.class));
    }

    @Test
    void backfill_revisionMissing_reportsAnomalyWithoutFaking_bddK01_019() {
        TemplateVersionEntity version = publishedVersion(null, null);
        TemplateEntity template = templateEntity();
        MasterDocumentEntity master = masterEntity(REVISION_ID);

        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdNotNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of());
        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdIsNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(version));
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_ID, MASTER_ID))
                .thenReturn(Optional.empty());

        ReleaseBundleBackfillResult result = service.runBackfill();

        assertThat(version.getMasterRevisionId()).isNull();
        assertThat(result.anomalies()).hasSize(1);
        verify(objectStoragePort, never()).get(any());
    }

    @Test
    void backfill_recomputesHashWhenMissingOnly_bddK01_017_partialFill() throws Exception {
        UUID existingRevision = UUID.fromString("11111111-2222-3333-4444-555555555555");
        TemplateVersionEntity partial = publishedVersion(existingRevision, null);
        partial.setPinMetadataJson("{\"pinOrigin\":\"PINNED_RETROACTIVELY\"}");
        MasterRevisionLineEntity revision = revision(existingRevision, "masters/R.docx");
        byte[] docx = new byte[]{9, 8, 7};
        String expectedHash = sha256Hex(docx);

        when(templateVersionRepository.findByLifecycleStatusAndMasterRevisionIdNotNullAndDeletedAtIsNull(
                TemplateLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(partial));
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(templateEntity()));
        // computeHashForExistingPin resolves the revision line directly — no masterDocument lookup.
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(existingRevision, MASTER_ID))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get("masters/R.docx")).thenReturn(new ByteArrayInputStream(docx));

        ReleaseBundleBackfillResult result = service.runBackfill();

        assertThat(partial.getMasterRevisionId()).isEqualTo(existingRevision);
        assertThat(partial.getMasterFileHash()).isEqualTo(expectedHash);
        assertThat(result.pinnedCount()).isEqualTo(1);
        verify(templateVersionRepository).save(partial);
    }

    private static TemplateVersionEntity publishedVersion(UUID revisionId, String fileHash) {
        TemplateVersionEntity v = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000009");
        v.setReleaseVersion("1.0.0");
        v.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        v.setMasterRevisionId(revisionId);
        v.setMasterFileHash(fileHash);
        return v;
    }

    private TemplateEntity templateEntity() {
        return new TemplateEntity(TEMPLATE_ID, "TPL-001", "RETAIL", "Sample", null, MASTER_ID, "10000009");
    }

    private MasterDocumentEntity masterEntity(UUID currentRevisionId) {
        MasterDocumentEntity master = new MasterDocumentEntity(
                MASTER_ID, "RETAIL", "Retail Master", "desc",
                "masters/live.docx", "master.docx", "10000009"
        );
        try {
            java.lang.reflect.Field f = master.getClass().getDeclaredField("currentRevisionLineId");
            f.setAccessible(true);
            f.set(master, currentRevisionId);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return master;
    }

    private MasterRevisionLineEntity revision(UUID id, String storageKey) {
        return new MasterRevisionLineEntity(
                id, MASTER_ID, storageKey, "master.docx",
                1, MasterDocumentStatus.APPROVED, 1, true, "initial", "10000009"
        );
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
