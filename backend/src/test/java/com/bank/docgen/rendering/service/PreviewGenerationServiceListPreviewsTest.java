package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.api.PreviewComparisonView;
import com.bank.docgen.rendering.api.PreviewSummaryView;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import com.bank.docgen.template.port.TestDataSetEvidencePort;
import com.bank.docgen.template.port.VariableComputePort;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * BDD-PRR-A02-001 / 003 / 004 — preview history TopN at DB.
 */
@ExtendWith(MockitoExtension.class)
class PreviewGenerationServiceListPreviewsTest {

    @Mock
    private TemplatePreviewAuthorizationPort previewAuthorizationPort;
    @Mock
    private TestDataSetEvidencePort testDataSetEvidencePort;
    @Mock
    private TemplateRenderContextPort renderContextPort;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private PreviewRecordRepository previewRecordRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private DocxAssembler docxAssembler;
    @Mock
    private DocumentArtifactPipeline documentArtifactPipeline;
    @Mock
    private PreviewComparisonService previewComparisonService;
    @Mock
    private RenderProfileService renderProfileService;
    @Mock
    private FidelityValidationService fidelityValidationService;
    @Mock
    private FidelityWarningJsonSupport fidelityWarningJsonSupport;
    @Mock
    private VariableComputePort variableComputePort;

    private PreviewGenerationService service;
    private UUID templateId;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new PreviewGenerationService(
                previewAuthorizationPort,
                testDataSetEvidencePort,
                renderContextPort,
                anchorBindingRepository,
                masterDocumentRepository,
                previewRecordRepository,
                objectStoragePort,
                docxAssembler,
                documentArtifactPipeline,
                new ObjectMapper(),
                previewComparisonService,
                renderProfileService,
                fidelityValidationService,
                fidelityWarningJsonSupport,
                variableComputePort,
                org.mockito.Mockito.mock(com.bank.docgen.template.port.VariableSchemaValidationPort.class),
                new com.bank.docgen.rendering.PaginationDeltaFidelitySupport(
                        new com.bank.docgen.infrastructure.config.DocgenRenderingProperties(),
                        new com.bank.docgen.rendering.PdfPageCountReader()
                ),
                org.mockito.Mockito.mock(com.bank.docgen.documentbrand.service.DocumentBrandResolveService.class)
        );
        templateId = UUID.randomUUID();
        session = new ManagementSessionClaims(
                "10000001", "Author", "author@test.com",
                AuthSource.LOCAL, List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"), "route.home", List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        lenient().when(previewAuthorizationPort.requireReadableSnapshot(templateId, session))
                .thenReturn(new RenderableTemplateSnapshot(templateId, UUID.randomUUID(), "RETAIL"));
        lenient().when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(any()))
                .thenReturn(List.of());
        lenient().when(previewComparisonService.compare(any(), any()))
                .thenReturn(new PreviewComparisonView(0, 0, 0, List.of()));
        lenient().when(fidelityWarningJsonSupport.readWarnings(any())).thenReturn(List.of());
    }

    @Test
    void listPreviews_usesPageableTop50_notLoadAll() {
        // BDD-PRR-A02-001
        List<PreviewRecordEntity> top50 = IntStream.range(0, 50)
                .mapToObj(i -> preview(templateId))
                .toList();
        when(previewRecordRepository.findByTemplateIdOrderByCreatedAtDesc(eq(templateId), any(Pageable.class)))
                .thenReturn(top50);

        List<PreviewSummaryView> result = service.listPreviews(templateId, session);

        assertThat(result).hasSize(50);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(previewRecordRepository).findByTemplateIdOrderByCreatedAtDesc(
                eq(templateId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void listPreviews_fewerThanLimit_returnsAll() {
        // BDD-PRR-A02-003
        List<PreviewRecordEntity> three = IntStream.range(0, 3)
                .mapToObj(i -> preview(templateId))
                .toList();
        when(previewRecordRepository.findByTemplateIdOrderByCreatedAtDesc(eq(templateId), any(Pageable.class)))
                .thenReturn(three);

        List<PreviewSummaryView> result = service.listPreviews(templateId, session);

        assertThat(result).hasSize(3);
    }

    @Test
    void listPreviews_unreadableTemplate_propagatesAccessDenied() {
        // BDD-PRR-A02-004
        doThrow(new TemplateAccessDeniedException())
                .when(previewAuthorizationPort).requireReadableSnapshot(templateId, session);

        assertThatThrownBy(() -> service.listPreviews(templateId, session))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    private PreviewRecordEntity preview(UUID templateId) {
        return new PreviewRecordEntity(
                UUID.randomUUID(),
                templateId,
                UUID.randomUUID(),
                "DOCX",
                "hash",
                "10000001",
                null,
                null
        );
    }
}
