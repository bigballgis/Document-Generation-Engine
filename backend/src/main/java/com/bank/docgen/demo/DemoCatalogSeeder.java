package com.bank.docgen.demo;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRoute;
import com.bank.docgen.master.api.CreateMasterRequest;
import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.SubmitMasterReviewRequest;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterDocumentService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Order(50)
@ConditionalOnProperty(prefix = "docgen.demo-catalog", name = "seed-enabled", havingValue = "true")
@EnableConfigurationProperties(DemoCatalogSeedProperties.class)
public class DemoCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoCatalogSeeder.class);

    static final String DEMO_GROUP_CODE = "RETAIL";
    static final String DEMO_MASTER_NAME = "Demo Retail Letterhead";
    static final String DEMO_TEMPLATE_EXTERNAL_ID = "DEMO-RETAIL-LETTER";
    static final String DEMO_TEMPLATE_NAME = "Demo Retail Letter";
    static final String DEMO_ANCHOR_ID = "HEADER";

    private final DemoCatalogSeedProperties properties;
    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterDocumentService masterDocumentService;
    private final TemplateRepository templateRepository;
    private final TemplateService templateService;

    public DemoCatalogSeeder(
            DemoCatalogSeedProperties properties,
            MasterDocumentRepository masterDocumentRepository,
            MasterDocumentService masterDocumentService,
            TemplateRepository templateRepository,
            TemplateService templateService
    ) {
        this.properties = properties;
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterDocumentService = masterDocumentService;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isSeedEnabled()) {
            return;
        }
        try {
            seedIfNeeded();
        } catch (Exception ex) {
            log.error("Demo catalog seed failed: {}", ex.getMessage(), ex);
        }
    }

    private void seedIfNeeded() {
        if (templateRepository.findByExternalIdAndDeletedAtIsNull(DEMO_TEMPLATE_EXTERNAL_ID).isPresent()) {
            log.info("Demo catalog already present (template {}). Skipping seed.", DEMO_TEMPLATE_EXTERNAL_ID);
            return;
        }

        String masterId = ensureApprovedDemoMaster();
        templateService.create(
                new CreateTemplateRequest(
                        DEMO_TEMPLATE_EXTERNAL_ID,
                        DEMO_GROUP_CODE,
                        DEMO_TEMPLATE_NAME,
                        "Automated demo template for catalog and E2E tests",
                        masterId
                ),
                templateAuthorSession()
        );
        log.info(
                "Seeded demo catalog: master '{}' (APPROVED) and template '{}'.",
                DEMO_MASTER_NAME,
                DEMO_TEMPLATE_EXTERNAL_ID
        );
    }

    private String ensureApprovedDemoMaster() {
        var existing = masterDocumentRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(
                List.of(DEMO_GROUP_CODE)
        ).stream()
                .filter(master -> DEMO_MASTER_NAME.equals(master.getName()))
                .findFirst();

        if (existing.isPresent()) {
            MasterDocumentDetailView detail = masterDocumentService.get(existing.get().getId(), globalAdminSession());
            if ("APPROVED".equals(detail.status())) {
                return detail.id();
            }
            return approveMaster(detail.id());
        }

        byte[] docx = DemoDocxFactory.buildHeaderAnchorDocx(DEMO_ANCHOR_ID);
        MultipartFile file = new ByteArrayMultipartFile(
                "file",
                "demo-retail-letterhead.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docx
        );
        MasterDocumentDetailView created = masterDocumentService.create(
                new CreateMasterRequest(DEMO_GROUP_CODE, DEMO_MASTER_NAME, "Demo master for automated tests"),
                file,
                groupAdminSession()
        );
        return approveMaster(created.id());
    }

    private String approveMaster(String masterId) {
        masterDocumentService.submitReview(
                java.util.UUID.fromString(masterId),
                new SubmitMasterReviewRequest("Demo catalog seed"),
                groupAdminSession()
        );
        MasterDocumentDetailView approved = masterDocumentService.decideReview(
                java.util.UUID.fromString(masterId),
                new DecideMasterReviewRequest("APPROVED", "Auto-approved for demo catalog"),
                globalAdminSession()
        );
        return approved.id();
    }

    private static ManagementSessionClaims groupAdminSession() {
        return session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
    }

    private static ManagementSessionClaims globalAdminSession() {
        return session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    private static ManagementSessionClaims templateAuthorSession() {
        return session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
    }

    private static ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                "Demo Seeder",
                "demo-seeder@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                ManagementRoute.DASHBOARD_HOME.routeKey(),
                List.of(ManagementRoute.DASHBOARD_HOME.routeKey()),
                Instant.now().plus(Duration.ofHours(1))
        );
    }
}
