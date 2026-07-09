package com.bank.docgen.authorization.management;

import java.util.Map;
import java.util.Set;

/**
 * Explicit registry of management REST controllers and their authorization anchors.
 * {@link ManagementAuthorizationContractTest} compares this registry to classpath discovery;
 * update here when adding controllers or primary domain services.
 */
final class ManagementAuthorizationRegistry {

    static final String MANAGEMENT_API_PREFIX = "/api/management/v1";

    static final Set<String> MANAGEMENT_CONTROLLERS = Set.of(
            "com.bank.docgen.apimgmt.web.ApiAccessController",
            "com.bank.docgen.apimgmt.web.ApiManagementController",
            "com.bank.docgen.audit.web.AuditController",
            "com.bank.docgen.authorization.management.web.GroupManagementController",
            "com.bank.docgen.authorization.management.web.ManagementAuthController",
            "com.bank.docgen.authorization.management.web.UserManagementController",
            "com.bank.docgen.collaboration.web.CollaborationTimeoutConfigController",
            "com.bank.docgen.collaboration.web.CollaborationWorkItemController",
            "com.bank.docgen.contentmodule.web.ContentModuleController",
            "com.bank.docgen.master.web.MasterDocumentController",
            "com.bank.docgen.master.web.MasterRevisionLineController",
            "com.bank.docgen.rendering.web.BatchTestController",
            "com.bank.docgen.rendering.web.PreviewController",
            "com.bank.docgen.template.web.RiskPromptConfigController",
            "com.bank.docgen.template.web.TemplateController",
            "com.bank.docgen.template.web.TemplateExportController",
            "com.bank.docgen.template.web.TemplateImportController",
            "com.bank.docgen.template.web.TemplateRiskPromptConfigController",
            "com.bank.docgen.template.web.TemplateVersionLineController",
            "com.bank.docgen.template.web.TestDataSetController"
    );

