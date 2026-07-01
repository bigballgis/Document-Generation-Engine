package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FidelityValidationServiceTest {

    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MASTER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;

    private FidelityValidationService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new FidelityValidationService(
                anchorBindingRepository,
                variableSchemaRepository,
                new NodeMatrixValidationService(objectMapper),
                new MasterStyleCatalogService(objectMapper),
                new TableComponentService(objectMapper),
                new ReferenceNodeService(objectMapper),
                new NumberingService(objectMapper)
        );
    }

    @Test
    void imageScaling_binding_emitsImageScalingAdjustedWarning() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(variable("customerName")));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID))
                .thenReturn(List.of(binding("""
                        {
                          "nodes": [
                            { "type": "imageRef", "imageRef": "IMG-1", "applyScaling": true }
                          ]
                        }
                        """)));

        List<FidelityWarningView> warnings = service.collectWarningsForVersion(VERSION_ID, MASTER_ID);

        assertThat(warnings).hasSize(1);
        assertThat(warnings.getFirst().code()).isEqualTo(FidelityWarningCode.IMAGE_SCALING_ADJUSTED.name());
        assertThat(warnings.getFirst().messageKey())
                .isEqualTo(ReferenceNodeService.MESSAGE_KEY_IMAGE_SCALING);
    }

    @Test
    void cleanParagraphVariable_binding_emitsNoWarnings() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(variable("customerName")));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID))
                .thenReturn(List.of(binding("""
                        {
                          "nodes": [
                            {
                              "type": "paragraph",
                              "children": [
                                { "type": "variable", "key": "customerName" }
                              ]
                            }
                          ]
                        }
                        """)));

        List<FidelityWarningView> warnings = service.collectWarningsForVersion(VERSION_ID, MASTER_ID);

        assertThat(warnings).isEmpty();
    }

    @Test
    void aggregatesWarningsAcrossMultipleBindings() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of());
        AnchorBindingEntity first = new AnchorBindingEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "HEADER",
                AnchorContentType.IMAGE,
                """
                        {"nodes":[{"type":"imageRef","imageRef":"IMG-1","applyScaling":true}]}
                        """,
                BindingValidationStatus.VALID
        );
        AnchorBindingEntity second = new AnchorBindingEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "FOOTER",
                AnchorContentType.IMAGE,
                """
                        {"nodes":[{"type":"imageRef","imageRef":"IMG-2","applyScaling":true}]}
                        """,
                BindingValidationStatus.VALID
        );
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID))
                .thenReturn(List.of(first, second));

        List<FidelityWarningView> warnings = service.collectWarningsForVersion(VERSION_ID, MASTER_ID);

        assertThat(warnings).hasSize(2);
        assertThat(warnings).allMatch(w -> FidelityWarningCode.IMAGE_SCALING_ADJUSTED.name().equals(w.code()));
    }

    private AnchorBindingEntity binding(String structuredContentJson) {
        return new AnchorBindingEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "HEADER",
                AnchorContentType.TEXT,
                structuredContentJson,
                BindingValidationStatus.VALID
        );
    }

    private VariableSchemaEntity variable(String key) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                VERSION_ID,
                key,
                com.bank.docgen.template.domain.VariableType.TEXT,
                true,
                "default",
                null,
                "desc",
                null
        );
    }
}
