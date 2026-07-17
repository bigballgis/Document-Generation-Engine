package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.PasteCleaningCategory;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.authoring.structured.MasterStyleCatalogService;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.authoring.structured.NumberingService;
import com.bank.docgen.authoring.structured.NumberingValidationResult;
import com.bank.docgen.authoring.structured.ReferenceNodeService;
import com.bank.docgen.authoring.structured.ReferenceNodeValidationResult;
import com.bank.docgen.authoring.structured.StructuredContentSchemaValidator;
import com.bank.docgen.authoring.structured.StructuredContentValidationResult;
import com.bank.docgen.authoring.structured.TableComponentService;
import com.bank.docgen.master.persistence.MasterAnchorEntity;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.template.api.PasteCleaningEvidenceItemView;
import com.bank.docgen.template.api.PasteCleaningEvidenceView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
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
                templateViewMapper,
                org.mockito.Mockito.mock(com.bank.docgen.template.service.VariableComputeService.class)
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
                "Amount field",
                null
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
                "Status field",
                null
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
        when(tableComponentService.validateStructuredContent(eq(MINIMAL_STRUCTURED_CONTENT), any()))
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
    void computeBindingStatus_unresolvedPasteBlockers_isIncompatible() {
        // BDD-OPS-PASTE-BINDING-001 / S3 — paste residue alone is enough (fail-closed before content checks)
        UUID masterId = UUID.randomUUID();
        String residue = """
                {
                  "transformedCount":0,
                  "removedCount":0,
                  "warningCount":0,
                  "blockedCount":1,
                  "unresolvedPasteBlockers":true,
                  "items":[
                    {"category":"BLOCKED","messageKey":"paste.summary.blocked","detectionSummary":"Blocked embedded object in pasted HTML."}
                  ]
                }
                """;

        BindingValidationStatus status = service.computeBindingStatus(
                "BODY",
                AnchorContentType.TEXT,
                Set.of("BODY"),
                List.of("BODY"),
                MINIMAL_STRUCTURED_CONTENT,
                Set.of(),
                masterId,
                residue
        );

        assertThat(status).isEqualTo(BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE);
    }

    @Test
    void computeBindingStatus_cleanPasteEvidence_remainsValid() {
        // BDD-OPS-PASTE-BINDING-001 / S2 + S5 (paste dimension)
        UUID masterId = UUID.randomUUID();
        stubValidStructuredValidators(masterId);

        String residue = """
                {
                  "transformedCount":1,
                  "removedCount":0,
                  "warningCount":0,
                  "blockedCount":0,
                  "unresolvedPasteBlockers":false,
                  "items":[
                    {"category":"TRANSFORMED","messageKey":"paste.summary.transformed","detectionSummary":"Transformed paragraph element into controlled structured node."}
                  ]
                }
                """;

        BindingValidationStatus status = service.computeBindingStatus(
                "BODY",
                AnchorContentType.TEXT,
                Set.of("BODY"),
                List.of("BODY"),
                MINIMAL_STRUCTURED_CONTENT,
                Set.of(),
                masterId,
                residue
        );

        assertThat(status).isEqualTo(BindingValidationStatus.VALID);
    }

    @Test
    void upsertBinding_persistsNonSensitivePasteEvidence_andBlocksOnUnresolved() {
        // BDD-OPS-PASTE-BINDING-001 / S2 + S3 (API write defense-in-depth)
        UUID masterId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId, "RETAIL", "Master", null, "key", "m.docx", "10000003"
        );
        master.replaceAnchors(List.of(new MasterAnchorEntity(masterId, "BODY", "BODY", 1)));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(anchorBindingRepository.findByTemplateVersionIdAndAnchorId(versionId, "BODY"))
                .thenReturn(Optional.empty());
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of());
        stubValidStructuredValidators(masterId);
        when(templateViewMapper.toBindingView(any(AnchorBindingEntity.class))).thenAnswer(invocation -> {
            AnchorBindingEntity entity = invocation.getArgument(0);
            return new com.bank.docgen.template.api.AnchorBindingView(
                    entity.getId().toString(),
                    entity.getAnchorId(),
                    entity.getDeclaredContentType().name(),
                    entity.getStructuredContentJson(),
                    entity.getValidationStatus(),
                    PasteCleaningEvidenceSupport.read(entity.getPasteCleaningEvidenceJson(), new ObjectMapper())
            );
        });

        PasteCleaningEvidenceView evidence = new PasteCleaningEvidenceView(
                1, 0, 0, 0, false,
                List.of(new PasteCleaningEvidenceItemView(
                        PasteCleaningCategory.TRANSFORMED,
                        "paste.summary.transformed",
                        "Transformed paragraph element into controlled structured node."
                ))
        );
        UpsertAnchorBindingRequest cleanRequest = new UpsertAnchorBindingRequest(
                "BODY",
                AnchorContentType.TEXT,
                MINIMAL_STRUCTURED_CONTENT,
                evidence,
                null
        );

        service.upsertBinding(masterId, version, cleanRequest);

        ArgumentCaptor<AnchorBindingEntity> captor = ArgumentCaptor.forClass(AnchorBindingEntity.class);
        verify(anchorBindingRepository).save(captor.capture());
        AnchorBindingEntity saved = captor.getValue();
        assertThat(saved.getValidationStatus()).isEqualTo(BindingValidationStatus.VALID);
        assertThat(saved.getPasteCleaningEvidenceJson()).doesNotContain("<");
        assertThat(saved.getPasteCleaningEvidenceJson()).contains("\"blockedCount\":0");
        assertThat(saved.getPasteCleaningEvidenceJson()).doesNotContain("sourceHtml");

        PasteCleaningEvidenceView blocked = new PasteCleaningEvidenceView(
                0, 0, 0, 1, true,
                List.of(new PasteCleaningEvidenceItemView(
                        PasteCleaningCategory.BLOCKED,
                        "paste.summary.blocked",
                        "Blocked absolute positioning in pasted HTML."
                ))
        );
        when(anchorBindingRepository.findByTemplateVersionIdAndAnchorId(versionId, "BODY"))
                .thenReturn(Optional.of(saved));
        UpsertAnchorBindingRequest blockedRequest = new UpsertAnchorBindingRequest(
                "BODY",
                AnchorContentType.TEXT,
                MINIMAL_STRUCTURED_CONTENT,
                blocked,
                null,
                saved.getUpdatedAt()
        );

        service.upsertBinding(masterId, version, blockedRequest);

        assertThat(saved.getValidationStatus()).isEqualTo(BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE);
        assertThat(saved.getPasteCleaningEvidenceJson()).contains("\"blockedCount\":1");
        assertThat(saved.getPasteCleaningEvidenceJson()).doesNotContain("<object");
    }

    @Test
    void upsertBinding_clearPasteCleaningEvidence_clearsResidue() {
        // BDD-OPS-PASTE-BINDING-001 / S5
        UUID masterId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId, "RETAIL", "Master", null, "key", "m.docx", "10000003"
        );
        master.replaceAnchors(List.of(new MasterAnchorEntity(masterId, "BODY", "BODY", 1)));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of());
        stubValidStructuredValidators(masterId);
        when(templateViewMapper.toBindingView(any(AnchorBindingEntity.class))).thenAnswer(invocation -> {
            AnchorBindingEntity entity = invocation.getArgument(0);
            return new com.bank.docgen.template.api.AnchorBindingView(
                    entity.getId().toString(),
                    entity.getAnchorId(),
                    entity.getDeclaredContentType().name(),
                    entity.getStructuredContentJson(),
                    entity.getValidationStatus(),
                    null
            );
        });

        AnchorBindingEntity existing = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                AnchorContentType.TEXT,
                MINIMAL_STRUCTURED_CONTENT,
                BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE
        );
        existing.setPasteCleaningEvidenceJson("""
                {"transformedCount":0,"removedCount":0,"warningCount":0,"blockedCount":1,"unresolvedPasteBlockers":true,"items":[]}
                """);
        when(anchorBindingRepository.findByTemplateVersionIdAndAnchorId(versionId, "BODY"))
                .thenReturn(Optional.of(existing));

        UpsertAnchorBindingRequest clearRequest = new UpsertAnchorBindingRequest(
                "BODY",
                AnchorContentType.TEXT,
                MINIMAL_STRUCTURED_CONTENT,
                null,
                true,
                existing.getUpdatedAt()
        );
        service.upsertBinding(masterId, version, clearRequest);

        assertThat(existing.getPasteCleaningEvidenceJson()).isNull();
        assertThat(existing.getValidationStatus()).isEqualTo(BindingValidationStatus.VALID);
    }

    private void stubValidStructuredValidators(UUID masterId) {
        when(nodeMatrixValidationService.validate(eq(MINIMAL_STRUCTURED_CONTENT), any()))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(masterStyleCatalogService.loadForMaster(masterId))
                .thenReturn(new MasterStyleCatalog("1.0", Map.of()));
        when(masterStyleCatalogService.validate(eq(MINIMAL_STRUCTURED_CONTENT), any()))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(tableComponentService.validateStructuredContent(eq(MINIMAL_STRUCTURED_CONTENT), any()))
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
    }

    @Test
    void computeBindingStatus_malformedConditionExpression_isIncompatible() {
        UUID masterId = UUID.randomUUID();
        String structuredContent = """
                {
                  "nodes": [
                    {
                      "type": "conditionBlock",
                      "conditionExpression": "${x} === true",
                      "children": [{ "type": "textRun", "value": "Hidden" }]
                    }
                  ]
                }
                """;
        ObjectMapper objectMapper = new ObjectMapper();
        TemplateBindingConfigurationService integrationService = new TemplateBindingConfigurationService(
                variableSchemaRepository,
                anchorBindingRepository,
                templateVersionRepository,
                masterDocumentRepository,
                objectMapper,
                new StructuredContentSchemaValidator(objectMapper),
                new NodeMatrixValidationService(objectMapper),
                masterStyleCatalogService,
                new TableComponentService(objectMapper),
                referenceNodeService,
                numberingService,
                templateViewMapper,
                org.mockito.Mockito.mock(com.bank.docgen.template.service.VariableComputeService.class)
        );
        when(masterStyleCatalogService.loadForMaster(masterId))
                .thenReturn(new MasterStyleCatalog("1.0", Map.of()));
        when(masterStyleCatalogService.validate(eq(structuredContent), any()))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(referenceNodeService.validateStructuredContent(structuredContent))
                .thenReturn(ReferenceNodeValidationResult.of(
                        StructuredContentValidationResult.of(List.of(), List.of()),
                        List.of()
                ));
        when(numberingService.validateStructuredContent(structuredContent))
                .thenReturn(NumberingValidationResult.of(
                        StructuredContentValidationResult.of(List.of(), List.of()),
                        List.of()
                ));

        BindingValidationStatus status = integrationService.computeBindingStatus(
                "BODY",
                AnchorContentType.TEXT,
                Set.of("BODY"),
                List.of("BODY"),
                structuredContent,
                Set.of("x"),
                masterId
        );

        assertThat(status).isEqualTo(BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE);
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
                "Customer name",
                null
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
                "Customer name",
                null
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
                "Old description",
                null
        );
        UpsertVariableSchemaRequest request = new UpsertVariableSchemaRequest(
                "customerName",
                VariableType.TEXT,
                true,
                "default",
                null,
                "Updated description",
                null
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
                "Updated description",
                null
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
