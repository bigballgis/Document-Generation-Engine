package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.rendering.ArtifactSpoolService;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.GeneratedArtifactSizeGuard;
import com.bank.docgen.rendering.SpooledArtifact;
import com.bank.docgen.runtime.metrics.GenerationMetrics;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.VariableComputeService;
import com.bank.docgen.sharedkernel.document.variable.VariableValidationException;
import com.bank.docgen.template.service.VariableSchemaValidationService;
import com.bank.docgen.template.service.VersionFidelityWarningService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-IBL-A1-001…004 / 008 — runtime assembly fail-closed VariableSchema validation.
 */
@ExtendWith(MockitoExtension.class)
class IblA1VariableValidationAssemblyTest {

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
    private MasterRevisionLineRepository masterRevisionLineRepository;
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
    private VariableSchemaRepository variableSchemaRepository;

    private DocumentGenerationEngine engine;
    private TemplateEntity template;
    private TemplateVersionEntity version;

    @BeforeEach
    void setUp() {
        lenient().when(variableComputeService.applyCompute(any(), any(), any())).thenAnswer(invocation -> {
            Map<String, Object> input = invocation.getArgument(1);
            return input == null ? Map.of() : new java.util.LinkedHashMap<>(input);
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
                renderProfileService,
                versionFidelityWarningService,
                variableComputeService,
                new VariableSchemaValidationService(variableSchemaRepository),
                new com.bank.docgen.rendering.PaginationDeltaFidelitySupport(
                        new com.bank.docgen.infrastructure.config.DocgenRenderingProperties(),
                        new com.bank.docgen.rendering.PdfPageCountReader()
                ),
                new GenerationMetrics(new SimpleMeterRegistry())
        );
        template = new TemplateEntity(TEMPLATE_ID, "TPL-001", "RETAIL", "Sample", null, MASTER_ID, "10000001");
        version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setReleaseVersion("1.0.0");
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));
    }

    @Test
    void bddIblA1_001_missingRequired_throwsBeforeComputeAndStorage() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(schema("customerName", VariableType.TEXT, true, null)));

        assertThatThrownBy(() -> engine.generate(
                template,
                "1.0.0",
                Map.of("other", "x"),
                "DOCX",
                new EncryptionOptionsView(false, null, null, null)
        ))
                .isInstanceOf(VariableValidationException.class)
                .satisfies(ex -> {
                    VariableValidationException typed = (VariableValidationException) ex;
                    assertThat(typed.fieldErrors()).anySatisfy(error -> {
                        assertThat(error.field()).isEqualTo("customerName");
                        assertThat(error.reason()).isEqualTo("REQUIRED");
                    });
                });

        verify(variableComputeService, never()).applyCompute(any(), any(), any());
        verify(objectStoragePort, never()).put(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    void bddIblA1_002_invalidType_throwsVariableValidationFailed() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(schema("principalAmount", VariableType.AMOUNT, true, null)));

        assertThatThrownBy(() -> engine.generate(
                template,
                "1.0.0",
                Map.of("principalAmount", "not-a-number"),
                "DOCX",
                new EncryptionOptionsView(false, null, null, null)
        ))
                .isInstanceOf(VariableValidationException.class)
                .satisfies(ex -> assertThat(((VariableValidationException) ex).fieldErrors())
                        .extracting(FieldError::reason)
                        .contains("INVALID_TYPE"));
    }

    @Test
    void bddIblA1_003_invalidEnum_throwsEnumNotAllowed() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(schema("letterType", VariableType.ENUM, true, "[\"OFFER\"]")));

        assertThatThrownBy(() -> engine.generate(
                template,
                "1.0.0",
                Map.of("letterType", "NOT_IN_ENUM"),
                "DOCX",
                new EncryptionOptionsView(false, null, null, null)
        ))
                .isInstanceOf(VariableValidationException.class)
                .satisfies(ex -> assertThat(((VariableValidationException) ex).fieldErrors())
                        .anySatisfy(error -> {
                            assertThat(error.field()).isEqualTo("letterType");
                            assertThat(error.reason()).isEqualTo("ENUM_NOT_ALLOWED");
                        }));
    }

    @Test
    void bddIblA1_004_validVariables_assembleWithProvidedValues() throws Exception {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(schema("customerName", VariableType.TEXT, true, null)));
        stubSuccessfulAssembly();

        DocumentGenerationEngine.GeneratedDocument generated = engine.generate(
                template,
                "1.0.0",
                Map.of("customerName", "Acme Bank Ltd"),
                "DOCX",
                new EncryptionOptionsView(false, null, null, null)
        );

        assertThat(generated.documentId()).startsWith("DOC-");
        verify(docxAssembler).assembleStructured(any(), any(), eq(Map.of("customerName", "Acme Bank Ltd")), any());
        verify(objectStoragePort).put(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    void bddIblA1_008_omittedComputeKey_doesNotFailRequired() throws Exception {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(
                        schema("customerName", VariableType.TEXT, true, null),
                        new VariableSchemaEntity(
                                UUID.randomUUID(),
                                VERSION_ID,
                                "amountWords",
                                VariableType.COMPUTED,
                                true,
                                null,
                                null,
                                null,
                                "${principal}"
                        )
                ));
        stubSuccessfulAssembly();

        DocumentGenerationEngine.GeneratedDocument generated = engine.generate(
                template,
                "1.0.0",
                Map.of("customerName", "Acme"),
                "DOCX",
                new EncryptionOptionsView(false, null, null, null)
        );

        assertThat(generated.documentId()).isNotBlank();
        verify(variableComputeService).applyCompute(eq(VERSION_ID), eq(Map.of("customerName", "Acme")), any());
    }

    private void stubSuccessfulAssembly() throws Exception {
        MasterDocumentEntity master = new MasterDocumentEntity(
                MASTER_ID, "RETAIL", "Retail Master", "desc", "masters/master.docx", "master.docx", "10000001"
        );
        byte[] docx = new byte[]{1, 2, 3};
        byte[] finalBytes = new byte[]{9, 8, 7};
        SpooledArtifact spooled = new ArtifactSpoolService(
                new GeneratedArtifactSizeGuard(new com.bank.docgen.infrastructure.config.DocgenRenderingProperties())
        ).spool(finalBytes);
        DocumentArtifactPipeline.GeneratedArtifact artifact = new DocumentArtifactPipeline.GeneratedArtifact(
                spooled,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "output.docx"
        );
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID)).thenReturn(List.of());
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(Map.of());
        when(objectStoragePort.get("masters/master.docx")).thenReturn(new ByteArrayInputStream(docx));
        when(docxAssembler.assembleStructured(any(), any(), any(), any())).thenReturn(docx);
        when(renderProfileService.resolveEffectiveProfile(any(), any()))
                .thenReturn(new com.bank.docgen.sharedkernel.document.RenderProfile(
                        "rp-v1",
                        "MASTER_CATALOG_LOCKED",
                        "CONTROLLED_MULTILEVEL",
                        "REPEAT_HEADER",
                        "PROPORTIONAL_FIT",
                        "SEMANTIC_FIDELITY",
                        "BLOCKERS_PREVENT_PUBLISH",
                        false,
                        com.bank.docgen.sharedkernel.document.PdfArchivalProfile.NONE
                ));
        when(documentArtifactPipeline.finalizeArtifact(any(), eq("DOCX"), any(), any())).thenReturn(artifact);
        when(versionFidelityWarningService.resolveWarningCodes(any(), eq(MASTER_ID))).thenReturn(List.of());
        doAnswer(invocation -> null).when(objectStoragePort)
                .put(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    private VariableSchemaEntity schema(String key, VariableType type, boolean required, String enumValues) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                VERSION_ID,
                key,
                type,
                required,
                null,
                enumValues,
                null,
                null
        );
    }
}
