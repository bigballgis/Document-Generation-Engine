<script setup lang="ts">
import { toRef } from 'vue'
import TemplateWorkflowBanner from '@/components/templates/TemplateWorkflowBanner.vue'
import TemplateAuthorJourneyBlock from '@/components/journey/TemplateAuthorJourneyBlock.vue'
import TemplateTesterJourneyBlock from '@/components/journey/TemplateTesterJourneyBlock.vue'
import TemplateApproverJourneyBlock from '@/components/journey/TemplateApproverJourneyBlock.vue'
import TemplateTeamLeadJourneyBlock from '@/components/journey/TemplateTeamLeadJourneyBlock.vue'
import TemplatePublishSummaryDialog from '@/components/templates/TemplatePublishSummaryDialog.vue'
import TemplateSubmitForApprovalSummaryDialog from '@/components/templates/TemplateSubmitForApprovalSummaryDialog.vue'
import TemplateLifecycleDecisionDialog from '@/components/templates/TemplateLifecycleDecisionDialog.vue'
import TemplateExportActions from '@/components/templates/TemplateExportActions.vue'
import TemplateMetadataEditDialog from '@/components/templates/TemplateMetadataEditDialog.vue'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import TemplateDetailLifecycleTab from '@/views/templates/detail/TemplateDetailLifecycleTab.vue'
import TemplateDetailAuthoringTab from '@/views/templates/detail/TemplateDetailAuthoringTab.vue'
import TemplateDetailDevWorkspace from '@/views/templates/detail/TemplateDetailDevWorkspace.vue'
import TemplateDetailReleaseVersionsTab from '@/views/templates/detail/TemplateDetailReleaseVersionsTab.vue'
import TemplateDetailApiAccessTab from '@/views/templates/detail/TemplateDetailApiAccessTab.vue'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import LifecycleCommentDialog from '@/components/templates/LifecycleCommentDialog.vue'
import TemplateWorkspaceHeader from '@/components/templates/TemplateWorkspaceHeader.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
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
      <template v-if="!isDevEditor">
        <TemplateAuthorJourneyBlock
          v-if="showAuthorJourney && authorJourneyContext"
          :journey-context="authorJourneyContext"
          :template-id="templateId"
          :can-write="authorTemplates"
          :primary-cta-disabled="authorJourneyPrimaryCtaDisabled"
          :show-primary-cta="false"
          @create="handleJourneyCreate"
          @design="handleJourneyDesign"
          @trial-generate="handleJourneyTrialGenerate"
          @submit-for-test="handleJourneySubmitForTest"
          @submit-for-approval="handleJourneySubmitForApproval"
        />

        <TemplateTesterJourneyBlock
          v-if="showTesterJourney && testerJourneyContext"
          :journey-context="testerJourneyContext"
          :can-decide="decideTests"
          :show-primary-cta="false"
          @review-request="handleJourneyReviewRequest"
          @check-evidence="handleJourneyCheckEvidence"
          @record-result="handleJourneyRecordResult"
        />

        <TemplateApproverJourneyBlock
          v-if="showApproverJourney && approverJourneyContext"
          :journey-context="approverJourneyContext"
          :can-decide="decideApprovals"
          :show-primary-cta="false"
          @review-request="handleJourneyApproverReviewRequest"
          @review-submission="handleJourneyApproverReviewSubmission"
          @record-decision="handleJourneyApproverRecordDecision"
        />

        <TemplateTeamLeadJourneyBlock
          v-if="showTeamLeadJourney && teamLeadJourneyContext"
          :journey-context="teamLeadJourneyContext"
          :can-publish="publishTemplates"
          :show-primary-cta="false"
          @review-go-live-request="handleJourneyTeamLeadReviewGoLiveRequest"
          @run-pre-release-checks="handleJourneyTeamLeadRunPreReleaseChecks"
          @confirm-go-live="handleJourneyTeamLeadConfirmGoLive"
        />

        <TemplateWorkflowBanner
          :template="template"
          @open-lifecycle="openLifecyclePanel"
        />
      </template>

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
      />

      <WorkspaceTabShell
        v-if="!isDevEditor"
        v-model="activeDetailTab"
        :tabs="detailTabs"
        class="detail-tabs"
      >
        <template #actions>
          <template v-if="activeDetailTab === 'lifecycle'">
            <el-button
              v-if="showDraftActions"
              type="primary"
              :loading="templatesStore.submitting"
              @click="lifecycleCommentDialogOpen = true"
            >
              {{ t('templates.lifecycle.submitTest') }}
            </el-button>
            <template v-if="showTestingDecisionActions">
              <el-button
                type="success"
                :loading="templatesStore.submitting"
                @click="handleTestDecision('PASSED')"
              >
                {{ t('templates.lifecycle.passTest') }}
              </el-button>
              <el-button
                type="danger"
                :loading="templatesStore.submitting"
                @click="handleTestDecision('FAILED')"
              >
                {{ t('templates.lifecycle.failTest') }}
              </el-button>
            </template>
            <el-button
              v-if="showSubmitForApproval"
              type="primary"
              :loading="templatesStore.submitting"
              :disabled="!submitGateReady || loadingSubmitGate"
              @click="handleSubmitForApproval"
            >
              {{ t('templates.lifecycle.submitApproval') }}
            </el-button>
            <template v-if="showApprovalDecisionActions">
              <el-button
                type="success"
                :loading="templatesStore.submitting"
                @click="handleApprovalDecision('APPROVED')"
              >
                {{ t('templates.lifecycle.approve') }}
              </el-button>
              <el-button
                type="danger"
                :loading="templatesStore.submitting"
                @click="handleApprovalDecision('REJECTED')"
              >
                {{ t('templates.lifecycle.reject') }}
              </el-button>
            </template>
            <el-button
              v-if="showPublishActions"
              type="primary"
              :loading="templatesStore.submitting"
              :disabled="!publishGateReady || loadingPublishGate"
              @click="handlePublish"
            >
              {{ t('templates.lifecycle.publish') }}
            </el-button>
            <el-button
              v-if="showTestGenerate"
              :loading="templatesStore.submitting"
              @click="handleTestGenerate()"
            >
              {{ t('templates.testGenerate.action') }}
            </el-button>
            <el-button
              v-if="showStopAction"
              type="warning"
              :loading="templatesStore.submitting"
              @click="handleGovernanceAction('stop')"
            >
              {{ t('templates.governance.stop') }}
            </el-button>
            <el-button
              v-if="showRestoreAction"
              type="primary"
              :loading="templatesStore.submitting"
              @click="handleGovernanceAction('restore')"
            >
              {{ t('templates.governance.restore') }}
            </el-button>
            <el-button
              v-if="showDeprecateAction"
              type="danger"
              :loading="templatesStore.submitting"
              @click="handleGovernanceAction('deprecate')"
            >
              {{ t('templates.governance.deprecate') }}
            </el-button>
          </template>
        </template>

        <template #overview>
          <TemplateDetailOverviewTab :template="template" :format-date-time="formatDateTime" />
        </template>

        <template #lifecycle>
          <TemplateDetailLifecycleTab
            :template-id="templateId"
            :show-lifecycle-section="showLifecycleSection"
            :show-governance-section="showGovernanceSection"
            :show-submit-for-approval="showSubmitForApproval"
            :show-publish-actions="showPublishActions"
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
            @update:publish-bump-level="publishBumpLevel = $event"
            @retry-publish-gate="loadPublishGateData"
            @retry-submit-gate="loadSubmitGateData"
          />
        </template>

        <template v-if="showAuthoringSection" #authoring>
          <TemplateDetailAuthoringTab
            :template-id="templateId"
            :master-id="template.masterId"
            :variables="template.variables"
            :bindings="template.bindings"
            :rules="template.rules"
            :group-code="template.groupCode"
            :can-edit-content-module-references="canEditContentModuleReferences"
            :coverage-refresh-token="coverageRefreshToken"
            @updated="loadTemplate"
          />
        </template>

        <template #releaseVersions>
          <TemplateDetailReleaseVersionsTab
            :template-id="templateId"
            :template-lifecycle-status="template.lifecycleStatus"
            @changed="loadTemplate"
          />
        </template>

        <template v-if="showPolicyPanel" #apiAccess>
          <TemplateDetailApiAccessTab
            v-model:credential-column-filters="credentialColumnFilters"
            v-model:credentials-current-page="credentialsCurrentPage"
            v-model:selected-contract-environment="selectedContractEnvironment"
            :template-id="templateId"
            :show-policy-panel="showPolicyPanel"
            :loading-policy="loadingPolicy"
            :api-policy="apiPolicy"
            :policy-load-failed="policyLoadFailed"
            :policy-load-error-key="policyLoadErrorKey"
            :paginated-credentials="paginatedCredentials"
            :credential-status-filter-options="credentialStatusFilterOptions"
            :page-size="CLIENT_TABLE_PAGE_SIZE"
            :total-credential-rows="totalCredentialRows"
            :submitting="policySubmitting"
            :format-date-time="formatDateTime"
            :sort-credentials-by-created-at="sortCredentialsByCreatedAt"
            @create-credential="handleCreateCredential"
            @rotate-credential="handleRotateCredential"
            @revoke-credential="handleRevokeCredential"
            @retry-policy-load="loadPolicyData"
          />
        </template>
      </WorkspaceTabShell>
    </template>

    <TemplateMetadataEditDialog
      v-if="template"
      v-model="metadataEditOpen"
      :initial-name="template.name"
      :initial-description="template.description ?? null"
      :loading="templatesStore.submitting"
      @submit="handleMetadataUpdate"
    />

    <TemplatePublishSummaryDialog
      v-if="template"
      v-model="publishSummaryOpen"
      :template-name="template.name"
      :release-version="publishVersion"
      :gate-items="publishGateItems"
      :coverage-summary="publishCoverageSummary"
      :change-diff-summary="publishChangeDiffSummary"
      :preview-comparison="lastPreview?.previewComparison ?? null"
      :loading="templatesStore.submitting"
      @confirm="confirmPublishFromSummary"
    />

    <TemplateSubmitForApprovalSummaryDialog
      v-if="template"
      v-model="submitSummaryOpen"
      :template-name="template.name"
      :gate-items="submitGateItems"
      :coverage-summary="submitCoverageSummary"
      :change-diff-summary="submitChangeDiffSummary"
      :preview-comparison="lastPreview?.previewComparison ?? null"
      :loading="templatesStore.submitting"
      @confirm="confirmSubmitFromSummary"
    />

    <TemplateLifecycleDecisionDialog
      v-model="decisionDialogOpen"
      :mode="decisionDialogMode"
      :template-id="templateId"
      :loading="templatesStore.submitting"
      :initial-comment="lifecycleComment"
      @submit="submitLifecycleDecision"
    />

    <LifecycleCommentDialog
      v-model="lifecycleCommentDialogOpen"
      :loading="templatesStore.submitting"
      @confirm="handleSubmitForTest"
    />

    <el-dialog
      v-model="credentialSecretDialogVisible"
      :title="t('templates.policy.credentialSecretDialogTitle')"
      width="480px"
      :close-on-click-modal="false"
    >
      <p>{{ t('templates.policy.credentialSecretHint') }}</p>
      <p>{{ t('templates.policy.credentialExternalId') }}: {{ credentialSecretExternalId }}</p>
      <el-input
        :model-value="displayedCredentialSecret || credentialSecretValue"
        readonly
        type="textarea"
        :rows="3"
      />
      <template #footer>
        <el-button type="primary" @click="credentialSecretDialogVisible = false">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.detail-tabs {
  margin-top: var(--space-2);
}
</style>
