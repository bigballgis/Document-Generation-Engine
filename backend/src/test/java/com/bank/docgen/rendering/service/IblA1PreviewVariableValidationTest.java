package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import com.bank.docgen.template.port.VariableComputePort;
import com.bank.docgen.sharedkernel.document.variable.VariableValidationException;
import com.bank.docgen.template.service.VariableSchemaValidationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-IBL-A1-005 — preview assembly aligned with runtime VariableSchema validation.
 */
@ExtendWith(MockitoExtension.class)
class IblA1PreviewVariableValidationTest {

    @Mock
    private AnchorBindingRepository anchorBindingRepository;
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
    private VariableSchemaRepository variableSchemaRepository;

    private PreviewGenerationAssemblySupport assembly;
    private UUID versionId;
    private TemplateVersionEntity version;
    private RenderableTemplateSnapshot template;

    @BeforeEach
    void setUp() {
        versionId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID templateId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID masterId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
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
                new VariableSchemaValidationService(variableSchemaRepository),
                new com.bank.docgen.rendering.PaginationDeltaFidelitySupport(
                        new DocgenRenderingProperties(),
                        new com.bank.docgen.rendering.PdfPageCountReader()
                )
        );
    }

    @Test
    void bddIblA1_005_missingRequired_failsBeforeComputeAndStorage() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of(new VariableSchemaEntity(
                        UUID.randomUUID(),
                        versionId,
                        "customerName",
                        VariableType.TEXT,
                        true,
                        null,
                        null,
                        null,
                        null
                )));

        assertThatThrownBy(() -> assembly.assembleAndStore(
                template,
                version,
                UUID.randomUUID(),
                Map.of()
        ))
                .isInstanceOf(VariableValidationException.class)
                .satisfies(ex -> {
                    List<FieldError> errors = ((VariableValidationException) ex).fieldErrors();
                    assertThat(errors).anySatisfy(error -> {
                        assertThat(error.field()).isEqualTo("customerName");
                        assertThat(error.reason()).isEqualTo("REQUIRED");
                    });
                });

        verify(variableComputePort, never()).applyCompute(any(), any(), any());
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }
}
