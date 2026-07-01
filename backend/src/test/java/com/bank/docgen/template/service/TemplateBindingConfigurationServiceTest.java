package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.MasterStyleCatalog;
import com.bank.docgen.authoring.structured.MasterStyleCatalogService;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.authoring.structured.NumberingService;
import com.bank.docgen.authoring.structured.NumberingValidationResult;
import com.bank.docgen.authoring.structured.ReferenceNodeService;
import com.bank.docgen.authoring.structured.ReferenceNodeValidationResult;
import com.bank.docgen.authoring.structured.StructuredContentSchemaValidator;
import com.bank.docgen.authoring.structured.StructuredContentValidationResult;
import com.bank.docgen.authoring.structured.TableComponentService;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateBindingConfigurationServiceTest {

    private static final String MINIMAL_STRUCTURED_CONTENT = "{\"schemaVersion\":\"1.0\",\"nodes\":[]}";

    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private NodeMatrixValidationService nodeMatrixValidationService;
    @Mock
    private MasterStyleCatalogService masterStyleCatalogService;
    @Mock
    private TableComponentService tableComponentService;
    @Mock
    private ReferenceNodeService referenceNodeService;
    @Mock
    private NumberingService numberingService;
    @Mock
    private TemplateViewMapper templateViewMapper;

    private TemplateBindingConfigurationService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new TemplateBindingConfigurationService(
                variableSchemaRepository,
                anchorBindingRepository,
                templateVersionRepository,
                masterDocumentRepository,
                objectMapper,
                new StructuredContentSchemaValidator(objectMapper),
                nodeMatrixValidationService,
                masterStyleCatalogService,
                tableComponentService,
                referenceNodeService,
                numberingService,
                templateViewMapper
        );
    }

    @Test
    void validateVariableRequest_nullType_throwsUnsupported() {
        UpsertVariableSchemaRequest request = new UpsertVariableSchemaRequest(
                "amount",
                null,
                true,
                null,
                null,
                "Amount field"
        );

        assertThatThrownBy(() -> service.validateVariableRequest(request))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.variableTypeUnsupported");
    }

    @Test
    void validateVariableRequest_enumWithoutValues_throwsRequired() {
        UpsertVariableSchemaRequest request = new UpsertVariableSchemaRequest(
                "status",
                VariableType.ENUM,
                true,
                null,
                null,
                "Status field"
        );

        assertThatThrownBy(() -> service.validateVariableRequest(request))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.enumValuesRequired");
    }

    @Test
    void computeBindingStatus_missingAnchor() {
        BindingValidationStatus status = service.computeBindingStatus(
                "UNKNOWN_ANCHOR",
                AnchorContentType.TEXT,
                Set.of("BODY"),
                List.of("UNKNOWN_ANCHOR"),
                null,
                Set.of(),
                UUID.randomUUID()
        );

        assertThat(status).isEqualTo(BindingValidationStatus.MISSING_ANCHOR);
    }

    @Test
    void computeBindingStatus_duplicateBinding() {
        BindingValidationStatus status = service.computeBindingStatus(
                "BODY",
                AnchorContentType.TEXT,
                Set.of("BODY"),
                List.of("BODY", "BODY"),
                null,
                Set.of(),
                UUID.randomUUID()
        );

        assertThat(status).isEqualTo(BindingValidationStatus.DUPLICATE_BINDING);
    }

    @Test
    void computeBindingStatus_incompatibleContentType() {
        BindingValidationStatus status = service.computeBindingStatus(
                "HEADER_TEXT",
                AnchorContentType.IMAGE,
                Set.of("HEADER_TEXT"),
                List.of("HEADER_TEXT"),
                null,
                Set.of(),
                UUID.randomUUID()
        );

        assertThat(status).isEqualTo(BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE);
    }

    @Test
    void computeBindingStatus_validAnchor() {
        UUID masterId = UUID.randomUUID();
        when(nodeMatrixValidationService.validate(eq(MINIMAL_STRUCTURED_CONTENT), any()))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(masterStyleCatalogService.loadForMaster(masterId))
                .thenReturn(new MasterStyleCatalog("1.0", Map.of()));
        when(masterStyleCatalogService.validate(eq(MINIMAL_STRUCTURED_CONTENT), any()))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(tableComponentService.validateStructuredContent(MINIMAL_STRUCTURED_CONTENT))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(referenceNodeService.validateStructuredContent(MINIMAL_STRUCTURED_CONTENT))
                .thenReturn(ReferenceNodeValidationResult.of(
                        StructuredContentValidationResult.of(List.of(), List.of()),
                        List.of()
                ));
        when(numberingService.validateStructuredContent(MINIMAL_STRUCTURED_CONTENT))
                .thenReturn(NumberingValidationResult.of(
                        StructuredContentValidationResult.of(List.of(), List.of()),
                        List.of()
                ));

        BindingValidationStatus status = service.computeBindingStatus(
                "BODY",
                AnchorContentType.TEXT,
                Set.of("BODY"),
                List.of("BODY"),
                MINIMAL_STRUCTURED_CONTENT,
                Set.of("customerName"),
                masterId
        );

        assertThat(status).isEqualTo(BindingValidationStatus.VALID);
    }

    @Test
    void upsertVariable_createsNewVariable() {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        UpsertVariableSchemaRequest request = new UpsertVariableSchemaRequest(
                "customerName",
                VariableType.TEXT,
                true,
                null,
                null,
                "Customer name"
        );
        when(variableSchemaRepository.findByTemplateVersionIdAndVariableKey(versionId, "customerName"))
                .thenReturn(Optional.empty());
        VariableSchemaView expectedView = new VariableSchemaView(
                UUID.randomUUID().toString(),
                "customerName",
                VariableType.TEXT,
                true,
                null,
                null,
                "Customer name"
        );
        when(templateViewMapper.toVariableView(any(VariableSchemaEntity.class))).thenReturn(expectedView);

        VariableSchemaView result = service.upsertVariable(version, request);

        assertThat(result).isEqualTo(expectedView);
        ArgumentCaptor<VariableSchemaEntity> captor = ArgumentCaptor.forClass(VariableSchemaEntity.class);
        verify(variableSchemaRepository).save(captor.capture());
        assertThat(captor.getValue().getVariableKey()).isEqualTo("customerName");
        assertThat(captor.getValue().getTemplateVersionId()).isEqualTo(versionId);
    }

    @Test
    void upsertVariable_updatesExistingVariable() {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        VariableSchemaEntity existing = new VariableSchemaEntity(
                UUID.randomUUID(),
                versionId,
                "customerName",
                VariableType.TEXT,
                false,
                "old",
                null,
                "Old description"
        );
        UpsertVariableSchemaRequest request = new UpsertVariableSchemaRequest(
                "customerName",
                VariableType.TEXT,
                true,
                "default",
                null,
                "Updated description"
        );
        when(variableSchemaRepository.findByTemplateVersionIdAndVariableKey(versionId, "customerName"))
                .thenReturn(Optional.of(existing));
        VariableSchemaView expectedView = new VariableSchemaView(
                existing.getId().toString(),
                "customerName",
                VariableType.TEXT,
                true,
                "default",
                null,
                "Updated description"
        );
        when(templateViewMapper.toVariableView(existing)).thenReturn(expectedView);

        VariableSchemaView result = service.upsertVariable(version, request);

        assertThat(result).isEqualTo(expectedView);
        assertThat(existing.isRequired()).isTrue();
        assertThat(existing.getDefaultValue()).isEqualTo("default");
        assertThat(existing.getDescription()).isEqualTo("Updated description");
        verify(variableSchemaRepository).save(existing);
    }
}
