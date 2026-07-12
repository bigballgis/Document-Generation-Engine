<script setup lang="ts">
import { toRef } from 'vue'
import TemplateExportActions from '@/components/templates/TemplateExportActions.vue'
import TemplateDetailDevWorkspace from '@/views/templates/detail/TemplateDetailDevWorkspace.vue'
import TemplateDetailJourneyStack from '@/views/templates/detail/TemplateDetailJourneyStack.vue'
import TemplateDetailLegacyWorkspace from '@/views/templates/detail/TemplateDetailLegacyWorkspace.vue'
import TemplateDetailDialogs from '@/views/templates/detail/TemplateDetailDialogs.vue'
import TemplateWorkspaceHeader from '@/components/templates/TemplateWorkspaceHeader.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import { useTemplateDetailController } from './useTemplateDetailController'

const props = withDefaults(
  defineProps<{
    workspace?: 'legacy' | 'dev-editor'
  }>(),
  {
    workspace: 'legacy',
  },
)

const {
  t,
  formatDateTime,
  templateId,
  isDevEditor,
  template,
  showDetailSkeleton,
  loadFailed,
  activeDetailTab,
  detailTabs,
  authorTemplates,
  decideTests,
  decideApprovals,
  publishTemplates,
  showAuthorJourney,
  authorJourneyContext,
  showTesterJourney,
  testerJourneyContext,
  showApproverJourney,
  approverJourneyContext,
  showTeamLeadJourney,
  teamLeadJourneyContext,
  authorJourneyPrimaryCtaDisabled,
  handleJourneyCreate,
  handleJourneyDesign,
  handleJourneyTrialGenerate,
  handleJourneySubmitForTest,
  handleJourneySubmitForApproval,
  handleJourneyReviewRequest,
  handleJourneyCheckEvidence,
  handleJourneyRecordResult,
  handleJourneyApproverReviewRequest,
  handleJourneyApproverReviewSubmission,
  handleJourneyApproverRecordDecision,
  handleJourneyTeamLeadReviewGoLiveRequest,
  handleJourneyTeamLeadRunPreReleaseChecks,
  handleJourneyTeamLeadConfirmGoLive,
  showLifecycleSection,
  showGovernanceSection,
  showDraftActions,
  showTestingDecisionActions,
  showSubmitForApproval,
  showApprovalDecisionActions,
  showPublishActions,
  showTestGenerate,
  showStopAction,
  showRestoreAction,
  showDeprecateAction,
  showAuthoringSection,
  canEditContentModuleReferences,
  showPolicyPanel,
  showExportActions,
  showDeleteTemplateAction,
  showMetadataEdit,
  policyLoadFailed,
  apiPolicy,
  loadingPolicy,
  policySubmitting,
  policyLoadErrorKey,
  publishGateItems,
  publishGateReady,
  publishVersion,
  publishBumpLevel,
  publishBumpOptions,
  publishVersionConflict,
  loadingPublishGate,
  publishGateLoadError,
  publishCoverageSummary,
  publishChangeDiffSummary,
  submitGateItems,
  submitGateReady,
  loadingSubmitGate,
  submitGateLoadError,
  submitCoverageSummary,
  submitChangeDiffSummary,
  bindingGateResult,
  lifecycleComment,
  lifecycleCommentDialogOpen,
  decisionDialogOpen,
  decisionDialogMode,
  publishSummaryOpen,
  submitSummaryOpen,
  metadataEditOpen,
  credentialSecretDialogVisible,
  credentialSecretValue,
  credentialSecretExternalId,
  displayedCredentialSecret,
  lastPreview,
  selectedPreviewId,
  selectedTestDataSetId,
  generatingPreview,
  generatingPreviewId,
  batchTesting,
  coverageRefreshToken,
  submitForTestDialogOpen,
  credentialColumnFilters,
  credentialsCurrentPage,
  paginatedCredentials,
  credentialStatusFilterOptions,
  totalCredentialRows,
  sortCredentialsByCreatedAt,
  selectedContractEnvironment,
  templatesStore,
  loadTemplate,
  loadPublishGateData,
  loadSubmitGateData,
  loadPolicyData,
  backToList,
  openLifecyclePanel,
  handleTestGenerate,
  handleBatchTestGenerate,
  handlePreviewSelected,
  handlePreviewRefreshed,
  handleSubmitForTest,
  handleTestDecision,
  submitLifecycleDecision,
  handleSubmitForApproval,
  confirmSubmitFromSummary,
  handleApprovalDecision,
  handlePublish,
  confirmPublishFromSummary,
  handleGovernanceAction,
  handleMetadataUpdate,
  handleCreateCredential,
  handleRotateCredential,
  handleRevokeCredential,
  handleDeleteTemplate,
} = useTemplateDetailController(toRef(props, 'workspace'))
</script>

