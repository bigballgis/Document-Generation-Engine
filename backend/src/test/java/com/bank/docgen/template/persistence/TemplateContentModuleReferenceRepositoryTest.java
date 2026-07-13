package com.bank.docgen.template.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.infrastructure.config.QuerydslConfig;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
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
class TemplateContentModuleReferenceRepositoryTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID VERSION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MODULE_VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MASTER_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @Autowired
    private TemplateContentModuleReferenceRepository referenceRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Autowired
    private ContentModuleRepository contentModuleRepository;

    @Autowired
    private ContentModuleVersionRepository contentModuleVersionRepository;

    @Autowired
    private MasterDocumentRepository masterDocumentRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        masterDocumentRepository.save(new MasterDocumentEntity(
                MASTER_ID,
                "RETAIL",
                "Master",
                "desc",
                "masters/test.docx",
                "test.docx",
                "10000001"
        ));
        templateRepository.save(new TemplateEntity(
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "RETAIL",
                "Loan Notice",
                null,
                MASTER_ID,
                "10000001"
        ));
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        templateVersionRepository.save(version);

        contentModuleRepository.save(new ContentModuleEntity(
                MODULE_ID,
                "MOD-LOAN-DISCLOSURE",
                "RETAIL",
                "Loan Disclosure",
                "desc",
                "[]",
                "10000001"
        ));
        ContentModuleVersionEntity moduleVersion = new ContentModuleVersionEntity(
                MODULE_VERSION_ID,
                MODULE_ID,
                "1.0.0",
                "{}",
                "approved",
                "10000001"
        );
        moduleVersion.setReviewState(ContentModuleReviewState.APPROVED);
        moduleVersion.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
        contentModuleVersionRepository.save(moduleVersion);
        entityManager.flush();
    }

    @Test
    void persistsReferenceWithUniqueKeyPerVersion() {
        referenceRepository.save(new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_ID
        ));
        entityManager.flush();
        entityManager.clear();

        assertThat(referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(VERSION_ID)).hasSize(1);
    }

    @Test
    void enforcesUniqueReferenceKeyPerTemplateVersion() {
        referenceRepository.save(new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                VERSION_ID,
                "CLAUSE-1",
                MODULE_VERSION_ID
        ));
        entityManager.flush();

        assertThatThrownBy(() -> {
            referenceRepository.save(new TemplateContentModuleReferenceEntity(
                    UUID.randomUUID(),
                    VERSION_ID,
                    "CLAUSE-1",
                    MODULE_VERSION_ID
            ));
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }
}
