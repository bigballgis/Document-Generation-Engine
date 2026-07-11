package com.bank.docgen.demo;

import com.bank.docgen.master.api.CreateMasterRequest;
import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.SubmitMasterReviewRequest;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterDocumentService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Seeds ≥500 templates across ≥2 groups for LR-C5 catalog pagination / p95 evidence.
 *
 * <p><b>How to enable (Docker / local):</b></p>
 * <pre>
 *   DOCGEN_SEED_CATALOG_LOAD=true
 *   # optional: DOCGEN_SEED_CATALOG_LOAD_COUNT=500
 * </pre>
 * or {@code docgen.catalog-load-seed.enabled=true} in application config.
 *
 * <p>Idempotent: skips when the last expected external id ({@code LOAD-TPL-NNNN}) already exists.</p>
 */
@Component
@Order(55)
@ConditionalOnProperty(prefix = "docgen.catalog-load-seed", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CatalogLoadSeedProperties.class)
public class CatalogLoadSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogLoadSeeder.class);

    static final String EXTERNAL_ID_PREFIX = "LOAD-TPL-";
    static final String MASTER_NAME_PREFIX = "LR-C5 Load Master ";
    static final String ANCHOR_ID = "HEADER";
    static final List<String> GROUPS = List.of("RETAIL", "CORP");

    private final CatalogLoadSeedProperties properties;
    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterDocumentService masterDocumentService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public CatalogLoadSeeder(
            CatalogLoadSeedProperties properties,
            MasterDocumentRepository masterDocumentRepository,
            MasterDocumentService masterDocumentService,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.properties = properties;
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterDocumentService = masterDocumentService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            seedIfNeeded();
        } catch (Exception ex) {
            log.error("Catalog load seed failed: {}", ex.getMessage(), ex);
        }
    }

    private void seedIfNeeded() {
        int target = Math.max(properties.getTemplateCount(), 1);
        String lastExternalId = externalId(target);
        if (templateRepository.findByExternalIdAndDeletedAtIsNull(lastExternalId).isPresent()) {
            log.info("Catalog load seed already present ({}). Skipping.", lastExternalId);
            return;
        }

        UUID retailMasterId = ensureApprovedMaster("RETAIL");
        UUID corpMasterId = ensureApprovedMaster("CORP");
        ManagementSessionClaims author = DemoCatalogSessions.templateAuthorSession();

        int created = 0;
        for (int i = 1; i <= target; i++) {
            String externalId = externalId(i);
            if (templateRepository.findByExternalIdAndDeletedAtIsNull(externalId).isPresent()) {
                continue;
            }
            String groupCode = GROUPS.get((i - 1) % GROUPS.size());
            UUID masterId = "RETAIL".equals(groupCode) ? retailMasterId : corpMasterId;
            UUID templateId = UUID.randomUUID();
            TemplateEntity template = new TemplateEntity(
                    templateId,
                    externalId,
                    groupCode,
                    "Load Template " + i,
                    "LR-C5 catalog load seed row",
                    masterId,
                    author.username()
            );
            templateRepository.save(template);
            templateVersionRepository.save(new TemplateVersionEntity(UUID.randomUUID(), templateId, author.username()));
            created++;
        }
        log.info(
                "Seeded catalog load templates: created={}, target={}, groups={}",
                created,
                target,
                GROUPS
        );
    }

    private UUID ensureApprovedMaster(String groupCode) {
        String masterName = MASTER_NAME_PREFIX + groupCode;
        var existing = masterDocumentRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(
                List.of(groupCode)
        ).stream()
                .filter(master -> masterName.equals(master.getName()))
                .findFirst();
        if (existing.isPresent()) {
            MasterDocumentEntity master = existing.get();
            MasterDocumentDetailView detail = masterDocumentService.get(master.getId(), globalAdmin());
            if ("APPROVED".equals(detail.status())) {
                return master.getId();
            }
            return UUID.fromString(approveMaster(detail.id()));
        }

        byte[] docx = DemoDocxFactory.buildHeaderAnchorDocx(ANCHOR_ID);
        MultipartFile file = new ByteArrayMultipartFile(
                "file",
                "lrc5-load-" + groupCode.toLowerCase(Locale.ROOT) + ".docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docx
        );
        MasterDocumentDetailView created = masterDocumentService.create(
                new CreateMasterRequest(groupCode, masterName, "LR-C5 catalog load seed master"),
                file,
                DemoCatalogSessions.groupAdminSession()
        );
        return UUID.fromString(approveMaster(created.id()));
    }

    private String approveMaster(String masterId) {
        masterDocumentService.submitReview(
                UUID.fromString(masterId),
                new SubmitMasterReviewRequest("LR-C5 load seed"),
                DemoCatalogSessions.groupAdminSession()
        );
        MasterDocumentDetailView approved = masterDocumentService.decideReview(
                UUID.fromString(masterId),
                new DecideMasterReviewRequest("APPROVED", "Auto-approved for LR-C5 load seed"),
                globalAdmin()
        );
        return approved.id();
    }

    private static ManagementSessionClaims globalAdmin() {
        return DemoCatalogSessions.globalAdminSession();
    }

    static String externalId(int index) {
        return EXTERNAL_ID_PREFIX + String.format(Locale.ROOT, "%04d", index);
    }
}
