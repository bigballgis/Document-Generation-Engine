package com.bank.docgen.demo;

import com.bank.docgen.apimgmt.service.ApiManagementService;
import com.bank.docgen.master.api.CreateMasterRequest;
import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.SubmitMasterReviewRequest;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterDocumentService;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.rendering.service.BatchTestGenerationService;
import com.bank.docgen.rendering.service.FidelityWarningJsonSupport;
import com.bank.docgen.rendering.service.FidelityWarningViewedService;
import com.bank.docgen.rendering.service.PreviewGenerationService;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateLifecycleService;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TestDataSetService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Order(51)
@ConditionalOnProperty(prefix = "docgen.demo-catalog", name = "seed-enabled", havingValue = "true")
public class DemoFullFlowCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoFullFlowCatalogSeeder.class);

    public static final String DEMO_FULL_FLOW_EXTERNAL_ID = "DEMO-FULL-FLOW-LETTER";
    static final String DEMO_FULL_FLOW_NAME = "Demo Full-Flow Retail Letter";
    static final String DEMO_FULL_FLOW_RELEASE_VERSION = "1.0.0";
    static final String DEMO_FULL_FLOW_MASTER_LAYOUT_VERSION = DemoRetailLetterheadDocxBuilder.MASTER_LAYOUT_VERSION;
    private static final String STRUCTURED_BINDING_JSON =
            "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[{\"type\":\"variable\",\"key\":\"customerName\"}]}]}";

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterDocumentService masterDocumentService;
    private final TemplateRepository templateRepository;
    private final TemplateService templateService;
    private final DemoFullFlowPublishSupport publishSupport;

    DemoFullFlowCatalogSeeder(
            MasterDocumentRepository masterDocumentRepository,
            MasterDocumentService masterDocumentService,
            TemplateRepository templateRepository,
            TemplateService templateService,
            TestDataSetService testDataSetService,
            PreviewGenerationService previewGenerationService,
            BatchTestGenerationService batchTestGenerationService,
            PreviewRecordRepository previewRecordRepository,
            FidelityWarningJsonSupport fidelityWarningJsonSupport,
            FidelityWarningViewedService fidelityWarningViewedService,
            TemplateLifecycleService templateLifecycleService,
            ApiManagementService apiManagementService
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterDocumentService = masterDocumentService;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.publishSupport = new DemoFullFlowPublishSupport(
                templateService,
                testDataSetService,
                previewGenerationService,
                batchTestGenerationService,
                previewRecordRepository,
                fidelityWarningJsonSupport,
                fidelityWarningViewedService,
                templateLifecycleService,
                apiManagementService,
                STRUCTURED_BINDING_JSON,
                DEMO_FULL_FLOW_RELEASE_VERSION
        );
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedFullFlowIfNeeded();
        } catch (Exception ex) {
            log.error("Demo full-flow catalog seed failed: {}", ex.getMessage(), ex);
        }
    }

    private void seedFullFlowIfNeeded() {
        var existing = templateRepository.findByExternalIdAndDeletedAtIsNull(DEMO_FULL_FLOW_EXTERNAL_ID);
        if (existing.isPresent() && existing.get().getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED) {
            log.info("Demo full-flow template {} already published. Skipping seed.", DEMO_FULL_FLOW_EXTERNAL_ID);
            return;
        }

        String masterId = ensureApprovedDemoMaster();
        UUID templateId = existing.map(TemplateEntity::getId).orElseGet(() -> createFullFlowTemplate(masterId));
        publishSupport.publishFullFlowTemplate(templateId);
        log.info(
                "Seeded demo full-flow catalog: published template '{}' release {} with API policy.",
                DEMO_FULL_FLOW_EXTERNAL_ID,
                DEMO_FULL_FLOW_RELEASE_VERSION
        );
    }

    private UUID createFullFlowTemplate(String masterId) {
        TemplateDetailView created = templateService.create(
                new CreateTemplateRequest(
                        DEMO_FULL_FLOW_EXTERNAL_ID,
                        DemoCatalogSeeder.DEMO_GROUP_CODE,
                        DEMO_FULL_FLOW_NAME,
                        "Full lifecycle demo template for E2E and manual walkthrough",
                        masterId,
                        "zh-CN",
                        null
                ),
                DemoCatalogSessions.templateAuthorSession()
        );
        return UUID.fromString(created.id());
    }

    private String ensureApprovedDemoMaster() {
        var existing = masterDocumentRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(
                List.of(DemoCatalogSeeder.DEMO_GROUP_CODE)
        ).stream()
                .filter(master -> DemoCatalogSeeder.DEMO_MASTER_NAME.equals(master.getName()))
                .findFirst();

        if (existing.isPresent()) {
            MasterDocumentDetailView detail = masterDocumentService.get(existing.get().getId(), DemoCatalogSessions.globalAdminSession());
            if ("APPROVED".equals(detail.status())) {
                return detail.id();
            }
            return approveMaster(detail.id());
        }

        byte[] docx = DemoDocxFactory.buildHeaderAnchorDocx(DemoCatalogSeeder.DEMO_ANCHOR_ID);
        MultipartFile file = new ByteArrayMultipartFile(
                "file",
                "demo-retail-letterhead.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docx
        );
        MasterDocumentDetailView created = masterDocumentService.create(
                new CreateMasterRequest(
                        DemoCatalogSeeder.DEMO_GROUP_CODE,
                        DemoCatalogSeeder.DEMO_MASTER_NAME,
                        "Demo master for automated tests"
                ),
                file,
                DemoCatalogSessions.groupAdminSession()
        );
        return approveMaster(created.id());
    }

    private String approveMaster(String masterId) {
        masterDocumentService.submitReview(
                UUID.fromString(masterId),
                new SubmitMasterReviewRequest("Demo catalog seed"),
                DemoCatalogSessions.groupAdminSession()
        );
        MasterDocumentDetailView approved = masterDocumentService.decideReview(
                UUID.fromString(masterId),
                new DecideMasterReviewRequest("APPROVED", "Auto-approved for demo catalog"),
                DemoCatalogSessions.globalAdminSession()
        );
        return approved.id();
    }
}
