package com.bank.docgen.contentmodule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.contentmodule.domain.ContentModuleCatalogDisplayStatus;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.infrastructure.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
class ContentModuleRepositoryTest {

    private static final UUID MODULE_RETAIL = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MODULE_CORP = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VERSION_V1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VERSION_V2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private ContentModuleRepository moduleRepository;

    @Autowired
    private ContentModuleVersionRepository versionRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seedModules() {
        moduleRepository.save(new ContentModuleEntity(
                MODULE_RETAIL,
                "MOD-RETAIL-DISCLOSURE",
                "RETAIL",
                "Loan Disclosure Module",
                "Standard loan disclosure clauses",
                "[]",
                "10000001"
        ));
        moduleRepository.save(new ContentModuleEntity(
                MODULE_CORP,
                "MOD-CORP-TERMS",
                "CORP",
                "Corp Terms Module",
                "Corporate standard terms",
                "[\"RETAIL\"]",
                "10000002"
        ));
    }

    @Test
    void persistsModuleHeaderWithGroupScopeAndShareScope() {
        Optional<ContentModuleEntity> found = moduleRepository.findByIdAndDeletedAtIsNull(MODULE_RETAIL);

        assertThat(found).isPresent();
        assertThat(found.get().getGroupCode()).isEqualTo("RETAIL");
        assertThat(found.get().getSharedGroupCodesJson()).isEqualTo("[]");
        assertThat(found.get().getDeletedAt()).isNull();
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void findByGroupCodeExcludesLogicallyDeletedModules() {
        ContentModuleEntity retail = moduleRepository.findById(MODULE_RETAIL).orElseThrow();
        retail.setDeletedAt(Instant.parse("2026-06-26T00:00:00Z"));
        moduleRepository.save(retail);
        entityManager.flush();

        assertThat(moduleRepository.findByGroupCodeAndDeletedAtIsNull("RETAIL")).isEmpty();
        assertThat(moduleRepository.findByGroupCodeAndDeletedAtIsNull("CORP")).hasSize(1);
    }

    @Test
    void persistsVersionWithReviewAndLifecycleStateFields() {
        ContentModuleVersionEntity version = versionRepository.save(new ContentModuleVersionEntity(
                VERSION_V1,
                MODULE_RETAIL,
                "1.0.0",
                "{\"blocks\":[]}",
                "Initial version",
                "10000001"
        ));
        entityManager.flush();
        entityManager.clear();

        ContentModuleVersionEntity loaded = versionRepository.findById(VERSION_V1).orElseThrow();
        assertThat(loaded.getModuleId()).isEqualTo(MODULE_RETAIL);
        assertThat(loaded.getSemanticVersion()).isEqualTo("1.0.0");
        assertThat(loaded.getReviewState()).isEqualTo(ContentModuleReviewState.DRAFT);
        assertThat(loaded.getLifecycleState()).isNull();
        assertThat(loaded.getContentStructureJson()).isEqualTo("{\"blocks\":[]}");
        assertThat(loaded.getChangeDescription()).isEqualTo("Initial version");
        assertThat(loaded.getRejectionReason()).isNull();
        assertThat(version.getCreatedAt()).isNotNull();
    }

    @Test
    void persistsLegalMetadataOnVersion_ceK08() {
        ContentModuleVersionEntity version = new ContentModuleVersionEntity(
                VERSION_V1,
                MODULE_RETAIL,
                "1.0.0",
                "{\"blocks\":[]}",
                "Initial version",
                "10000001"
        );
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");
        version.setLegalMetadata("England and Wales", from, to, "LR-2026-001");
        versionRepository.save(version);
        entityManager.flush();
        entityManager.clear();

        ContentModuleVersionEntity loaded = versionRepository.findById(VERSION_V1).orElseThrow();
        assertThat(loaded.getJurisdiction()).isEqualTo("England and Wales");
        assertThat(loaded.getEffectiveFrom()).isEqualTo(from);
        assertThat(loaded.getEffectiveTo()).isEqualTo(to);
        assertThat(loaded.getLegalReviewRef()).isEqualTo("LR-2026-001");
    }

    @Test
    void searchCatalog_filtersByJurisdictionOnCatalogFilterVersion_ceK08() {
        ContentModuleVersionEntity england = approvedActiveVersion(VERSION_V1, "1.0.0");
        england.setJurisdiction("England and Wales");
        versionRepository.save(england);

        ContentModuleVersionEntity corpVersion = new ContentModuleVersionEntity(
                VERSION_V2,
                MODULE_CORP,
                "1.0.0",
                "{}",
                "corp",
                "10000002"
        );
        corpVersion.setReviewState(ContentModuleReviewState.APPROVED);
        corpVersion.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
        corpVersion.setJurisdiction("Hong Kong");
        versionRepository.save(corpVersion);
        entityManager.flush();

        var filter = new ContentModuleRepositoryCustom.ContentModuleCatalogFilter(
                List.of("RETAIL", "CORP"),
                false,
                null,
                null,
                null,
                null,
                "England and Wales",
                null,
                null,
                null
        );
        CatalogQueryPage<ContentModuleEntity> page = moduleRepository.searchCatalog(filter, 0, 20);

        assertThat(page.content()).extracting(ContentModuleEntity::getModuleCode)
                .containsExactly("MOD-RETAIL-DISCLOSURE");
    }

    @Test
    void searchCatalog_filtersByHeadDisplayStatus_draft_ceU20() {
        versionRepository.save(new ContentModuleVersionEntity(
                VERSION_V1, MODULE_RETAIL, "1.0.0", "{}", "draft", "10000001"));
        ContentModuleVersionEntity approved = new ContentModuleVersionEntity(
                VERSION_V2, MODULE_CORP, "1.0.0", "{}", "approved", "10000002");
        approved.setReviewState(ContentModuleReviewState.APPROVED);
        approved.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
        versionRepository.save(approved);
        entityManager.flush();

        var filter = new ContentModuleRepositoryCustom.ContentModuleCatalogFilter(
                List.of("RETAIL", "CORP"),
                false,
                null,
                null,
                null,
                ContentModuleCatalogDisplayStatus.DRAFT,
                null,
                null,
                null,
                null
        );
        CatalogQueryPage<ContentModuleEntity> page = moduleRepository.searchCatalog(filter, 0, 20);

        assertThat(page.content()).extracting(ContentModuleEntity::getModuleCode)
                .containsExactly("MOD-RETAIL-DISCLOSURE");
    }

    @Test
    void searchCatalog_stoppedHeadDoesNotMatchDraft_ceU20() {
        ContentModuleVersionEntity stopped = approvedActiveVersion(VERSION_V1, "1.0.0");
        stopped.setLifecycleState(ContentModuleLifecycleState.STOPPED);
        versionRepository.save(stopped);
        entityManager.flush();

        var draftFilter = new ContentModuleRepositoryCustom.ContentModuleCatalogFilter(
                List.of("RETAIL"),
                false,
                null,
                null,
                null,
                ContentModuleCatalogDisplayStatus.DRAFT,
                null,
                null,
                null,
                null
        );
        var stoppedFilter = new ContentModuleRepositoryCustom.ContentModuleCatalogFilter(
                List.of("RETAIL"),
                false,
                null,
                null,
                null,
                ContentModuleCatalogDisplayStatus.STOPPED,
                null,
                null,
                null,
                null
        );

        assertThat(moduleRepository.searchCatalog(draftFilter, 0, 20).content()).isEmpty();
        assertThat(moduleRepository.searchCatalog(stoppedFilter, 0, 20).content())
                .extracting(ContentModuleEntity::getModuleCode)
                .containsExactly("MOD-RETAIL-DISCLOSURE");
    }

    @Test
    void searchCatalog_statusAndJurisdictionAnd_ceU20() {
        ContentModuleVersionEntity englandDraft = new ContentModuleVersionEntity(
                VERSION_V1, MODULE_RETAIL, "1.0.0", "{}", "draft", "10000001");
        englandDraft.setJurisdiction("England and Wales");
        versionRepository.save(englandDraft);

        ContentModuleVersionEntity hongKongDraft = new ContentModuleVersionEntity(
                VERSION_V2, MODULE_CORP, "1.0.0", "{}", "draft", "10000002");
        hongKongDraft.setJurisdiction("Hong Kong");
        versionRepository.save(hongKongDraft);
        entityManager.flush();

        var filter = new ContentModuleRepositoryCustom.ContentModuleCatalogFilter(
                List.of("RETAIL", "CORP"),
                false,
                null,
                null,
                null,
                ContentModuleCatalogDisplayStatus.DRAFT,
                "England and Wales",
                null,
                null,
                null
        );
        CatalogQueryPage<ContentModuleEntity> page = moduleRepository.searchCatalog(filter, 0, 20);

        assertThat(page.content()).extracting(ContentModuleEntity::getModuleCode)
                .containsExactly("MOD-RETAIL-DISCLOSURE");
    }

    @Test
    void enforcesUniqueSemanticVersionPerModule() {
        versionRepository.save(new ContentModuleVersionEntity(
                VERSION_V1,
                MODULE_RETAIL,
                "1.0.0",
                "{}",
                "First",
                "10000001"
        ));
        entityManager.flush();

        assertThatThrownBy(() -> {
            versionRepository.save(new ContentModuleVersionEntity(
                    VERSION_V2,
                    MODULE_RETAIL,
                    "1.0.0",
                    "{}",
                    "Duplicate",
                    "10000001"
            ));
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void findByModuleIdAndSemanticVersionReturnsMatchingVersion() {
        versionRepository.save(new ContentModuleVersionEntity(
                VERSION_V1,
                MODULE_RETAIL,
                "1.0.0",
                "{}",
                "v1",
                "10000001"
        ));
        versionRepository.save(new ContentModuleVersionEntity(
                VERSION_V2,
                MODULE_RETAIL,
                "1.1.0",
                "{}",
                "v2",
                "10000001"
        ));

        Optional<ContentModuleVersionEntity> found =
                versionRepository.findByModuleIdAndSemanticVersion(MODULE_RETAIL, "1.1.0");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(VERSION_V2);
    }

    @Test
    void referencableWhenApprovedAndActive() {
        ContentModuleVersionEntity referencable = approvedActiveVersion(VERSION_V1, "1.0.0");
        ContentModuleVersionEntity stopped = approvedActiveVersion(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "2.0.0"
        );
        stopped.setLifecycleState(ContentModuleLifecycleState.STOPPED);

        ContentModuleVersionEntity draft = new ContentModuleVersionEntity(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                MODULE_RETAIL,
                "3.0.0",
                "{}",
                "draft",
                "10000001"
        );

        assertThat(referencable.isReferencable()).isTrue();
        assertThat(stopped.isReferencable()).isFalse();
        assertThat(draft.isReferencable()).isFalse();
    }

    @Test
    void findReferencableVersionsByModuleId() {
        versionRepository.save(approvedActiveVersion(VERSION_V1, "1.0.0"));
        ContentModuleVersionEntity stopped = approvedActiveVersion(VERSION_V2, "1.1.0");
        stopped.setLifecycleState(ContentModuleLifecycleState.STOPPED);
        versionRepository.save(stopped);

        List<ContentModuleVersionEntity> referencable =
                versionRepository.findByModuleIdAndReviewStateAndLifecycleState(
                        MODULE_RETAIL,
                        ContentModuleReviewState.APPROVED,
                        ContentModuleLifecycleState.ACTIVE
                );

        assertThat(referencable).hasSize(1);
        assertThat(referencable.getFirst().getSemanticVersion()).isEqualTo("1.0.0");
    }

    private ContentModuleVersionEntity approvedActiveVersion(UUID versionId, String semanticVersion) {
        ContentModuleVersionEntity version = new ContentModuleVersionEntity(
                versionId,
                MODULE_RETAIL,
                semanticVersion,
                "{}",
                "approved",
                "10000001"
        );
        version.setReviewState(ContentModuleReviewState.APPROVED);
        version.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
        return version;
    }
}