    /**
     * Primary domain services invoked by each management controller (excluding infrastructure helpers).
     */
    static final Map<String, Set<String>> CONTROLLER_PRIMARY_SERVICES = Map.ofEntries(
            Map.entry(
                    "com.bank.docgen.apimgmt.web.ApiAccessController",
                    Set.of("com.bank.docgen.apimgmt.service.ApiAccessAlertQueryService")
            ),
            Map.entry(
                    "com.bank.docgen.apimgmt.web.ApiManagementController",
                    Set.of(
                            "com.bank.docgen.apimgmt.service.ApiManagementService",
                            "com.bank.docgen.apimgmt.service.ApiPolicyImpactPreviewService",
                            "com.bank.docgen.apimgmt.service.ApiPolicyRollbackService",
                            "com.bank.docgen.apimgmt.service.ManagementInvocationQueryService"
                    )
            ),
            Map.entry(
                    "com.bank.docgen.audit.web.AuditController",
                    Set.of("com.bank.docgen.audit.service.AuditQueryService")
            ),
            Map.entry(
                    "com.bank.docgen.authorization.management.web.GroupManagementController",
                    Set.of("com.bank.docgen.authorization.management.service.BusinessGroupService")
            ),
            Map.entry(
                    "com.bank.docgen.authorization.management.web.ManagementAuthController",
                    Set.of("com.bank.docgen.authorization.management.service.ManagementAuthService")
            ),
            Map.entry(
                    "com.bank.docgen.authorization.management.web.UserManagementController",
                    Set.of("com.bank.docgen.authorization.management.service.UserManagementService")
            ),
            Map.entry(
                    "com.bank.docgen.collaboration.web.CollaborationTimeoutConfigController",
                    Set.of("com.bank.docgen.collaboration.service.CollaborationTimeoutConfigService")
            ),
            Map.entry(
                    "com.bank.docgen.collaboration.web.CollaborationWorkItemController",
                    Set.of("com.bank.docgen.collaboration.service.CollaborationWorkItemService")
            ),
            Map.entry(
                    "com.bank.docgen.contentmodule.web.ContentModuleController",
                    Set.of(
                            "com.bank.docgen.contentmodule.service.ContentModuleService",
                            "com.bank.docgen.contentmodule.service.ContentModuleReviewService",
                            "com.bank.docgen.contentmodule.service.ContentModuleLifecycleService",
                            "com.bank.docgen.contentmodule.service.ContentModuleLifecycleImpactService"
                    )
            ),
            Map.entry(
                    "com.bank.docgen.master.web.MasterDocumentController",
                    Set.of("com.bank.docgen.master.service.MasterDocumentService")
            ),
            Map.entry(
                    "com.bank.docgen.master.web.MasterRevisionLineController",
                    Set.of("com.bank.docgen.master.service.MasterRevisionLineService")
            ),
            Map.entry(
                    "com.bank.docgen.rendering.web.BatchTestController",
                    Set.of(
                            "com.bank.docgen.rendering.service.AsyncBatchTestOrchestrator",
                            "com.bank.docgen.rendering.service.BatchTestHistoryService",
                            "com.bank.docgen.rendering.service.SubmitTestEligibilityService"
                    )
            ),
            Map.entry(
                    "com.bank.docgen.rendering.web.PreviewController",
                    Set.of(
                            "com.bank.docgen.rendering.service.AsyncPreviewOrchestrator",
                            "com.bank.docgen.rendering.service.BatchTestGenerationService",
                            "com.bank.docgen.rendering.service.PreviewArtifactDownloadService",
                            "com.bank.docgen.rendering.service.PreviewGenerationService"
                    )
            ),
            Map.entry(
                    "com.bank.docgen.template.web.RiskPromptConfigController",
                    Set.of("com.bank.docgen.template.service.RiskPromptConfigService")
            ),
            Map.entry(
                    "com.bank.docgen.template.web.TemplateController",
                    Set.of(
                            "com.bank.docgen.template.service.TemplateService",
                            "com.bank.docgen.template.service.TemplateLifecycleService",
                            "com.bank.docgen.template.service.TemplateDeleteService",
                            "com.bank.docgen.template.service.RiskPromptConfigService",
                            "com.bank.docgen.template.service.ChangeDiffService",
                            "com.bank.docgen.template.service.CoverageComputationService",
                            "com.bank.docgen.template.service.PublishGateService",
                            "com.bank.docgen.template.service.TemplateContentModuleReferenceService",
                            "com.bank.docgen.template.service.TemplateRuleValidationService"
                    )
            ),
            Map.entry(
                    "com.bank.docgen.template.web.TemplateExportController",
                    Set.of("com.bank.docgen.template.service.TemplateExportService")
            ),
            Map.entry(
                    "com.bank.docgen.template.web.TemplateImportController",
                    Set.of("com.bank.docgen.template.service.TemplateImportService")
            ),
            Map.entry(
                    "com.bank.docgen.template.web.TemplateRiskPromptConfigController",
                    Set.of("com.bank.docgen.template.service.RiskPromptConfigService")
            ),
            Map.entry(
                    "com.bank.docgen.template.web.TemplateVersionLineController",
                    Set.of("com.bank.docgen.template.service.TemplateVersionLineService")
            ),
            Map.entry(
                    "com.bank.docgen.template.web.TestDataSetController",
                    Set.of("com.bank.docgen.template.service.TestDataSetService")
            )
    );

