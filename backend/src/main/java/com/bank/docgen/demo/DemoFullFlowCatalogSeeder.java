package com.bank.docgen.demo;

import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.service.ApiManagementService;
import com.bank.docgen.master.api.CreateMasterRequest;
import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.SubmitMasterReviewRequest;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterDocumentService;
import com.bank.docgen.rendering.api.BatchTestGenerateRequest;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.service.BatchTestGenerationService;
import com.bank.docgen.rendering.service.PreviewGenerationService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertTestDataSetRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateLifecycleService;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TestDataSetService;
import java.util.List;
import java.util.Map;
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

    static final String DEMO_FULL_FLOW_EXTERNAL_ID = "DEMO-FULL-FLOW-LETTER";
    static final String DEMO_FULL_FLOW_NAME = "Demo Full-Flow Retail Letter";
    static final String DEMO_FULL_FLOW_RELEASE_VERSION = "1.0.0";
    private static final String STRUCTURED_BINDING_JSON =
            "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[{\"type\":\"variable\",\"key\":\"customerName\"}]}]}";

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterDocumentService masterDocumentService;
    private final TemplateRepository templateRepository;
    private final TemplateService templateService;
    private final TestDataSetService testDataSetService;
    private final PreviewGenerationService previewGenerationService;
    private final BatchTestGenerationService batchTestGenerationService;
    private final TemplateLifecycleService templateLifecycleService;
    private final ApiManagementService apiManagementService;

    DemoFullFlowCatalogSeeder(
            MasterDocumentRepository masterDocumentRepository,
            MasterDocumentService masterDocumentService,
            TemplateRepository templateRepository,
            TemplateService templateService,
            TestDataSetService testDataSetService,
            PreviewGenerationService previewGenerationService,
            BatchTestGenerationService batchTestGenerationService,
            TemplateLifecycleService templateLifecycleService,
            ApiManagementService apiManagementService
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterDocumentService = masterDocumentService;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.testDataSetService = testDataSetService;
        this.previewGenerationService = previewGenerationService;
        this.batchTestGenerationService = batchTestGenerationService;
        this.templateLifecycleService = templateLifecycleService;
        this.apiManagementService = apiManagementService;
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
        publishFullFlowTemplate(templateId);
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
                        masterId
                ),
                DemoCatalogSessions.templateAuthorSession()
        );
        return UUID.fromString(created.id());
    }

    private void publishFullFlowTemplate(UUID templateId) {
        ManagementSessionClaims author = DemoCatalogSessions.templateAuthorSession();
        configurePublishableTemplate(templateId, author);

        var testDataSet = testDataSetService.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Demo full-flow required sample",
                        "Automated demo catalog seed",
                        Map.of("customerName", "Alice"),
                        true,
                        null,
                        null
                ),
                author
        );

        previewGenerationService.testGenerate(
                templateId,
                new TestGenerateRequest(Map.of("customerName", "Alice"), testDataSet.testDataSetId()),
                author
        );
        batchTestGenerationService.runBatch(
                templateId,
                new BatchTestGenerateRequest(List.of(testDataSet.testDataSetId())),
                author
        );

        templateLifecycleService.submitForTest(
                templateId,
                new LifecycleCommentRequest("Demo full-flow seed ready for test"),
                author
        );

        templateLifecycleService.recordTestDecision(
                templateId,
                new LifecycleDecisionRequest(
                        LifecycleDecision.PASSED,
                        "Demo full-flow seed test passed",
                        null,
                        null,
                        true,
                        true,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                DemoCatalogSessions.templateTesterSession()
        );

        templateLifecycleService.recordApprovalDecision(
                templateId,
                new LifecycleDecisionRequest(
                        LifecycleDecision.APPROVED,
                        "Demo full-flow seed approved",
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                DemoCatalogSessions.templateApproverSession()
        );

        apiManagementService.upsertPolicy(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        DEMO_FULL_FLOW_RELEASE_VERSION,
                        List.of("DOCX"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                DemoCatalogSessions.groupAdminSession()
        );

        templateLifecycleService.publish(
                templateId,
                new PublishTemplateRequest(DEMO_FULL_FLOW_RELEASE_VERSION),
                DemoCatalogSessions.groupAdminSession()
        );
    }

    private void configurePublishableTemplate(UUID templateId, ManagementSessionClaims author) {
        templateService.upsertVariable(
                templateId,
                new UpsertVariableSchemaRequest(
                        "customerName",
                        VariableType.TEXT,
                        true,
                        "Customer",
                        null,
                        "Customer name",
                        null
                ),
                author
        );
        templateService.upsertBinding(
                templateId,
                new UpsertAnchorBindingRequest(
                        DemoCatalogSeeder.DEMO_ANCHOR_ID,
                        AnchorContentType.TEXT,
                        STRUCTURED_BINDING_JSON
                ),
                author
        );
        templateService.validateBindings(templateId, author);
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
