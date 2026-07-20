package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleAccessService;
import com.bank.docgen.template.api.ContentModuleEffectiveNotStartedSummaryView;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * IBL-E5 BDD-IBL-E5-001…004 / 017 — effectiveFrom not-started publish-gate evaluation.
 */
@ExtendWith(MockitoExtension.class)
class ContentModuleEffectiveNotStartedEvaluationTest {

    private static final UUID TEMPLATE_VERSION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MODULE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID MODULE_VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID REF_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Instant NOW = Instant.parse("2026-07-15T12:00:00Z");

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateContentModuleReferenceRepository referenceRepository;
    @Mock
    private ContentModuleRepository contentModuleRepository;
    @Mock
    private ContentModuleVersionRepository contentModuleVersionRepository;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private com.bank.docgen.authorization.management.service.GroupAccessService groupAccessService;

    private TemplateContentModuleReferenceService service;
    private ContentModuleVersionEntity moduleVersion;

    @BeforeEach
    void setUp() {
        ContentModuleAccessService accessSupport =
                new ContentModuleAccessService(contentModuleRepository, groupAccessService, new ObjectMapper());
        service = new TemplateContentModuleReferenceService(
                templateService,
                templateRepository,
                templateVersionRepository,
                referenceRepository,
                contentModuleRepository,
                contentModuleVersionRepository,
                accessSupport,
                templateCurrentVersionResolver,
                groupAccessService
        );
        moduleVersion = new ContentModuleVersionEntity(
                MODULE_VERSION_ID,
                MODULE_ID,
                "1.0.0",
                "{\"blocks\":[]}",
                "Initial",
                "10000003"
        );
        moduleVersion.setReviewState(ContentModuleReviewState.APPROVED);
        moduleVersion.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
        TemplateContentModuleReferenceEntity reference = new TemplateContentModuleReferenceEntity(
                REF_ID,
                TEMPLATE_VERSION_ID,
                "CLAUSE-A",
                MODULE_VERSION_ID
        );
        lenient().when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(TEMPLATE_VERSION_ID))
                .thenReturn(List.of(reference));
        lenient().when(contentModuleVersionRepository.findById(MODULE_VERSION_ID))
                .thenReturn(Optional.of(moduleVersion));
        lenient().when(contentModuleRepository.findByIdAndDeletedAtIsNull(MODULE_ID)).thenReturn(Optional.of(
                new ContentModuleEntity(
                        MODULE_ID,
                        "MOD-A",
                        "RETAIL",
                        "Module A",
                        null,
                        "[]",
                        "10000003"
                )
        ));
    }

    @Test
    void evaluateEffectiveNotStarted_futureEffectiveFrom_blocks_e5001() {
        moduleVersion.setEffectiveFrom(Instant.parse("2027-01-01T00:00:00Z"));

        ContentModuleEffectiveNotStartedSummaryView summary =
                service.evaluateEffectiveNotStarted(TEMPLATE_VERSION_ID, NOW);

        assertThat(summary.blocking()).isTrue();
        assertThat(summary.notStartedReferences()).isEqualTo(1);
        assertThat(summary.notStartedDetails().getFirst()).contains("CLAUSE-A");
        assertThat(summary.notStartedDetails().getFirst()).contains("MOD-A@1.0.0");
        assertThat(summary.notStartedDetails().getFirst()).contains("effectiveFrom=");
    }

    @Test
    void evaluateEffectiveNotStarted_nullEffectiveFrom_passes_e5002() {
        moduleVersion.setEffectiveFrom(null);

        ContentModuleEffectiveNotStartedSummaryView summary =
                service.evaluateEffectiveNotStarted(TEMPLATE_VERSION_ID, NOW);

        assertThat(summary.blocking()).isFalse();
        assertThat(summary.notStartedReferences()).isZero();
    }

    @Test
    void evaluateEffectiveNotStarted_equalNow_passes_e5003() {
        moduleVersion.setEffectiveFrom(NOW);

        ContentModuleEffectiveNotStartedSummaryView summary =
                service.evaluateEffectiveNotStarted(TEMPLATE_VERSION_ID, NOW);

        assertThat(summary.blocking()).isFalse();
    }

    @Test
    void evaluateEffectiveNotStarted_pastEffectiveFrom_passes_e5004() {
        moduleVersion.setEffectiveFrom(Instant.parse("2026-01-01T00:00:00Z"));

        ContentModuleEffectiveNotStartedSummaryView summary =
                service.evaluateEffectiveNotStarted(TEMPLATE_VERSION_ID, NOW);

        assertThat(summary.blocking()).isFalse();
    }

    @Test
    void evaluateEffectiveNotStarted_orthogonalToExpiry_e5005() {
        moduleVersion.setEffectiveTo(Instant.parse("2026-07-01T00:00:00Z"));
        moduleVersion.setEffectiveFrom(null);

        assertThat(service.evaluateEffectiveExpiry(TEMPLATE_VERSION_ID, NOW).blocking()).isTrue();
        assertThat(service.evaluateEffectiveNotStarted(TEMPLATE_VERSION_ID, NOW).blocking()).isFalse();
    }
}
