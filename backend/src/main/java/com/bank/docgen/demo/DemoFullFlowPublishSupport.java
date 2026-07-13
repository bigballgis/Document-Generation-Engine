package com.bank.docgen.demo;

import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.service.ApiManagementService;
import com.bank.docgen.rendering.api.BatchTestGenerateRequest;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.service.BatchTestGenerationService;
import com.bank.docgen.rendering.service.PreviewGenerationService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertTestDataSetRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.service.TemplateLifecycleService;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TestDataSetService;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private publish-path helpers for demo full-flow catalog seeding.
 */
final class DemoFullFlowPublishSupport {

    private final TemplateService templateService;
    private final TestDataSetService testDataSetService;
    private final PreviewGenerationService previewGenerationService;
    private final BatchTestGenerationService batchTestGenerationService;
    private final TemplateLifecycleService templateLifecycleService;
    private final ApiManagementService apiManagementService;
    private final String structuredBindingJson;
    private final String releaseVersion;

    DemoFullFlowPublishSupport(
            TemplateService templateService,
            TestDataSetService testDataSetService,
            PreviewGenerationService previewGenerationService,
            BatchTestGenerationService batchTestGenerationService,
            TemplateLifecycleService templateLifecycleService,
            ApiManagementService apiManagementService,
            String structuredBindingJson,
            String releaseVersion
    ) {
        this.templateService = templateService;
        this.testDataSetService = testDataSetService;
        this.previewGenerationService = previewGenerationService;
        this.batchTestGenerationService = batchTestGenerationService;
        this.templateLifecycleService = templateLifecycleService;
        this.apiManagementService = apiManagementService;
        this.structuredBindingJson = structuredBindingJson;
        this.releaseVersion = releaseVersion;
    }

    void publishFullFlowTemplate(UUID templateId) {
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
                        true,
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
                        releaseVersion,
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
                new PublishTemplateRequest(releaseVersion, true),
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
                        structuredBindingJson
                ),
                author
        );
        templateService.validateBindings(templateId, author);
    }
}