    /**
     * Services that delegate authorization to another anchor listed here or in {@link #GROUP_ACCESS_ANCHORS}.
     */
    static final Map<String, String> AUTHORIZATION_DELEGATES = Map.ofEntries(
            Map.entry(
                    "com.bank.docgen.apimgmt.service.ManagementInvocationQueryService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.rendering.service.BatchTestHistoryService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.rendering.service.BatchTestGenerationService",
                    "com.bank.docgen.template.service.TemplatePreviewAuthorizationAdapter"
            ),
            Map.entry(
                    "com.bank.docgen.rendering.service.AsyncBatchTestOrchestrator",
                    "com.bank.docgen.template.service.TemplatePreviewAuthorizationAdapter"
            ),
            Map.entry(
                    "com.bank.docgen.rendering.service.AsyncPreviewOrchestrator",
                    "com.bank.docgen.template.service.TemplatePreviewAuthorizationAdapter"
            ),
            Map.entry(
                    "com.bank.docgen.rendering.service.PreviewArtifactDownloadService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.rendering.service.PreviewGenerationService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.rendering.service.SubmitTestEligibilityService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.template.service.ChangeDiffService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.template.service.CoverageComputationService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.template.service.PublishGateService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.template.service.TemplateContentModuleReferenceService",
                    "com.bank.docgen.template.service.TemplateService"
            ),
            Map.entry(
                    "com.bank.docgen.template.service.TemplateExportService",
                    "com.bank.docgen.template.service.TemplateExportAccessService"
            ),
            Map.entry(
                    "com.bank.docgen.template.service.TemplateImportService",
                    "com.bank.docgen.template.service.TemplateExportAccessService"
            ),
            Map.entry(
                    "com.bank.docgen.template.service.TemplateRuleValidationService",
                    "com.bank.docgen.template.service.TemplateService"
            )
    );

    /**
     * Services with documented non-{@code GroupAccessService} authorization (session roles or login flow).
     */
    static final Map<String, String> AUTHORIZATION_EXCEPTIONS = Map.ofEntries(
            Map.entry(
                    "com.bank.docgen.authorization.management.service.BusinessGroupService",
                    "Identity admin; enforces session role checks directly (requireGlobalAdmin, canView)"
            ),
            Map.entry(
                    "com.bank.docgen.authorization.management.service.ManagementAuthService",
                    "Login/session endpoints; credential authentication, not capability checks"
            ),
            Map.entry(
                    "com.bank.docgen.authorization.management.service.UserManagementService",
                    "Identity admin; enforces session role checks directly (requireUserAdministrator, guardScopeSubset)"
            ),
            Map.entry(
                    "com.bank.docgen.template.service.RiskPromptConfigService",
                    "Global config uses session role checks; template scope delegates to TemplateService"
            )
    );

    static final Set<String> GROUP_ACCESS_ANCHORS = Set.of(
            "com.bank.docgen.apimgmt.service.ApiAccessAlertQueryService",
            "com.bank.docgen.apimgmt.service.ApiManagementService",
            "com.bank.docgen.apimgmt.service.ApiPolicyImpactPreviewService",
            "com.bank.docgen.apimgmt.service.ApiPolicyRollbackService",
            "com.bank.docgen.audit.service.AuditQueryService",
            "com.bank.docgen.collaboration.service.CollaborationTimeoutConfigService",
            "com.bank.docgen.collaboration.service.CollaborationWorkItemService",
            "com.bank.docgen.contentmodule.service.ContentModuleLifecycleImpactService",
            "com.bank.docgen.contentmodule.service.ContentModuleLifecycleService",
            "com.bank.docgen.contentmodule.service.ContentModuleReviewService",
            "com.bank.docgen.contentmodule.service.ContentModuleService",
            "com.bank.docgen.master.service.MasterDocumentService",
            "com.bank.docgen.master.service.MasterRevisionLineService",
            "com.bank.docgen.template.service.TemplateDeleteService",
            "com.bank.docgen.template.service.TemplateExportAccessService",
            "com.bank.docgen.template.service.TemplateLifecycleService",
            "com.bank.docgen.template.service.TemplatePreviewAuthorizationAdapter",
            "com.bank.docgen.template.service.TemplateService",
            "com.bank.docgen.template.service.TemplateVersionLineService",
            "com.bank.docgen.template.service.TestDataSetService"
    );

    private ManagementAuthorizationRegistry() {
    }
}
