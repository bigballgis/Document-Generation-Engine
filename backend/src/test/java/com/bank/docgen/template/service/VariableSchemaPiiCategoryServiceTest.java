package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.MasterStyleCatalogService;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.authoring.structured.NumberingService;
import com.bank.docgen.authoring.structured.ReferenceNodeService;
import com.bank.docgen.authoring.structured.StructuredContentSchemaValidator;
import com.bank.docgen.authoring.structured.TableComponentService;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VariableSchemaPiiCategoryServiceTest {

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
    private UUID versionId;
    private TemplateVersionEntity version;

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
                templateViewMapper,
                org.mockito.Mockito.mock(VariableComputeService.class)
        );
        versionId = UUID.randomUUID();
        version = new TemplateVersionEntity(versionId, UUID.randomUUID(), "10000003");
    }

    @Test
    void upsertVariable_withPiiCategory_persistsAndReturnsCategory() {
        UpsertVariableSchemaRequest request = new UpsertVariableSchemaRequest(
                "customerName",
                VariableType.TEXT,
                true,
                null,
                null,
                "Customer name",
                null,
                VariablePiiCategory.PERSONAL_NAME.name()
        );
        when(variableSchemaRepository.findByTemplateVersionIdAndVariableKey(versionId, "customerName"))
                .thenReturn(Optional.empty());
        when(templateViewMapper.toVariableView(any(VariableSchemaEntity.class))).thenAnswer(inv -> {
            VariableSchemaEntity entity = inv.getArgument(0);
            return new VariableSchemaView(
                    entity.getId().toString(),
                    entity.getVariableKey(),
                    entity.getVariableType(),
                    entity.isRequired(),
                    entity.getDefaultValue(),
                    entity.getEnumValues(),
                    entity.getDescription(),
                    entity.getComputeExpression(),
                    entity.getPiiCategory()
            );
        });

        VariableSchemaView view = service.upsertVariable(version, request);

        ArgumentCaptor<VariableSchemaEntity> captor = ArgumentCaptor.forClass(VariableSchemaEntity.class);
        verify(variableSchemaRepository).save(captor.capture());
        assertThat(captor.getValue().getPiiCategory()).isEqualTo(VariablePiiCategory.PERSONAL_NAME);
        assertThat(view.piiCategory()).isEqualTo(VariablePiiCategory.PERSONAL_NAME);
    }

    @Test
    void upsertVariable_omittedPiiCategory_defaultsToNone() {
        UpsertVariableSchemaRequest request = new UpsertVariableSchemaRequest(
                "productCode",
                VariableType.TEXT,
                true,
                null,
                null,
                "Product",
                null
        );
        when(variableSchemaRepository.findByTemplateVersionIdAndVariableKey(versionId, "productCode"))
                .thenReturn(Optional.empty());
        when(templateViewMapper.toVariableView(any(VariableSchemaEntity.class))).thenAnswer(inv -> {
            VariableSchemaEntity entity = inv.getArgument(0);
            return new VariableSchemaView(
                    entity.getId().toString(),
                    entity.getVariableKey(),
                    entity.getVariableType(),
                    entity.isRequired(),
                    entity.getDefaultValue(),
                    entity.getEnumValues(),
                    entity.getDescription(),
                    entity.getComputeExpression(),
                    entity.getPiiCategory()
            );
        });

        VariableSchemaView view = service.upsertVariable(version, request);

        ArgumentCaptor<VariableSchemaEntity> captor = ArgumentCaptor.forClass(VariableSchemaEntity.class);
        verify(variableSchemaRepository).save(captor.capture());
        assertThat(captor.getValue().getPiiCategory()).isEqualTo(VariablePiiCategory.NONE);
        assertThat(view.piiCategory()).isEqualTo(VariablePiiCategory.NONE);
    }

    @Test
    void validateVariableRequest_unknownPiiCategory_throws() {
        UpsertVariableSchemaRequest request = new UpsertVariableSchemaRequest(
                "customerName",
                VariableType.TEXT,
                true,
                null,
                null,
                "Customer",
                null,
                "SSN"
        );

        assertThatThrownBy(() -> service.validateVariableRequest(request))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.piiCategoryInvalid");
    }
}