<template>
  <AppPageLayout>
    <TemplateWorkspaceHeader
      :template-name="template?.name ?? t('templates.packageHub.loadingTitle')"
      :group-label="template ? t('templates.detail.groupLabel', { groupCode: template.groupCode }) : undefined"
      :status="template?.lifecycleStatus"
      :approval-sub-state="template?.approvalSubState"
      :back-label="isDevEditor ? t('templates.releaseDetail.backToHub') : t('templates.detail.backToList')"
      @back="backToList"
    >
      <template v-if="template" #actions>
        <TemplateExportActions
          v-if="showExportActions"
          :template-id="templateId"
          :external-id="template.externalId"
        />
        <el-button
          v-if="showDeleteTemplateAction"
          type="danger"
          plain
          :loading="templatesStore.submitting"
          @click="handleDeleteTemplate"
        >
          {{ t('templates.deleteAction.button') }}
        </el-button>
        <el-button v-if="showMetadataEdit" @click="metadataEditOpen = true">
          {{ t('templates.metadata.edit') }}
        </el-button>
      </template>
    </TemplateWorkspaceHeader>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="templatesStore.lastErrorMessageKey ?? 'templates.error.loadDetail'"
      @retry="loadTemplate"
    />

    <el-skeleton v-else-if="showDetailSkeleton" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!template"
      title-key="templates.empty.notFoundTitle"
      description-key="templates.empty.notFoundDescription"
    />

    <template v-else-if="template">
      <TemplateDetailJourneyStack
        v-if="!isDevEditor"
        :template="template"
        :template-id="templateId"
        :show-author-journey="showAuthorJourney"
        :author-journey-context="authorJourneyContext"
        :author-templates="authorTemplates"
        :author-journey-primary-cta-disabled="authorJourneyPrimaryCtaDisabled"
        :show-tester-journey="showTesterJourney"
        :tester-journey-context="testerJourneyContext"
        :decide-tests="decideTests"
        :show-approver-journey="showApproverJourney"
        :approver-journey-context="approverJourneyContext"
        :decide-approvals="decideApprovals"
        :show-team-lead-journey="showTeamLeadJourney"
        :team-lead-journey-context="teamLeadJourneyContext"
        :publish-templates="publishTemplates"
        @create="handleJourneyCreate"
        @design="handleJourneyDesign"
        @trial-generate="handleJourneyTrialGenerate"
        @submit-for-test="handleJourneySubmitForTest"
        @submit-for-approval="handleJourneySubmitForApproval"
        @review-request="handleJourneyReviewRequest"
        @check-evidence="handleJourneyCheckEvidence"
        @record-result="handleJourneyRecordResult"
        @review-approver-request="handleJourneyApproverReviewRequest"
        @review-submission="handleJourneyApproverReviewSubmission"
        @record-decision="handleJourneyApproverRecordDecision"
        @review-go-live-request="handleJourneyTeamLeadReviewGoLiveRequest"
        @run-pre-release-checks="handleJourneyTeamLeadRunPreReleaseChecks"
        @confirm-go-live="handleJourneyTeamLeadConfirmGoLive"
        @open-lifecycle="openLifecyclePanel"
      />

      <TemplateDetailDevWorkspace
        v-if="isDevEditor && showAuthoringSection"
        :template-id="templateId"
        :master-id="template.masterId"
        :variables="template.variables"
        :bindings="template.bindings"
        :rules="template.rules"
        :group-code="template.groupCode"
        :lifecycle-status="template.lifecycleStatus"
        :can-edit-content-module-references="canEditContentModuleReferences"
        :coverage-refresh-token="coverageRefreshToken"
        :last-preview="lastPreview"
        :selected-preview-id="selectedPreviewId"
        :selected-test-data-set-id="selectedTestDataSetId"
        :show-draft-actions="showDraftActions"
        :show-testing-decision-actions="showTestingDecisionActions"
        :show-submit-for-approval="showSubmitForApproval"
        :show-approval-decision-actions="showApprovalDecisionActions"
        :show-publish-actions="showPublishActions"
        :show-test-generate="showTestGenerate"
        :show-stop-action="showStopAction"
        :show-restore-action="showRestoreAction"
        :show-deprecate-action="showDeprecateAction"
        :show-governance-section="showGovernanceSection"
        :publish-gate-items="publishGateItems"
        :loading-publish-gate="loadingPublishGate"
        :publish-bump-level="publishBumpLevel"
        :publish-version-conflict="publishVersionConflict"
        :publish-gate-ready="publishGateReady"
        :publish-bump-options="publishBumpOptions"
        :binding-gate-result="bindingGateResult"
        :publish-gate-load-error="publishGateLoadError"
        :submit-gate-items="submitGateItems"
        :loading-submit-gate="loadingSubmitGate"
        :submit-gate-ready="submitGateReady"
        :submit-gate-load-error="submitGateLoadError"
        :submitting="templatesStore.submitting"
        :generating-preview="generatingPreview"
        :generating-preview-id="generatingPreviewId"
        :batch-testing="batchTesting"
        :open-submit-for-test-dialog="submitForTestDialogOpen"
        @updated="loadTemplate"
        @update:selected-test-data-set-id="selectedTestDataSetId = $event"
        @update:selected-preview-id="handlePreviewSelected"
        @update:publish-bump-level="publishBumpLevel = $event"
        @update:open-submit-for-test-dialog="submitForTestDialogOpen = $event"
        @test-generate="handleTestGenerate"
        @test-generate-batch="handleBatchTestGenerate"
        @submit-for-test="handleSubmitForTest"
        @test-decision="handleTestDecision"
        @submit-for-approval="handleSubmitForApproval"
        @approval-decision="handleApprovalDecision"
        @publish="handlePublish"
        @governance-action="handleGovernanceAction"
        @retry-publish-gate="loadPublishGateData"
        @retry-submit-gate="loadSubmitGateData"
        @preview-refreshed="handlePreviewRefreshed"
      />

      <TemplateDetailLegacyWorkspace
        v-if="!isDevEditor"
        v-model:active-detail-tab="activeDetailTab"
        v-model:publish-bump-level="publishBumpLevel"
        v-model:credential-column-filters="credentialColumnFilters"
        v-model:credentials-current-page="credentialsCurrentPage"
        v-model:selected-contract-environment="selectedContractEnvironment"
        :template="template"
        :template-id="templateId"
        :detail-tabs="detailTabs"
        :format-date-time="formatDateTime"
        :show-lifecycle-section="showLifecycleSection"
        :show-governance-section="showGovernanceSection"
        :show-draft-actions="showDraftActions"
        :show-testing-decision-actions="showTestingDecisionActions"
        :show-submit-for-approval="showSubmitForApproval"
        :show-approval-decision-actions="showApprovalDecisionActions"
        :show-publish-actions="showPublishActions"
        :show-test-generate="showTestGenerate"
        :show-stop-action="showStopAction"
        :show-restore-action="showRestoreAction"
        :show-deprecate-action="showDeprecateAction"
        :show-authoring-section="showAuthoringSection"
        :can-edit-content-module-references="canEditContentModuleReferences"
        :show-policy-panel="showPolicyPanel"
        :coverage-refresh-token="coverageRefreshToken"
        :publish-gate-items="publishGateItems"
        :loading-publish-gate="loadingPublishGate"
        :publish-version-conflict="publishVersionConflict"
        :publish-gate-ready="publishGateReady"
        :publish-bump-options="publishBumpOptions"
        :binding-gate-result="bindingGateResult"
        :publish-gate-load-error="publishGateLoadError"
        :submit-gate-items="submitGateItems"
        :loading-submit-gate="loadingSubmitGate"
        :submit-gate-ready="submitGateReady"
        :submit-gate-load-error="submitGateLoadError"
        :submitting="templatesStore.submitting"
        :loading-policy="loadingPolicy"
        :api-policy="apiPolicy"
        :policy-load-failed="policyLoadFailed"
        :policy-load-error-key="policyLoadErrorKey"
        :paginated-credentials="paginatedCredentials"
        :credential-status-filter-options="credentialStatusFilterOptions"
        :total-credential-rows="totalCredentialRows"
        :policy-submitting="policySubmitting"
        :sort-credentials-by-created-at="sortCredentialsByCreatedAt"
        @open-submit-for-test="lifecycleCommentDialogOpen = true"
        @test-decision="handleTestDecision"
        @submit-for-approval="handleSubmitForApproval"
        @approval-decision="handleApprovalDecision"
        @publish="handlePublish"
        @test-generate="handleTestGenerate()"
        @governance-action="handleGovernanceAction"
        @retry-publish-gate="loadPublishGateData"
        @retry-submit-gate="loadSubmitGateData"
        @updated="loadTemplate"
        @create-credential="handleCreateCredential"
        @rotate-credential="handleRotateCredential"
        @revoke-credential="handleRevokeCredential"
        @retry-policy-load="loadPolicyData"
      />
    </template>

    <TemplateDetailDialogs
      v-if="template"
      v-model:metadata-edit-open="metadataEditOpen"
      v-model:publish-summary-open="publishSummaryOpen"
      v-model:submit-summary-open="submitSummaryOpen"
      v-model:decision-dialog-open="decisionDialogOpen"
      v-model:lifecycle-comment-dialog-open="lifecycleCommentDialogOpen"
      v-model:credential-secret-dialog-visible="credentialSecretDialogVisible"
      :template-name="template.name"
      :template-description="template.description ?? null"
      :template-id="templateId"
      :submitting="templatesStore.submitting"
      :publish-version="publishVersion"
      :publish-gate-items="publishGateItems"
      :publish-coverage-summary="publishCoverageSummary"
      :publish-change-diff-summary="publishChangeDiffSummary"
      :submit-gate-items="submitGateItems"
      :submit-coverage-summary="submitCoverageSummary"
      :submit-change-diff-summary="submitChangeDiffSummary"
      :preview-comparison="lastPreview?.previewComparison ?? null"
      :decision-dialog-mode="decisionDialogMode"
      :lifecycle-comment="lifecycleComment"
      :credential-secret-external-id="credentialSecretExternalId"
      :displayed-credential-secret="displayedCredentialSecret"
      :credential-secret-value="credentialSecretValue"
      @metadata-submit="handleMetadataUpdate"
      @confirm-publish="confirmPublishFromSummary"
      @confirm-submit="confirmSubmitFromSummary"
      @submit-decision="submitLifecycleDecision"
      @submit-for-test="handleSubmitForTest"
    />
  </AppPageLayout>
</template>
