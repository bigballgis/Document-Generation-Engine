package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleAccessService;
import com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView;
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
 * CE-K08 BDD-CE-K08-LM-008…013 — effectiveTo publish-gate expiry evaluation.
 */
@ExtendWith(MockitoExtension.class)
class ContentModuleEffectiveExpiryEvaluationTest {

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
    private TemplateContentModuleReferenceEntity reference;

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
        reference = new TemplateContentModuleReferenceEntity(
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
    void evaluateEffectiveExpiry_pastEffectiveTo_blocks_lm008() {
        moduleVersion.setEffectiveTo(Instant.parse("2026-07-01T00:00:00Z"));

        ContentModuleEffectiveExpirySummaryView summary =
                service.evaluateEffectiveExpiry(TEMPLATE_VERSION_ID, NOW);

        assertThat(summary.blocking()).isTrue();
        assertThat(summary.expiredReferences()).isEqualTo(1);
        assertThat(summary.expiredDetails().getFirst()).contains("MOD-A@1.0.0");
        assertThat(summary.expiredDetails().getFirst()).contains("effectiveTo=");
    }

    @Test
    void evaluateEffectiveExpiry_nullEffectiveTo_passes_lm009() {
        moduleVersion.setEffectiveTo(null);

        ContentModuleEffectiveExpirySummaryView summary =
                service.evaluateEffectiveExpiry(TEMPLATE_VERSION_ID, NOW);

        assertThat(summary.blocking()).isFalse();
        assertThat(summary.expiredReferences()).isZero();
    }

    @Test
    void evaluateEffectiveExpiry_equalNow_notExpired_lm010() {
        moduleVersion.setEffectiveTo(NOW);

        ContentModuleEffectiveExpirySummaryView summary =
                service.evaluateEffectiveExpiry(TEMPLATE_VERSION_ID, NOW);

        assertThat(summary.blocking()).isFalse();
    }

    @Test
    void evaluateEffectiveExpiry_futureEffectiveFrom_doesNotBlock_lm011() {
        moduleVersion.setEffectiveFrom(Instant.parse("2027-01-01T00:00:00Z"));
        moduleVersion.setEffectiveTo(null);

        ContentModuleEffectiveExpirySummaryView summary =
                service.evaluateEffectiveExpiry(TEMPLATE_VERSION_ID, NOW);

        assertThat(summary.blocking()).isFalse();
    }

    @Test
    void resolvePinnedContentStructures_ignoresEffectiveExpiry_lm012() {
        moduleVersion.setEffectiveTo(Instant.parse("2020-01-01T00:00:00Z"));

        var pinned = service.resolvePinnedContentStructures(TEMPLATE_VERSION_ID);

        assertThat(pinned).containsEntry("CLAUSE-A", "{\"blocks\":[]}");
    }

    @Test
    void evaluateEffectiveExpiry_orthogonalToReferenceValidity_lm013() {
        moduleVersion.setEffectiveTo(Instant.parse("2026-07-01T00:00:00Z"));
        // Structure present + referencable → CONTENT_MODULE_REFERENCES would PASS
        assertThat(moduleVersion.isReferencable()).isTrue();

        ContentModuleEffectiveExpirySummaryView expiry =
                service.evaluateEffectiveExpiry(TEMPLATE_VERSION_ID, NOW);
        var refs = service.validateReferences(TEMPLATE_VERSION_ID);

        assertThat(refs.blocking()).isFalse();
        assertThat(expiry.blocking()).isTrue();
    }

    @Test
    void entity_isEffectiveExpired_strictAfterOnly() {
        moduleVersion.setEffectiveTo(NOW);
        assertThat(moduleVersion.isEffectiveExpired(NOW)).isFalse();
        assertThat(moduleVersion.isEffectiveExpired(NOW.plusSeconds(1))).isTrue();
        moduleVersion.setEffectiveTo(null);
        assertThat(moduleVersion.isEffectiveExpired(NOW)).isFalse();
    }
}
