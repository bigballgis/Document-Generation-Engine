package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.rendering.ArtifactSpoolService;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.GeneratedArtifactSizeGuard;
import com.bank.docgen.rendering.SpooledArtifact;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.VersionFidelityWarningService;
import com.bank.docgen.runtime.metrics.GenerationMetrics;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CE-K01 BDD-CE-K01-006..009 + 014: runtime assembly reads the pinned master revision
 * storage key (not the live master) for PUBLISHED versions, fail-closes when the pin
 * is missing, and stays stable after the master's current revision is superseded.
 */
@ExtendWith(MockitoExtension.class)
class DocumentGenerationAssemblyPinningTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MASTER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID REVISION_R1 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID REVISION_R2 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private AnchorBindingRepository anchorBindingRepository;
    @Mock private MasterDocumentRepository masterDocumentRepository;
    @Mock private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock private ObjectStoragePort objectStoragePort;
    @Mock private DocxAssembler docxAssembler;
    @Mock private DocumentArtifactPipeline documentArtifactPipeline;
    @Mock private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock private RenderProfileService renderProfileService;
    @Mock private VersionFidelityWarningService versionFidelityWarningService;

    private DocumentGenerationEngine engine;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        engine = new DocumentGenerationEngine(
                templateVersionRepository,
                anchorBindingRepository,
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                docxAssembler,
                documentArtifactPipeline,
                contentModuleReferenceService,
                renderProfileService,
                versionFidelityWarningService,
                new GenerationMetrics(new SimpleMeterRegistry())
        );
        template = new TemplateEntity(TEMPLATE_ID, "TPL-001", "RETAIL", "Sample", null, MASTER_ID, "10000001");
    }

    @Test
    void publishedVersion_readsPinnedRevisionStorageKey_notLiveMaster_bddK01_006() throws Exception {
        TemplateVersionEntity version = publishedPinnedVersion(REVISION_R1);
        MasterRevisionLineEntity r1 = revision(REVISION_R1, "masters/R1.docx", true);
        MasterDocumentEntity master = masterEntity(REVISION_R1, "masters/live.docx");
        byte[] docx = new byte[]{1, 2, 3};
        stubAssemblySuccess(version, r1, master, "masters/R1.docx", docx);

        engine.generate(template, "1.0.0", java.util.Map.of(), "DOCX",
                new EncryptionOptionsView(false, null, null, null));

        verify(objectStoragePort).get("masters/R1.docx");
        verify(masterDocumentRepository, org.mockito.Mockito.never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void publishedVersion_afterMasterSuperseded_stillReadsPinnedR1_bddK01_007_014() throws Exception {
        TemplateVersionEntity version = publishedPinnedVersion(REVISION_R1);
        // Current revision is now R2 (R1 superseded) — release generation must still read R1.
        MasterRevisionLineEntity r1 = revision(REVISION_R1, "masters/R1.docx", false);
        MasterDocumentEntity master = masterEntity(REVISION_R2, "masters/R2.docx");
        byte[] docx = new byte[]{1, 2, 3};
        stubAssemblySuccess(version, r1, master, "masters/R1.docx", docx);

        engine.generate(template, "1.0.0", java.util.Map.of(), "DOCX",
                new EncryptionOptionsView(false, null, null, null));

        verify(objectStoragePort).get("masters/R1.docx");
        verify(objectStoragePort, org.mockito.Mockito.never()).get("masters/R2.docx");
    }

    @Test
    void publishedVersion_pinnedRevisionMissing_failClosed_bddK01_008() {
        TemplateVersionEntity version = publishedPinnedVersion(REVISION_R1);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_R1, MASTER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> engine.generate(
                template, "1.0.0", java.util.Map.of(), "DOCX",
                new EncryptionOptionsView(false, null, null, null)))
                .isInstanceOf(com.bank.docgen.rendering.RenderingOperationException.class)
                .satisfies(ex -> assertThat(((com.bank.docgen.rendering.RenderingOperationException) ex).messageKey())
                        .isEqualTo("api.error.rendering.pinnedMasterUnavailable"));
        verify(objectStoragePort, org.mockito.Mockito.never()).get(anyString());
    }

    @Test
    void publishedVersion_nullMasterRevisionId_failClosed_bddK01_008() {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setReleaseVersion("1.0.0");
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        // masterRevisionId intentionally null (un-migrated / anomalous)
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));

        assertThatThrownBy(() -> engine.generate(
                template, "1.0.0", java.util.Map.of(), "DOCX",
                new EncryptionOptionsView(false, null, null, null)))
                .isInstanceOf(com.bank.docgen.rendering.RenderingOperationException.class)
                .satisfies(ex -> assertThat(((com.bank.docgen.rendering.RenderingOperationException) ex).messageKey())
                        .isEqualTo("api.error.rendering.pinnedMasterUnavailable"));
    }

    @Test
    void publishedVersion_pinnedRevisionStorage404_failClosed_bddK01_008() {
        TemplateVersionEntity version = publishedPinnedVersion(REVISION_R1);
        MasterRevisionLineEntity r1 = revision(REVISION_R1, "masters/R1.docx", true);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_R1, MASTER_ID))
                .thenReturn(Optional.of(r1));
        when(objectStoragePort.get("masters/R1.docx")).thenThrow(new RuntimeException("404"));

        assertThatThrownBy(() -> engine.generate(
                template, "1.0.0", java.util.Map.of(), "DOCX",
                new EncryptionOptionsView(false, null, null, null)))
                .isInstanceOf(com.bank.docgen.rendering.RenderingOperationException.class)
                .satisfies(ex -> assertThat(((com.bank.docgen.rendering.RenderingOperationException) ex).messageKey())
                        .isEqualTo("api.error.rendering.pinnedMasterUnavailable"));
    }

    @Test
    void draftVersion_followsLiveMaster_bddK01_020_previewUnpinned() throws Exception {
        // Non-PUBLISHED versions keep the legacy live-master path (preview / dev).
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        MasterDocumentEntity master = masterEntity(REVISION_R1, "masters/dev-live.docx");
        byte[] docx = new byte[]{1};
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID)).thenReturn(List.of());
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(java.util.Map.of());
        when(objectStoragePort.get("masters/dev-live.docx")).thenReturn(new ByteArrayInputStream(docx));
        when(docxAssembler.assembleStructured(any(), any(), any(), any())).thenReturn(docx);
        when(renderProfileService.resolveEffectiveProfile(any(), any())).thenReturn(renderProfile());
        stubFinalizeArtifact(docx);
        when(versionFidelityWarningService.resolveWarningCodes(any(), eq(MASTER_ID))).thenReturn(List.of());

        engine.generate(template, "1.0.0", java.util.Map.of(), "DOCX",
                new EncryptionOptionsView(false, null, null, null));

        verify(objectStoragePort).get("masters/dev-live.docx");
        verify(masterRevisionLineRepository, org.mockito.Mockito.never())
                .findByIdAndMasterIdAndDeletedAtIsNull(any(), any());
    }

    private void stubAssemblySuccess(TemplateVersionEntity version, MasterRevisionLineEntity r1,
                                     MasterDocumentEntity master, String storageKey, byte[] docx) throws Exception {
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(r1.getId(), MASTER_ID))
                .thenReturn(Optional.of(r1));
        // Master lookup should NOT be invoked for PUBLISHED versions; leave it unstubbed so
        // Mockito would NPE-fail the test if the assembly accidentally fell back to live master.
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID)).thenReturn(List.of());
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(java.util.Map.of());
        when(objectStoragePort.get(storageKey)).thenReturn(new ByteArrayInputStream(docx));
        when(docxAssembler.assembleStructured(any(), any(), any(), any())).thenReturn(docx);
        when(renderProfileService.resolveEffectiveProfile(any(), any())).thenReturn(renderProfile());
        stubFinalizeArtifact(docx);
        when(versionFidelityWarningService.resolveWarningCodes(any(), eq(MASTER_ID))).thenReturn(List.of());
        // Reference master to avoid unused-var warnings in callers; the test asserts it is never *read*.
        assertThat(master).isNotNull();
    }

    private void stubFinalizeArtifact(byte[] finalBytes) throws Exception {
        GeneratedArtifactSizeGuard sizeGuard = new GeneratedArtifactSizeGuard(
                new com.bank.docgen.infrastructure.config.DocgenRenderingProperties()
        );
        SpooledArtifact spooled = new ArtifactSpoolService(sizeGuard).spool(finalBytes);
        DocumentArtifactPipeline.GeneratedArtifact artifact = new DocumentArtifactPipeline.GeneratedArtifact(
                spooled,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "output.docx"
        );
        when(documentArtifactPipeline.finalizeArtifact(any(), eq("DOCX"), any(), any())).thenReturn(artifact);
        doAnswer(inv -> null).when(objectStoragePort).put(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    private static RenderProfile renderProfile() {
        return new RenderProfile(
                "rp-v1", "MASTER_CATALOG_LOCKED", "CONTROLLED_MULTILEVEL", "REPEAT_HEADER",
                "PROPORTIONAL_FIT", "SEMANTIC_FIDELITY", "BLOCKERS_PREVENT_PUBLISH", false
        );
    }

    private static TemplateVersionEntity publishedPinnedVersion(UUID revisionId) {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setReleaseVersion("1.0.0");
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        version.setMasterRevisionId(revisionId);
        version.setMasterFileHash("deadbeef".repeat(8));
        return version;
    }

    private static MasterDocumentEntity masterEntity(UUID currentRevisionId, String liveStorageKey) {
        MasterDocumentEntity master = new MasterDocumentEntity(
                MASTER_ID, "RETAIL", "Retail Master", "desc",
                liveStorageKey, "master.docx", "10000001"
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

    private static MasterRevisionLineEntity revision(UUID id, String storageKey, boolean current) {
        return new MasterRevisionLineEntity(
                id, MASTER_ID, storageKey, "master.docx",
                1, MasterDocumentStatus.APPROVED, current ? 1 : 2, current, "change", "10000001"
        );
    }
}
