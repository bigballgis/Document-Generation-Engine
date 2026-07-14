package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FidelityWarningViewedServiceTest {

    @Mock
    private TemplatePreviewAuthorizationPort previewAuthorizationPort;
    @Mock
    private PreviewRecordRepository previewRecordRepository;
    @Mock
    private PreviewGenerationService previewGenerationService;

    private FidelityWarningJsonSupport fidelityWarningJsonSupport;
    private FidelityWarningViewedService service;

    private UUID templateId;
    private UUID previewId;
    private PreviewRecordEntity preview;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        fidelityWarningJsonSupport = new FidelityWarningJsonSupport(new ObjectMapper());
        service = new FidelityWarningViewedService(
                previewAuthorizationPort,
                previewRecordRepository,
                fidelityWarningJsonSupport,
                previewGenerationService
        );
        templateId = UUID.randomUUID();
        previewId = UUID.randomUUID();
        preview = new PreviewRecordEntity(
                previewId,
                templateId,
                UUID.randomUUID(),
                "DOCX",
                "hash",
                "10000005",
                null,
                null
        );
        preview.markSucceeded(
                "artifact.docx",
                null,
                fidelityWarningJsonSupport.writeWarnings(List.of(
                        new FidelityWarningView(
                                "UNRESOLVED_VARIABLE",
                                "generation.warning.fidelity.unresolvedVariable",
                                "BODY:node[0]",
                                "BODY",
                                Boolean.FALSE
                        )
                ))
        );
        session = new ManagementSessionClaims(
                "10000005",
                "Tester",
                "tester@example.com",
                com.bank.docgen.authorization.management.domain.AuthSource.LOCAL,
                List.of("TEMPLATE_TESTER"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void markWarningViewed_persistsViewedFlag() {
        when(previewRecordRepository.findById(previewId)).thenReturn(Optional.of(preview));
        when(previewGenerationService.getPreview(templateId, previewId, session))
                .thenReturn(new PreviewRecordView(
                        previewId.toString(),
                        templateId.toString(),
                        preview.getTemplateVersionId().toString(),
                        PreviewStatus.SUCCEEDED,
                        "DOCX",
                        null,
                        "artifact.docx",
                        null,
                        List.of(new FidelityWarningView(
                                "UNRESOLVED_VARIABLE",
                                "generation.warning.fidelity.unresolvedVariable",
                                "BODY:node[0]",
                                "BODY",
                                Boolean.TRUE
                        )),
                        null,
                        null,
                        preview.getCreatedAt()
                ));

        service.markWarningViewed(templateId, previewId, 0, session);

        ArgumentCaptor<PreviewRecordEntity> saved = ArgumentCaptor.forClass(PreviewRecordEntity.class);
        verify(previewRecordRepository).save(saved.capture());
        List<FidelityWarningView> warnings = fidelityWarningJsonSupport.readWarnings(
                saved.getValue().getFidelityWarningsJson()
        );
        assertThat(warnings).hasSize(1);
        assertThat(warnings.getFirst().viewed()).isTrue();
    }

    @Test
    void markWarningViewed_rejectsOutOfRangeIndex() {
        when(previewRecordRepository.findById(previewId)).thenReturn(Optional.of(preview));

        assertThatThrownBy(() -> service.markWarningViewed(templateId, previewId, 3, session))
                .isInstanceOf(PreviewValidationException.class)
                .extracting(ex -> ((PreviewValidationException) ex).messageKey())
                .isEqualTo("api.error.rendering.fidelityWarningIndexOutOfRange");
    }
}
