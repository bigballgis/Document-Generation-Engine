package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.UpsertContentModuleReferenceRequest;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
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
class TemplateContentModuleReferenceServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID VERSION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MODULE_VERSION_V1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODULE_VERSION_V2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateContentModuleReferenceRepository referenceRepository;
    @Mock
    private ContentModuleRepository contentModuleRepository;
    @Mock
    private ContentModuleVersionRepository contentModuleVersionRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;

    private TemplateContentModuleReferenceService service;
    private TemplateEntity template;
    private ManagementSessionClaims author;

    @BeforeEach
    void setUp() {
        ContentModuleAccessService accessSupport = new ContentModuleAccessService(
                contentModuleRepository,
                groupAccessService,
                new ObjectMapper()
        );
        service = new TemplateContentModuleReferenceService(
                templateService,
                templateRepository,
                templateVersionRepository,
                referenceRepository,
                contentModuleRepository,
                contentModuleVersionRepository,
                accessSupport,
                templateCurrentVersionResolver,
                groupAccessService,
                org.mockito.Mockito.mock(com.bank.docgen.contentmodule.service.ContentModuleNestingService.class)
        );
        template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "RETAIL",
                "Loan Notice",
                null,
                UUID.randomUUID(),
                "10000003"
        );
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("DOCUMENT_AUTHOR"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    private void stubDevVersion() {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000003");
        lenient().when(templateCurrentVersionResolver.requireInFlightDevVersion(TEMPLATE_ID)).thenReturn(version);
        lenient().when(templateCurrentVersionResolver.requireMutableInFlightDevVersion(TEMPLATE_ID))
                .thenReturn(version);
    }

    @Test
    void upsert_pinsExplicitVersionWhenNewerApprovedExists() {
        stubDevVersion();
        when(templateService.requireWritableTemplate(TEMPLATE_ID, author)).thenReturn(template);
        stubModule(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]");
        stubVersion(MODULE_VERSION_V1, "1.0.0", ContentModuleReviewState.APPROVED, ContentModuleLifecycleState.ACTIVE);
        when(referenceRepository.findByTemplateVersionIdAndReferenceKey(VERSION_ID, "CLAUSE-1"))
                .thenReturn(Optional.empty());
        when(referenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]")));
        when(contentModuleRepository.findByIdAndDeletedAtIsNull(MODULE_ID))
                .thenReturn(Optional.of(module(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]")));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);

        var view = service.upsertReference(
                TEMPLATE_ID,
                new UpsertContentModuleReferenceRequest("clause-1", "MOD-LOAN-DISCLOSURE", "1.0.0"),
                author
        );

        assertThat(view.semanticVersion()).isEqualTo("1.0.0");
        assertThat(view.locked()).isFalse();
    }

    @Test
    void upsert_upgradesReferenceToNewerVersion() {
        stubDevVersion();
        when(templateService.requireWritableTemplate(TEMPLATE_ID, author)).thenReturn(template);
        stubModule(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]");
        ContentModuleVersionEntity newerVersion = version(
                MODULE_VERSION_V2,
                "1.1.0",
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.ACTIVE
        );
        when(contentModuleVersionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "1.1.0"))
                .thenReturn(Optional.of(newerVersion));
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V2)).thenReturn(Optional.of(newerVersion));
        TemplateContentModuleReferenceEntity existingReference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        when(referenceRepository.findByTemplateVersionIdAndReferenceKey(VERSION_ID, "CLAUSE-1"))
                .thenReturn(Optional.of(existingReference));
        when(referenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]")));
        when(contentModuleRepository.findByIdAndDeletedAtIsNull(MODULE_ID))
                .thenReturn(Optional.of(module(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]")));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);

        var view = service.upsertReference(
                TEMPLATE_ID,
                new UpsertContentModuleReferenceRequest("CLAUSE-1", "MOD-LOAN-DISCLOSURE", "1.1.0"),
                author
        );

        assertThat(view.semanticVersion()).isEqualTo("1.1.0");
        assertThat(existingReference.getContentModuleVersionId()).isEqualTo(MODULE_VERSION_V2);
    }

    @Test
    void upsert_rejectsLockedReferenceChange() {
        stubDevVersion();
        when(templateService.requireWritableTemplate(TEMPLATE_ID, author)).thenReturn(template);
        TemplateContentModuleReferenceEntity lockedReference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        lockedReference.lock();
        when(referenceRepository.findByTemplateVersionIdAndReferenceKey(VERSION_ID, "CLAUSE-1"))
                .thenReturn(Optional.of(lockedReference));

        assertThatThrownBy(() -> service.upsertReference(
                TEMPLATE_ID,
                new UpsertContentModuleReferenceRequest("CLAUSE-1", "MOD-LOAN-DISCLOSURE", "1.1.0"),
                author
        )).isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void resolvePinnedContentStructures_returnsPinnedVersionContent() {
        TemplateContentModuleReferenceEntity reference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        reference.lock();
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(reference));
        ContentModuleVersionEntity pinnedVersion = version(
                MODULE_VERSION_V1,
                "1.0.0",
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.STOPPED
        );
        pinnedVersion.setContentStructureJson("{\"nodes\":[{\"type\":\"text\",\"value\":\"v1.0 clause\"}]}");
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V1)).thenReturn(Optional.of(pinnedVersion));

        var pinned = service.resolvePinnedContentStructures(VERSION_ID);

        assertThat(pinned).containsEntry("CLAUSE-1", "{\"nodes\":[{\"type\":\"text\",\"value\":\"v1.0 clause\"}]}");
    }

    @Test
    void resolvePinnedContentStructures_missingVersion_failsClosed_fosW7_1() {
        TemplateContentModuleReferenceEntity reference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(reference));
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolvePinnedContentStructures(VERSION_ID))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.validation.contentModuleStructureMissing");
    }

    @Test
    void upsert_acceptsApprovedActiveModuleVersion() {
        stubDevVersion();
        when(templateService.requireWritableTemplate(TEMPLATE_ID, author)).thenReturn(template);
        stubModule(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]");
        stubVersion(MODULE_VERSION_V1, "1.0.0", ContentModuleReviewState.APPROVED, ContentModuleLifecycleState.ACTIVE);
        when(referenceRepository.findByTemplateVersionIdAndReferenceKey(VERSION_ID, "CLAUSE-1"))
                .thenReturn(Optional.empty());
        when(referenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]")));
        when(contentModuleRepository.findByIdAndDeletedAtIsNull(MODULE_ID))
                .thenReturn(Optional.of(module(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]")));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);

        var view = service.upsertReference(
                TEMPLATE_ID,
                new UpsertContentModuleReferenceRequest("clause-1", "MOD-LOAN-DISCLOSURE", "1.0.0"),
                author
        );

        assertThat(view.referenceKey()).isEqualTo("CLAUSE-1");
        assertThat(view.semanticVersion()).isEqualTo("1.0.0");
        assertThat(view.locked()).isFalse();
    }

    @Test
    void upsert_rejectsNonReferencableVersion() {
        stubDevVersion();
        when(templateService.requireWritableTemplate(TEMPLATE_ID, author)).thenReturn(template);
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]")));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        ContentModuleVersionEntity draft = version(
                MODULE_VERSION_V1,
                "1.0.0",
                ContentModuleReviewState.DRAFT,
                null
        );
        when(contentModuleVersionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "1.0.0"))
                .thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.upsertReference(
                TEMPLATE_ID,
                new UpsertContentModuleReferenceRequest("CLAUSE-1", "MOD-LOAN-DISCLOSURE", "1.0.0"),
                author
        )).isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void validateForPublishGate_blocksInvalidReferences() {
        TemplateContentModuleReferenceEntity invalidReference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(invalidReference));
        ContentModuleVersionEntity stopped = version(
                MODULE_VERSION_V1,
                "1.0.0",
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.STOPPED
        );
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V1)).thenReturn(Optional.of(stopped));

        var summary = service.validateReferences(VERSION_ID);

        assertThat(summary.blocking()).isTrue();
        assertThat(summary.invalidReferences()).isEqualTo(1);
    }

    @Test
    void validateForPublishGate_blocksEmptyPinnedStructure() {
        TemplateContentModuleReferenceEntity reference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(reference));
        ContentModuleVersionEntity emptyStructure = version(
                MODULE_VERSION_V1,
                "1.0.0",
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.ACTIVE
        );
        emptyStructure.setContentStructureJson("");
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V1))
                .thenReturn(Optional.of(emptyStructure));

        var summary = service.validateReferences(VERSION_ID);

        assertThat(summary.blocking()).isTrue();
        assertThat(summary.invalidReferences()).isEqualTo(1);
    }

    @Test
    void validateForPublishGate_passesWhenAllReferencable() {
        TemplateContentModuleReferenceEntity validReference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(validReference));
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V1))
                .thenReturn(Optional.of(version(
                        MODULE_VERSION_V1,
                        "1.0.0",
                        ContentModuleReviewState.APPROVED,
                        ContentModuleLifecycleState.ACTIVE
                )));

        var summary = service.validateReferences(VERSION_ID);

        assertThat(summary.blocking()).isFalse();
        assertThat(summary.invalidReferences()).isZero();
    }

    @Test
    void listReferences_marksOutOfDateWhenNewerApprovedVersionExists() {
        when(templateService.requireReadableTemplate(TEMPLATE_ID, author)).thenReturn(template);
        lenient().when(templateCurrentVersionResolver.requireExportableVersion(TEMPLATE_ID))
                .thenReturn(new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000003"));
        TemplateContentModuleReferenceEntity reference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(reference));
        ContentModuleVersionEntity pinned = version(
                MODULE_VERSION_V1,
                "1.0.0",
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.ACTIVE
        );
        ContentModuleVersionEntity newer = version(
                MODULE_VERSION_V2,
                "1.1.0",
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.ACTIVE
        );
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V1)).thenReturn(Optional.of(pinned));
        when(contentModuleRepository.findByIdAndDeletedAtIsNull(MODULE_ID))
                .thenReturn(Optional.of(module(MODULE_ID, "MOD-LOAN-DISCLOSURE", "RETAIL", "[]")));
        when(contentModuleVersionRepository
                .findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                        MODULE_ID,
                        ContentModuleReviewState.APPROVED,
                        ContentModuleLifecycleState.ACTIVE
                ))
                .thenReturn(List.of(newer));

        var views = service.listReferences(TEMPLATE_ID, author);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().outOfDate()).isTrue();
        assertThat(views.getFirst().latestApprovedSemanticVersion()).isEqualTo("1.1.0");
    }

    @Test
    void listOutdatedClauseReferenceAuthorTasks_returnsDraftTemplatesWithOutdatedPins() {
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                List.of("RETAIL"),
                TemplateLifecycleStatus.DRAFT
        )).thenReturn(List.of(template));
        when(templateCurrentVersionResolver.findInFlightDevVersion(TEMPLATE_ID))
                .thenReturn(Optional.of(new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000003")));
        TemplateContentModuleReferenceEntity reference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(reference));
        ContentModuleVersionEntity pinned = version(
                MODULE_VERSION_V1,
                "1.0.0",
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.ACTIVE
        );
        ContentModuleVersionEntity newer = version(
                MODULE_VERSION_V2,
                "1.1.0",
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.ACTIVE
        );
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V1)).thenReturn(Optional.of(pinned));
        when(contentModuleVersionRepository
                .findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                        MODULE_ID,
                        ContentModuleReviewState.APPROVED,
                        ContentModuleLifecycleState.ACTIVE
                ))
                .thenReturn(List.of(newer));

        var tasks = service.listOutdatedClauseReferenceAuthorTasks(author);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().templateId()).isEqualTo(TEMPLATE_ID.toString());
        assertThat(tasks.getFirst().inFlightDevVersionId()).isEqualTo(VERSION_ID.toString());
        assertThat(tasks.getFirst().outdatedReferenceCount()).isEqualTo(1);
    }

    @Test
    void lockReferencesForPublish_setsLockedFlag() {
        TemplateContentModuleReferenceEntity reference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(reference));
        when(referenceRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.lockReferencesForPublish(VERSION_ID);

        ArgumentCaptor<List<TemplateContentModuleReferenceEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(referenceRepository).saveAll(captor.capture());
        assertThat(captor.getValue().getFirst().isLockedFlag()).isTrue();
    }

    @Test
    void lockedReferenceRemainsValidAfterModuleStopped() {
        TemplateContentModuleReferenceEntity lockedReference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_V1
        );
        lockedReference.lock();
        when(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID))
                .thenReturn(List.of(lockedReference));
        when(contentModuleVersionRepository.findById(MODULE_VERSION_V1))
                .thenReturn(Optional.of(version(
                        MODULE_VERSION_V1,
                        "1.0.0",
                        ContentModuleReviewState.APPROVED,
                        ContentModuleLifecycleState.STOPPED
                )));

        var summary = service.validateReferences(VERSION_ID);

        assertThat(summary.blocking()).isFalse();
        assertThat(service.resolveLockedModuleVersionIds(VERSION_ID)).containsExactly(MODULE_VERSION_V1);
    }

    private void stubModule(UUID moduleId, String moduleCode, String groupCode, String sharedJson) {
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(moduleCode))
                .thenReturn(Optional.of(module(moduleId, moduleCode, groupCode, sharedJson)));
    }

    private void stubVersion(
            UUID versionId,
            String semanticVersion,
            ContentModuleReviewState reviewState,
            ContentModuleLifecycleState lifecycleState
    ) {
        ContentModuleVersionEntity version = version(versionId, semanticVersion, reviewState, lifecycleState);
        when(contentModuleVersionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, semanticVersion))
                .thenReturn(Optional.of(version));
        when(contentModuleVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
    }

    private ContentModuleEntity module(UUID moduleId, String moduleCode, String groupCode, String sharedJson) {
        return new ContentModuleEntity(
                moduleId,
                moduleCode,
                groupCode,
                "Module",
                "desc",
                sharedJson,
                "10000003"
        );
    }

    private ContentModuleVersionEntity version(
            UUID versionId,
            String semanticVersion,
            ContentModuleReviewState reviewState,
            ContentModuleLifecycleState lifecycleState
    ) {
        ContentModuleVersionEntity version = new ContentModuleVersionEntity(
                versionId,
                MODULE_ID,
                semanticVersion,
                "{}",
                "change",
                "10000003"
        );
        version.setReviewState(reviewState);
        version.setLifecycleState(lifecycleState);
        return version;
    }
}
