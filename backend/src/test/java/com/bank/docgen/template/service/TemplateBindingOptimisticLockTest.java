package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * CE-U21 / BDD-CE-U21-DAC-006…010 — optimistic lock on anchor binding upsert.
 */
@ExtendWith(MockitoExtension.class)
class TemplateBindingOptimisticLockTest {

    private static final String MINIMAL_STRUCTURED_CONTENT = "{\"schemaVersion\":\"1.0\",\"nodes\":[]}";
    private static final String UPDATED_STRUCTURED_CONTENT =
            "{\"schemaVersion\":\"1.0\",\"nodes\":[{\"type\":\"paragraph\",\"children\":[]}]}";

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
                org.mockito.Mockito.mock(VariableComputeService.class)
        );
    }

    @Test
    void upsertBinding_updateWithMatchingExpectedUpdatedAt_persistsAndReturnsNewUpdatedAt() {
        // BDD-CE-U21-DAC-006
        Instant t0 = Instant.parse("2026-07-17T00:00:00Z");
        Fixture fixture = stubExistingBinding(t0);
        when(templateViewMapper.toBindingView(any(AnchorBindingEntity.class))).thenAnswer(invocation -> {
            AnchorBindingEntity entity = invocation.getArgument(0);
            return new AnchorBindingView(
                    entity.getId().toString(),
                    entity.getAnchorId(),
                    entity.getDeclaredContentType().name(),
                    entity.getStructuredContentJson(),
                    entity.getValidationStatus(),
                    null,
                    entity.getUpdatedAt()
            );
        });

        UpsertAnchorBindingRequest request = new UpsertAnchorBindingRequest(
                "BODY",
                AnchorContentType.TEXT,
                UPDATED_STRUCTURED_CONTENT,
                null,
                null,
                t0
        );

        AnchorBindingView view = service.upsertBinding(fixture.masterId(), fixture.version(), request);

        ArgumentCaptor<AnchorBindingEntity> captor = ArgumentCaptor.forClass(AnchorBindingEntity.class);
        verify(anchorBindingRepository).save(captor.capture());
        assertThat(captor.getValue().getStructuredContentJson()).isEqualTo(UPDATED_STRUCTURED_CONTENT);
        assertThat(view.updatedAt()).isNotNull();
        assertThat(view.updatedAt().truncatedTo(ChronoUnit.MILLIS))
                .isAfter(t0.truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void upsertBinding_staleExpectedUpdatedAt_throwsBindingVersionConflict() {
        // BDD-CE-U21-DAC-007 — fails closed before content validation
        Instant t0 = Instant.parse("2026-07-17T00:00:00Z");
        Instant t1 = Instant.parse("2026-07-17T00:01:00Z");
        Fixture fixture = stubExistingBindingForLockOnly(t1);

        UpsertAnchorBindingRequest request = new UpsertAnchorBindingRequest(
                "BODY",
                AnchorContentType.TEXT,
                UPDATED_STRUCTURED_CONTENT,
                null,
                null,
                t0
        );

        assertThatThrownBy(() -> service.upsertBinding(fixture.masterId(), fixture.version(), request))
                .isInstanceOf(BindingVersionConflictException.class)
                .extracting(ex -> ((BindingVersionConflictException) ex).messageKey())
                .isEqualTo("api.error.template.bindingVersionConflict");
        verify(anchorBindingRepository, never()).save(any());
        assertThat(fixture.existing().getStructuredContentJson()).isEqualTo(MINIMAL_STRUCTURED_CONTENT);
    }

    @Test
    void upsertBinding_updateMissingExpectedUpdatedAt_throwsValidation() {
        // BDD-CE-U21-DAC-010 — fails closed before content validation
        Fixture fixture = stubExistingBindingForLockOnly(Instant.parse("2026-07-17T00:00:00Z"));

        UpsertAnchorBindingRequest request = new UpsertAnchorBindingRequest(
                "BODY",
                AnchorContentType.TEXT,
                UPDATED_STRUCTURED_CONTENT
        );

        assertThatThrownBy(() -> service.upsertBinding(fixture.masterId(), fixture.version(), request))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.bindingExpectedUpdatedAtRequired");
        verify(anchorBindingRepository, never()).save(any());
    }

    @Test
    void upsertBinding_createOmittingExpectedUpdatedAt_succeedsWithUpdatedAt() {
        // BDD-CE-U21-DAC-009
        UUID masterId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        stubMasterAndValidators(masterId, versionId);
        when(anchorBindingRepository.findByTemplateVersionIdAndAnchorId(versionId, "BODY"))
                .thenReturn(Optional.empty());
        when(templateViewMapper.toBindingView(any(AnchorBindingEntity.class))).thenAnswer(invocation -> {
            AnchorBindingEntity entity = invocation.getArgument(0);
            return new AnchorBindingView(
                    entity.getId().toString(),
                    entity.getAnchorId(),
                    entity.getDeclaredContentType().name(),
                    entity.getStructuredContentJson(),
                    entity.getValidationStatus(),
                    null,
                    entity.getUpdatedAt()
            );
        });

        UpsertAnchorBindingRequest request = new UpsertAnchorBindingRequest(
                "BODY",
                AnchorContentType.TEXT,
                MINIMAL_STRUCTURED_CONTENT
        );

        AnchorBindingView view = service.upsertBinding(masterId, version, request);

        verify(anchorBindingRepository).save(any(AnchorBindingEntity.class));
        assertThat(view.updatedAt()).isNotNull();
    }

    @Test
    void upsertBinding_matchingWithinMillisTruncation_succeeds() {
        // U21-P1 — compare truncated to millis
        Instant stored = Instant.parse("2026-07-17T00:00:00.123456789Z");
        Instant clientToken = Instant.parse("2026-07-17T00:00:00.123Z");
        Fixture fixture = stubExistingBinding(stored);
        when(templateViewMapper.toBindingView(any(AnchorBindingEntity.class))).thenAnswer(invocation -> {
            AnchorBindingEntity entity = invocation.getArgument(0);
            return new AnchorBindingView(
                    entity.getId().toString(),
                    entity.getAnchorId(),
                    entity.getDeclaredContentType().name(),
                    entity.getStructuredContentJson(),
                    entity.getValidationStatus(),
                    null,
                    entity.getUpdatedAt()
            );
        });

        UpsertAnchorBindingRequest request = new UpsertAnchorBindingRequest(
                "BODY",
                AnchorContentType.TEXT,
                UPDATED_STRUCTURED_CONTENT,
                null,
                null,
                clientToken
        );

        service.upsertBinding(fixture.masterId(), fixture.version(), request);

        verify(anchorBindingRepository).save(any(AnchorBindingEntity.class));
    }

    private Fixture stubExistingBinding(Instant updatedAt) {
        UUID masterId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        stubMasterAndValidators(masterId, versionId);
        AnchorBindingEntity existing = existingBinding(versionId, updatedAt);
        when(anchorBindingRepository.findByTemplateVersionIdAndAnchorId(versionId, "BODY"))
                .thenReturn(Optional.of(existing));
        return new Fixture(masterId, version, existing);
    }

    private Fixture stubExistingBindingForLockOnly(Instant updatedAt) {
        UUID masterId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        AnchorBindingEntity existing = existingBinding(versionId, updatedAt);
        when(anchorBindingRepository.findByTemplateVersionIdAndAnchorId(versionId, "BODY"))
                .thenReturn(Optional.of(existing));
        return new Fixture(masterId, version, existing);
    }

    private static AnchorBindingEntity existingBinding(UUID versionId, Instant updatedAt) {
        AnchorBindingEntity existing = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                AnchorContentType.TEXT,
                MINIMAL_STRUCTURED_CONTENT,
                BindingValidationStatus.VALID
        );
        setUpdatedAt(existing, updatedAt);
        return existing;
    }

    private void stubMasterAndValidators(UUID masterId, UUID versionId) {
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId, "RETAIL", "Master", null, "key", "m.docx", "10000003"
        );
        master.replaceAnchors(List.of(new MasterAnchorEntity(masterId, "BODY", "BODY", 1)));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of());
        when(nodeMatrixValidationService.validate(any(), any()))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(masterStyleCatalogService.loadForMaster(masterId))
                .thenReturn(new MasterStyleCatalog("1.0", Map.of()));
        when(masterStyleCatalogService.validate(any(), any()))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(tableComponentService.validateStructuredContent(any(), any()))
                .thenReturn(StructuredContentValidationResult.of(List.of(), List.of()));
        when(referenceNodeService.validateStructuredContent(any()))
                .thenReturn(ReferenceNodeValidationResult.of(
                        StructuredContentValidationResult.of(List.of(), List.of()),
                        List.of()
                ));
        when(numberingService.validateStructuredContent(any()))
                .thenReturn(NumberingValidationResult.of(
                        StructuredContentValidationResult.of(List.of(), List.of()),
                        List.of()
                ));
    }

    private static void setUpdatedAt(AnchorBindingEntity entity, Instant updatedAt) {
        try {
            var field = AnchorBindingEntity.class.getDeclaredField("updatedAt");
            field.setAccessible(true);
            field.set(entity, updatedAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Fixture(UUID masterId, TemplateVersionEntity version, AnchorBindingEntity existing) {
    }
}
