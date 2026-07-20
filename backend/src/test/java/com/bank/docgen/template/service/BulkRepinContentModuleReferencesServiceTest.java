package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleAccessService;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BulkRepinContentModuleReferencesRequest;
import com.bank.docgen.template.api.BulkRepinContentModuleReferencesResultView;
import com.bank.docgen.template.api.BulkRepinItemStatus;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
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

/**
 * IBL-E5 BDD-IBL-E5-008…015 — bulk re-pin dry-run / apply / skip / audit.
 */
@ExtendWith(MockitoExtension.class)
class BulkRepinContentModuleReferencesServiceTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID V100 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID V110 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TPL_A = UUID.fromString("aaaaaaaa-0001-0001-0001-000000000001");
    private static final UUID TPL_B = UUID.fromString("aaaaaaaa-0002-0002-0002-000000000002");
    private static final UUID TPL_PUB = UUID.fromString("aaaaaaaa-0003-0003-0003-000000000003");
    private static final UUID VER_A = UUID.fromString("bbbbbbbb-0001-0001-0001-000000000001");
    private static final UUID VER_B = UUID.fromString("bbbbbbbb-0002-0002-0002-000000000002");
    private static final UUID VER_PUB = UUID.fromString("bbbbbbbb-0003-0003-0003-000000000003");

    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ContentModuleRepository contentModuleRepository;
    @Mock
    private ContentModuleVersionRepository contentModuleVersionRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateContentModuleReferenceRepository referenceRepository;
    @Mock
    private TemplateCurrentVersionResolver templateVersionSupport;
    @Mock
    private TemplateContentModuleReferenceService referenceService;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private BulkRepinContentModuleReferencesService service;
    private ContentModuleAccessService accessSupport;
    private ManagementSessionClaims author;
    private ContentModuleEntity module;
    private ContentModuleVersionEntity version100;
    private ContentModuleVersionEntity version110;
    private TemplateEntity draftA;
    private TemplateEntity draftB;

    @BeforeEach
    void setUp() {
        accessSupport = new ContentModuleAccessService(
                contentModuleRepository, groupAccessService, new ObjectMapper());
        service = new BulkRepinContentModuleReferencesService(
                groupAccessService,
                accessSupport,
                contentModuleRepository,
                contentModuleVersionRepository,
                templateRepository,
                referenceRepository,
                templateVersionSupport,
                referenceService,
                auditRecorder
        );
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
        module = new ContentModuleEntity(MODULE_ID, "MOD-A", "RETAIL", "Module A", null, "[]", "10000003");
        version100 = approvedVersion(V100, "1.0.0");
        version110 = approvedVersion(V110, "1.1.0");
        draftA = draftTemplate(TPL_A, "TPL-A");
        draftB = draftTemplate(TPL_B, "TPL-B");

        lenient().when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        lenient().when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        lenient().when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        lenient().when(contentModuleRepository.findByIdAndDeletedAtIsNull(MODULE_ID))
                .thenReturn(Optional.of(module));
        lenient().when(contentModuleVersionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "1.1.0"))
                .thenReturn(Optional.of(version110));
        lenient().when(contentModuleVersionRepository.findById(V100)).thenReturn(Optional.of(version100));
        lenient().when(contentModuleVersionRepository.findById(V110)).thenReturn(Optional.of(version110));
        lenient().when(templateRepository
                        .findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                                List.of("RETAIL"), TemplateLifecycleStatus.DRAFT))
                .thenReturn(List.of(draftA, draftB));
        lenient().when(templateVersionSupport.findInFlightDevVersion(TPL_A))
                .thenReturn(Optional.of(new TemplateVersionEntity(VER_A, TPL_A, "10000003")));
        lenient().when(templateVersionSupport.findInFlightDevVersion(TPL_B))
                .thenReturn(Optional.of(new TemplateVersionEntity(VER_B, TPL_B, "10000003")));
        lenient().when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VER_A))
                .thenReturn(List.of(new TemplateContentModuleReferenceEntity(
                        UUID.randomUUID(), VER_A, "CLAUSE-A", V100)));
        lenient().when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VER_B))
                .thenReturn(List.of(new TemplateContentModuleReferenceEntity(
                        UUID.randomUUID(), VER_B, "CLAUSE-A", V100)));
    }

    @Test
    void dryRun_previewsWithoutMutation_andAudits_e5008() {
        BulkRepinContentModuleReferencesResultView result = service.bulkRepin(request(true, "1.1.0"), author);

        assertThat(result.summary().dryRun()).isTrue();
        assertThat(result.summary().wouldApplyCount()).isEqualTo(2);
        assertThat(result.items()).allMatch(item -> item.status() == BulkRepinItemStatus.WOULD_APPLY);
        verify(referenceService, never()).upsertReference(any(), any(), any());
        verify(auditRecorder).recordContentModuleBulkRepin(
                eq(MODULE_ID),
                eq("RETAIL"),
                eq("10000003"),
                any(),
                eq(true),
                any(),
                eq("1.1.0"),
                eq(false),
                any(),
                any()
        );
    }

    @Test
    void apply_repinsDrafts_e5009() {
        when(referenceService.upsertReference(any(), any(), eq(author)))
                .thenReturn(new ContentModuleReferenceView("CLAUSE-A", "MOD-A", "1.1.0", false, false, null));

        BulkRepinContentModuleReferencesResultView result = service.bulkRepin(request(false, "1.1.0"), author);

        assertThat(result.summary().dryRun()).isFalse();
        assertThat(result.summary().appliedCount()).isEqualTo(2);
        assertThat(result.items()).allMatch(item -> item.status() == BulkRepinItemStatus.APPLIED);
        verify(referenceService).upsertReference(eq(TPL_A), any(), eq(author));
        verify(referenceService).upsertReference(eq(TPL_B), any(), eq(author));
        ArgumentCaptor<Boolean> dryRunCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(auditRecorder).recordContentModuleBulkRepin(
                eq(MODULE_ID),
                eq("RETAIL"),
                eq("10000003"),
                any(),
                dryRunCaptor.capture(),
                any(),
                eq("1.1.0"),
                eq(false),
                any(),
                any()
        );
        assertThat(dryRunCaptor.getValue()).isFalse();
    }

    @Test
    void publishedLocked_skipped_e5010() {
        TemplateEntity published = new TemplateEntity(
                TPL_PUB, "TPL-PUB", "RETAIL", "Published", null, UUID.randomUUID(), "10000003");
        published.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        when(templateRepository
                .findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                        List.of("RETAIL"), TemplateLifecycleStatus.DRAFT))
                .thenReturn(List.of());
        when(templateRepository.findByIdAndDeletedAtIsNull(TPL_PUB)).thenReturn(Optional.of(published));
        when(templateVersionSupport.findInFlightDevVersion(TPL_PUB)).thenReturn(Optional.empty());
        TemplateVersionEntity publishedVersion = new TemplateVersionEntity(VER_PUB, TPL_PUB, "10000003");
        publishedVersion.setReleaseVersion("1.0.0");
        when(templateVersionSupport.findLatestPublishedVersion(TPL_PUB))
                .thenReturn(Optional.of(publishedVersion));
        TemplateContentModuleReferenceEntity locked =
                new TemplateContentModuleReferenceEntity(UUID.randomUUID(), VER_PUB, "CLAUSE-A", V100);
        locked.lock();
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VER_PUB))
                .thenReturn(List.of(locked));

        BulkRepinContentModuleReferencesResultView result = service.bulkRepin(
                new BulkRepinContentModuleReferencesRequest(
                        "RETAIL",
                        MODULE_ID.toString(),
                        null,
                        "1.1.0",
                        null,
                        List.of(TPL_PUB.toString()),
                        false
                ),
                author
        );

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).allMatch(item -> item.status() == BulkRepinItemStatus.SKIPPED_LOCKED);
        verify(referenceService, never()).upsertReference(any(), any(), any());
    }

    @Test
    void alreadyAtTarget_skipped_e5011() {
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VER_A))
                .thenReturn(List.of(new TemplateContentModuleReferenceEntity(
                        UUID.randomUUID(), VER_A, "CLAUSE-A", V110)));
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VER_B))
                .thenReturn(List.of(new TemplateContentModuleReferenceEntity(
                        UUID.randomUUID(), VER_B, "CLAUSE-A", V110)));

        BulkRepinContentModuleReferencesResultView result = service.bulkRepin(request(true, "1.1.0"), author);

        assertThat(result.summary().skippedAlreadyAtTargetCount()).isEqualTo(2);
        assertThat(result.items()).allMatch(item -> item.status() == BulkRepinItemStatus.SKIPPED_ALREADY_AT_TARGET);
    }

    @Test
    void invalidTarget_failsPerItem_e5012() {
        when(contentModuleVersionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "9.9.9"))
                .thenReturn(Optional.empty());

        BulkRepinContentModuleReferencesResultView result = service.bulkRepin(request(true, "9.9.9"), author);

        assertThat(result.summary().failedCount()).isEqualTo(2);
        assertThat(result.items()).allMatch(item ->
                item.status() == BulkRepinItemStatus.FAILED
                        && ApiErrorCodes.BULK_REPIN_TARGET_INVALID.equals(item.errorCode()));
    }

    @Test
    void unauthorized_rejected_e5013() {
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(false);

        assertThatThrownBy(() -> service.bulkRepin(request(true, "1.1.0"), author))
                .isInstanceOf(TemplateAccessDeniedException.class);
        verify(auditRecorder, never()).recordContentModuleBulkRepin(
                any(), any(), any(), any(), any(Boolean.class), any(), any(), any(Boolean.class), any(), any());
    }

    @Test
    void fromSemanticVersion_filters_e5014() {
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VER_B))
                .thenReturn(List.of(new TemplateContentModuleReferenceEntity(
                        UUID.randomUUID(), VER_B, "CLAUSE-A",
                        UUID.fromString("33333333-3333-3333-3333-333333333333"))));
        ContentModuleVersionEntity version101 = approvedVersion(
                UUID.fromString("33333333-3333-3333-3333-333333333333"), "1.0.1");
        when(contentModuleVersionRepository.findById(version101.getId())).thenReturn(Optional.of(version101));

        BulkRepinContentModuleReferencesResultView result = service.bulkRepin(
                new BulkRepinContentModuleReferencesRequest(
                        "RETAIL",
                        MODULE_ID.toString(),
                        "1.0.0",
                        "1.1.0",
                        null,
                        null,
                        true
                ),
                author
        );

        assertThat(result.items()).anyMatch(item ->
                item.templateId().equals(TPL_A.toString())
                        && item.status() == BulkRepinItemStatus.WOULD_APPLY);
        assertThat(result.items()).anyMatch(item ->
                item.templateId().equals(TPL_B.toString())
                        && item.status() == BulkRepinItemStatus.SKIPPED_NO_MATCH);
    }

    @Test
    void dryRunRequired_e5015() {
        assertThatThrownBy(() -> service.bulkRepin(
                new BulkRepinContentModuleReferencesRequest(
                        "RETAIL", MODULE_ID.toString(), null, "1.1.0", null, null, null),
                author
        )).isInstanceOf(TemplateGovernanceException.class)
                .extracting(ex -> ((TemplateGovernanceException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.REQUEST_BODY_INVALID);
    }

    private BulkRepinContentModuleReferencesRequest request(boolean dryRun, String toVersion) {
        return new BulkRepinContentModuleReferencesRequest(
                "RETAIL",
                MODULE_ID.toString(),
                null,
                toVersion,
                null,
                null,
                dryRun
        );
    }

    private TemplateEntity draftTemplate(UUID id, String externalId) {
        TemplateEntity template = new TemplateEntity(
                id, externalId, "RETAIL", externalId, null, UUID.randomUUID(), "10000003");
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        return template;
    }

    private ContentModuleVersionEntity approvedVersion(UUID id, String semantic) {
        ContentModuleVersionEntity version = new ContentModuleVersionEntity(
                id, MODULE_ID, semantic, "{\"blocks\":[]}", "v", "10000003");
        version.setReviewState(ContentModuleReviewState.APPROVED);
        version.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
        return version;
    }
}
