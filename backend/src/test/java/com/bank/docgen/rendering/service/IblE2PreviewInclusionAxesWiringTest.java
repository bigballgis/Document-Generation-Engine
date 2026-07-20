package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PaginationDeltaFidelitySupport;
import com.bank.docgen.rendering.PdfPageCountReader;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import com.bank.docgen.template.port.VariableComputePort;
import com.bank.docgen.template.port.VariableSchemaValidationPort;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD E2-C9 / ADR-0063 Decision 7 — preview assembly must pass real composition axes
 * into the shared pinned-structure resolver (not hardcode empty).
 */
@ExtendWith(MockitoExtension.class)
class IblE2PreviewInclusionAxesWiringTest {

    @Mock
    private com.bank.docgen.template.persistence.AnchorBindingRepository anchorBindingRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private DocxAssembler docxAssembler;
    @Mock
    private DocumentArtifactPipeline documentArtifactPipeline;
    @Mock
    private TemplateRenderContextPort renderContextPort;
    @Mock
    private RenderProfileService renderProfileService;
    @Mock
    private FidelityValidationService fidelityValidationService;
    @Mock
    private VariableComputePort variableComputePort;
    @Mock
    private VariableSchemaValidationPort variableSchemaValidationPort;

    private PreviewGenerationAssemblySupport assembly;
    private UUID versionId;
    private UUID masterId;
    private TemplateVersionEntity version;
    private RenderableTemplateSnapshot template;

    @BeforeEach
    void setUp() {
        versionId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID templateId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        masterId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        version = new TemplateVersionEntity(versionId, templateId, "10000001");
        template = new RenderableTemplateSnapshot(templateId, masterId, "RETAIL");
        assembly = new PreviewGenerationAssemblySupport(
                anchorBindingRepository,
                masterDocumentRepository,
                objectStoragePort,
                docxAssembler,
                documentArtifactPipeline,
                renderContextPort,
                renderProfileService,
                fidelityValidationService,
                variableComputePort,
                variableSchemaValidationPort,
                new PaginationDeltaFidelitySupport(new DocgenRenderingProperties(), new PdfPageCountReader()),
                mockDocumentBrandResolveService()
        );
    }

    @Test
    void previewAssemble_passesJurisdictionProductChannelAxesToResolver() {
        MasterDocumentEntity master = org.mockito.Mockito.mock(MasterDocumentEntity.class);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(variableComputePort.applyCompute(eq(versionId), any(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)).thenReturn(List.of());
        when(renderContextPort.resolvePinnedContentStructures(eq(versionId), any()))
                .thenThrow(new IllegalStateException("stop-after-axes-capture"));

        CompositionInclusionAxes axes = CompositionInclusionAxes.of("Hong Kong", "TRADE-LC", "API");

        assertThatThrownBy(() -> assembly.assembleAndStore(
                template,
                version,
                UUID.randomUUID(),
                Map.of("customerName", "Alice"),
                null,
                axes
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("stop-after-axes-capture");

        ArgumentCaptor<CompositionInclusionAxes> axesCaptor = ArgumentCaptor.forClass(CompositionInclusionAxes.class);
        verify(renderContextPort).resolvePinnedContentStructures(eq(versionId), axesCaptor.capture());
        assertThat(axesCaptor.getValue().jurisdiction()).isEqualTo("Hong Kong");
        assertThat(axesCaptor.getValue().product()).isEqualTo("TRADE-LC");
        assertThat(axesCaptor.getValue().channel()).isEqualTo("API");
    }

    private static com.bank.docgen.documentbrand.service.DocumentBrandResolveService mockDocumentBrandResolveService() {
        com.bank.docgen.documentbrand.service.DocumentBrandResolveService service =
                org.mockito.Mockito.mock(com.bank.docgen.documentbrand.service.DocumentBrandResolveService.class);
        org.mockito.Mockito.lenient().when(service.resolve(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(
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
