package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.runtime.metrics.GenerationMetrics;
import com.bank.docgen.template.service.VersionFidelityWarningService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.ArtifactSpoolService;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.GeneratedArtifactSizeGuard;
import com.bank.docgen.rendering.SpooledArtifact;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.port.VariableSchemaValidationPort;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.VariableComputeService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentGenerationEngineTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MASTER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private com.bank.docgen.master.persistence.MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private DocxAssembler docxAssembler;
    @Mock
    private DocumentArtifactPipeline documentArtifactPipeline;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private RenderProfileService renderProfileService;
    @Mock
    private VersionFidelityWarningService versionFidelityWarningService;
    @Mock
    private VariableComputeService variableComputeService;
    @Mock
    private VariableSchemaValidationPort variableSchemaValidationPort;

    private GenerationMetrics generationMetrics;
    private DocumentGenerationEngine engine;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        generationMetrics = new GenerationMetrics(new SimpleMeterRegistry());
        lenient().when(variableComputeService.applyCompute(any(), any(), any())).thenAnswer(invocation -> {
            java.util.Map<String, Object> input = invocation.getArgument(1);
            return input == null ? java.util.Map.of() : new java.util.LinkedHashMap<>(input);
        });
        engine = new DocumentGenerationEngine(
                templateVersionRepository,
                anchorBindingRepository,
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                docxAssembler,
                documentArtifactPipeline,
                contentModuleReferenceService,
                org.mockito.Mockito.mock(com.bank.docgen.template.service.CompositionInclusionRuleService.class),
                renderProfileService,
                versionFidelityWarningService,
                variableComputeService,
                variableSchemaValidationPort,
                new com.bank.docgen.rendering.PaginationDeltaFidelitySupport(
                        new com.bank.docgen.infrastructure.config.DocgenRenderingProperties(),
                        new com.bank.docgen.rendering.PdfPageCountReader()
                ),
                mockDocumentBrandResolveService(),
                generationMetrics
        );
        template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                MASTER_ID,
                "10000001"
        );
    }

    @Test
    void generate_uploadsFromSpoolAndReturnsNullArtifactBytes() throws Exception {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setReleaseVersion("1.0.0");
        MasterDocumentEntity master = new MasterDocumentEntity(
                MASTER_ID,
                "RETAIL",
                "Retail Master",
                "Retail master document",
                "masters/master.docx",
                "master.docx",
                "10000001"
        );
        byte[] docx = new byte[]{1, 2, 3};
        byte[] finalBytes = new byte[]{9, 8, 7};
        GeneratedArtifactSizeGuard sizeGuard = new GeneratedArtifactSizeGuard(
                new com.bank.docgen.infrastructure.config.DocgenRenderingProperties()
        );
        SpooledArtifact spooled = new ArtifactSpoolService(sizeGuard).spool(finalBytes);
        DocumentArtifactPipeline.GeneratedArtifact artifact = new DocumentArtifactPipeline.GeneratedArtifact(
                spooled,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "output.docx"
        );

        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID)).thenReturn(List.of());
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(java.util.Map.of());
        when(objectStoragePort.get("masters/master.docx")).thenReturn(new ByteArrayInputStream(docx));
        when(docxAssembler.assembleStructured(any(), any(), any(), any(), any())).thenReturn(docx);
        when(renderProfileService.resolveEffectiveProfile(any(), any()))
                .thenReturn(new com.bank.docgen.sharedkernel.document.RenderProfile(
                        "rp-v1",
                        "MASTER_CATALOG_LOCKED",
                        "CONTROLLED_MULTILEVEL",
                        "REPEAT_HEADER",
                        "PROPORTIONAL_FIT",
                        "SEMANTIC_FIDELITY",
                        "BLOCKERS_PREVENT_PUBLISH", false, com.bank.docgen.sharedkernel.document.PdfArchivalProfile.NONE));
        when(documentArtifactPipeline.finalizeArtifact(any(), eq("DOCX"), any(), any())).thenReturn(artifact);
        when(versionFidelityWarningService.resolveWarningCodes(any(), eq(MASTER_ID))).thenReturn(List.of());

        byte[][] uploadedBytesHolder = new byte[1][];
        doAnswer(invocation -> {
            try (InputStream stream = invocation.getArgument(1)) {
                uploadedBytesHolder[0] = stream.readAllBytes();
            }
            return null;
        }).when(objectStoragePort).put(
                anyString(),
                any(InputStream.class),
                anyLong(),
                anyString()
        );

        DocumentGenerationEngine.GeneratedDocument generated = engine.generate(
                template,
                "1.0.0",
                java.util.Map.of(),
                "DOCX",
                new EncryptionOptionsView(false, null, null, null)
        );

        assertThat(generated.artifactBytes()).isNull();
        assertThat(generated.storageKey()).endsWith("/output.docx");
        assertThat(uploadedBytesHolder[0]).containsExactly(9, 8, 7);
        verify(objectStoragePort).put(
                eq(generated.storageKey()),
                any(InputStream.class),
                eq((long) finalBytes.length),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
        assertThat(Files.exists(spooled.path())).isFalse();
    }

    @Test
    void generate_usesCachedFidelityWarningsForPublishedVersion() throws Exception {
        UUID pinnedRevisionId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setReleaseVersion("1.0.0");
        version.setLifecycleStatus(com.bank.docgen.template.domain.TemplateLifecycleStatus.PUBLISHED);
        version.setFidelityWarningCodesJson("[\"CACHED_WARNING\"]");
        version.setMasterRevisionId(pinnedRevisionId);
        com.bank.docgen.master.persistence.MasterRevisionLineEntity pinnedRevision =
                new com.bank.docgen.master.persistence.MasterRevisionLineEntity(
                        pinnedRevisionId, MASTER_ID, "masters/master.docx", "master.docx",
                        1, com.bank.docgen.master.domain.MasterDocumentStatus.APPROVED,
                        1, true, "initial", "10000001"
                );
        byte[] docx = new byte[]{1, 2, 3};
        byte[] finalBytes = new byte[]{9, 8, 7};
        GeneratedArtifactSizeGuard sizeGuard = new GeneratedArtifactSizeGuard(
                new com.bank.docgen.infrastructure.config.DocgenRenderingProperties()
        );
        SpooledArtifact spooled = new ArtifactSpoolService(sizeGuard).spool(finalBytes);
        DocumentArtifactPipeline.GeneratedArtifact artifact = new DocumentArtifactPipeline.GeneratedArtifact(
                spooled,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "output.docx"
        );

        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(pinnedRevisionId, MASTER_ID))
                .thenReturn(Optional.of(pinnedRevision));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID)).thenReturn(List.of());
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(java.util.Map.of());
        when(objectStoragePort.get("masters/master.docx")).thenReturn(new ByteArrayInputStream(docx));
        when(docxAssembler.assembleStructured(any(), any(), any(), any(), any())).thenReturn(docx);
        when(renderProfileService.resolveEffectiveProfile(any(), any()))
                .thenReturn(new com.bank.docgen.sharedkernel.document.RenderProfile(
                        "rp-v1",
                        "MASTER_CATALOG_LOCKED",
                        "CONTROLLED_MULTILEVEL",
                        "REPEAT_HEADER",
                        "PROPORTIONAL_FIT",
                        "SEMANTIC_FIDELITY",
                        "BLOCKERS_PREVENT_PUBLISH", false, com.bank.docgen.sharedkernel.document.PdfArchivalProfile.NONE));
        when(documentArtifactPipeline.finalizeArtifact(any(), eq("DOCX"), any(), any())).thenReturn(artifact);
        when(versionFidelityWarningService.resolveWarningCodes(version, MASTER_ID))
                .thenReturn(List.of("CACHED_WARNING"));
        doAnswer(invocation -> null).when(objectStoragePort).put(
                anyString(),
                any(InputStream.class),
                anyLong(),
                anyString()
        );

        DocumentGenerationEngine.GeneratedDocument generated = engine.generate(
                template,
                "1.0.0",
                java.util.Map.of(),
                "DOCX",
                new EncryptionOptionsView(false, null, null, null)
        );

        assertThat(generated.fidelityWarningCodes()).contains("CACHED_WARNING");
        assertThat(generated.resolvedDocumentBrandCode()).isEqualTo("PLATFORM_DEFAULT");
        verify(versionFidelityWarningService).resolveWarningCodes(version, MASTER_ID);
    }

    @Test
    void generate_propagatesOoxmlValidationFailedFromAssembler() throws Exception {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setReleaseVersion("1.0.0");
        MasterDocumentEntity master = new MasterDocumentEntity(
                MASTER_ID,
                "RETAIL",
                "Retail Master",
                "Retail master document",
                "masters/master.docx",
                "master.docx",
                "10000001"
        );

        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID)).thenReturn(List.of());
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(java.util.Map.of());
        when(objectStoragePort.get("masters/master.docx")).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(renderProfileService.resolveEffectiveProfile(any(), any()))
                .thenReturn(new com.bank.docgen.sharedkernel.document.RenderProfile(
                        "rp-v1",
                        "MASTER_CATALOG_LOCKED",
                        "CONTROLLED_MULTILEVEL",
                        "REPEAT_HEADER",
                        "PROPORTIONAL_FIT",
                        "SEMANTIC_FIDELITY",
                        "BLOCKERS_PREVENT_PUBLISH", false, com.bank.docgen.sharedkernel.document.PdfArchivalProfile.NONE));
        when(docxAssembler.assembleStructured(any(), any(), any(), any(), any())).thenThrow(
                new DocxAssemblyException(
                        ApiErrorCodes.OOXML_VALIDATION_FAILED,
                        ApiErrorCategories.RENDERING,
                        "api.error.rendering.ooxmlValidationFailed",
                        "Malformed XML part: word/document.xml"
                )
        );

        assertThatThrownBy(() -> engine.generate(
                template,
                "1.0.0",
                java.util.Map.of(),
                "DOCX",
                new EncryptionOptionsView(false, null, null, null)
        ))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assemblyException = (DocxAssemblyException) ex;
                    assertThat(assemblyException.errorCode()).isEqualTo(ApiErrorCodes.OOXML_VALIDATION_FAILED);
                    assertThat(assemblyException.category()).isEqualTo(ApiErrorCategories.RENDERING);
                    assertThat(assemblyException.messageKey())
                            .isEqualTo("api.error.rendering.ooxmlValidationFailed");
                });
    }

    private static com.bank.docgen.documentbrand.service.DocumentBrandResolveService mockDocumentBrandResolveService() {
        com.bank.docgen.documentbrand.service.DocumentBrandResolveService service =
                org.mockito.Mockito.mock(com.bank.docgen.documentbrand.service.DocumentBrandResolveService.class);
        org.mockito.Mockito.lenient().when(service.resolve(any(), any(), any())).thenReturn(
                new com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand(
                        null,
                        "PLATFORM_DEFAULT",
                        "platform/document-brands/PLATFORM_DEFAULT/logo",
                        null,
                        null
                )
        );
        return service;
    }
}
