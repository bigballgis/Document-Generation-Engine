<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import LifecycleCommentDialog from '@/components/common/LifecycleCommentDialog.vue'
import BatchTestProgressDialog from '@/components/template/BatchTestProgressDialog.vue'
import AuthoringPathGuide from '@/components/templates/AuthoringPathGuide.vue'
import AuthoringPathMasterPanel from '@/components/templates/AuthoringPathMasterPanel.vue'
import LifecycleStepper from '@/components/templates/LifecycleStepper.vue'
import TemplateDetailDesignTab from '@/views/templates/detail/TemplateDetailDesignTab.vue'
import TemplateDetailTestingTab from '@/views/templates/detail/TemplateDetailTestingTab.vue'
import TemplateDetailApprovalTab from '@/views/templates/detail/TemplateDetailApprovalTab.vue'
import TemplateDetailDevWorkspaceActions from '@/views/templates/detail/TemplateDetailDevWorkspaceActions.vue'
import { useTemplateDetailDevWorkspace } from '@/views/templates/detail/useTemplateDetailDevWorkspace'
import type { SemverBumpLevel } from '@/utils/semver'
import type { PreviewRecord } from '@/types/template'
import type { TemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'
import {
  dismissAuthoringPathGuide,
  isAuthoringPathGuideVisible,
  resolveAuthoringPathGuideStep,
  stripAuthoringPathGuideQuery,
  type AuthoringPathGuideNavigateQuery,
} from '@/utils/templateAuthoringPathGuide'
import {
  buildDevWorkspaceQuery,
  type TemplateDevWorkspaceSubTab,
} from '@/views/templates/templateDevWorkspaceTabs'
import type {
  GovernanceAction,
  TemplateDetailDevWorkspaceProps,
} from '@/views/templates/detail/templateDetailDevWorkspaceProps'

const props = defineProps<TemplateDetailDevWorkspaceProps>()

const emit = defineEmits<{
  updated: []
  'update:selectedTestDataSetId': [id: string | null]
  'update:selectedPreviewId': [previewId: string | null]
  'update:publishBumpLevel': [value: SemverBumpLevel]
  'update:openSubmitForTestDialog': [value: boolean]
  'test-generate': [testDataSetId: string | undefined]
  'test-generate-batch': []
  'submit-for-test': [comment: string]
  'test-decision': [decision: 'PASSED' | 'FAILED']
  submitForApproval: []
  approvalDecision: [decision: 'APPROVED' | 'REJECTED']
  publish: []
  governanceAction: [action: GovernanceAction]
  retryPublishGate: []
  retrySubmitGate: []
  'preview-refreshed': [preview: PreviewRecord]
}>()

const {
  activeWorkspaceTab,
  workspaceTabs,
  testDataSetCount,
  submitForTestDialogOpen,
  batchDialogVisible,
  batchDialogRunId,
  batchDialogStreamUrl,
  batchRunning,
  isEligible,
  submitTooltipContent,
  submitTooltipDisabled,
  handleSubmitForTestConfirm,
  requestSubmitForTestDialog,
  handleRunFullTest,
  handleBatchCompleted,
} = useTemplateDetailDevWorkspace({
  templateId: () => props.templateId,
  openSubmitForTestDialog: () => props.openSubmitForTestDialog,
  onClearOpenSubmitForTestDialog: () => emit('update:openSubmitForTestDialog', false),
  onSubmitForTest: (comment) => emit('submit-for-test', comment),
  onBatchCompleted: () => emit('test-generate-batch'),
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const authoringPathGuideVisible = computed(() =>
  isAuthoringPathGuideVisible(props.templateId, route.query),
)
const authoringPathGuideStep = computed(() => resolveAuthoringPathGuideStep(route.query))
const showAuthoringPathMasterPanel = computed(
  () => authoringPathGuideVisible.value && authoringPathGuideStep.value === 'master',
)

function resolveStepperSubTab(query: TemplateJourneyWorkspaceQuery): TemplateDevWorkspaceSubTab | undefined {
  if (typeof query.designTab === 'string') {
    return query.designTab as TemplateDevWorkspaceSubTab
  }
  if (typeof query.testingTab === 'string') {
    return query.testingTab as TemplateDevWorkspaceSubTab
  }
  if (typeof query.approvalTab === 'string') {
    return query.approvalTab as TemplateDevWorkspaceSubTab
  }
  return undefined
}

function onLifecycleStepperNavigate(query: TemplateJourneyWorkspaceQuery) {
  void router.replace({
    query: buildDevWorkspaceQuery(route.query, query.workspaceTab, resolveStepperSubTab(query)),
  })
}

function onAuthoringPathNavigate(query: AuthoringPathGuideNavigateQuery) {
  const workspaceQuery = buildDevWorkspaceQuery(
    route.query,
    query.workspaceTab,
    resolveStepperSubTab(query),
  )
  void router.replace({
    query: {
      ...workspaceQuery,
      authoringGuide: query.authoringGuide,
      authoringGuideStep: query.authoringGuideStep,
    },
  })
}

function onAuthoringPathDismiss() {
  dismissAuthoringPathGuide(props.templateId)
  void router.replace({
    query: stripAuthoringPathGuideQuery(route.query),
  })
}
</script>

<template>
  <section id="dev-workspace" class="dev-workspace">
    <LifecycleStepper
      :lifecycle-status="lifecycleStatus"
      :approval-sub-state="approvalSubState"
      @navigate="onLifecycleStepperNavigate"
    />
    <AuthoringPathGuide
      v-if="authoringPathGuideVisible"
      :current-step="authoringPathGuideStep"
      @navigate="onAuthoringPathNavigate"
      @dismiss="onAuthoringPathDismiss"
    />
    <AuthoringPathMasterPanel
      v-if="showAuthoringPathMasterPanel"
      :master-id="masterId"
    />
    <WorkspaceTabShell
      v-if="!showAuthoringPathMasterPanel"
      v-model="activeWorkspaceTab"
      :tabs="workspaceTabs"
    >
      <template #actions>
        <TemplateDetailDevWorkspaceActions
          :active-workspace-tab="activeWorkspaceTab"
          :show-test-generate="showTestGenerate"
          :show-draft-actions="showDraftActions"
          :show-testing-decision-actions="showTestingDecisionActions"
          :show-submit-for-approval="showSubmitForApproval"
          :show-approval-decision-actions="showApprovalDecisionActions"
          :show-publish-actions="showPublishActions"
          :show-stop-action="showStopAction"
          :show-restore-action="showRestoreAction"
          :show-deprecate-action="showDeprecateAction"
          :test-data-set-count="testDataSetCount"
          :batch-running="batchRunning"
          :submitting="submitting"
          :is-eligible="isEligible"
          :submit-tooltip-content="submitTooltipContent"
          :submit-tooltip-disabled="submitTooltipDisabled"
          :submit-gate-ready="submitGateReady"
          :loading-submit-gate="loadingSubmitGate"
          :submit-gate-load-error="submitGateLoadError"
          :publish-gate-ready="publishGateReady"
          :loading-publish-gate="loadingPublishGate"
          @run-full-test="handleRunFullTest"
          @request-submit-for-test="requestSubmitForTestDialog"
          @test-decision="emit('test-decision', $event)"
          @submit-for-approval="emit('submitForApproval')"
          @approval-decision="emit('approvalDecision', $event)"
          @publish="emit('publish')"
          @governance-action="emit('governanceAction', $event)"
        />
      </template>

      <template #design>
        <TemplateDetailDesignTab
          :template-id="templateId"
          :master-id="masterId"
          :variables="variables"
          :bindings="bindings"
          :rules="rules"
          :group-code="groupCode"
          :can-edit-content-module-references="canEditContentModuleReferences"
          :coverage-refresh-token="coverageRefreshToken"
          :last-preview="lastPreview"
          :selected-test-data-set-id="selectedTestDataSetId"
          :generating-preview="generatingPreview"
          @updated="emit('updated')"
          @preview-refreshed="emit('preview-refreshed', $event)"
        />
      </template>

      <template #testing>
        <TemplateDetailTestingTab
          :template-id="templateId"
          :variables="variables"
          :bindings="bindings"
          :coverage-refresh-token="coverageRefreshToken"
          :last-preview="lastPreview"
          :selected-preview-id="selectedPreviewId"
          :lifecycle-status="lifecycleStatus"
          :generating-preview-id="generatingPreviewId"
          :selected-test-data-set-id="selectedTestDataSetId"
          @update:selected-preview-id="emit('update:selectedPreviewId', $event)"
          @update:selected-test-data-set-id="emit('update:selectedTestDataSetId', $event)"
          @loaded-data-set-count="testDataSetCount = $event"
        />
      </template>

      <template #approval>
        <TemplateDetailApprovalTab
          :template-id="templateId"
          :show-submit-for-approval="showSubmitForApproval"
          :show-publish-actions="showPublishActions"
          :show-governance-section="showGovernanceSection"
          :publish-gate-items="publishGateItems"
          :loading-publish-gate="loadingPublishGate"
          :publish-bump-level="publishBumpLevel"
          :publish-version-conflict="publishVersionConflict"
          :publish-bump-options="publishBumpOptions"
          :submit-gate-items="submitGateItems"
          :loading-submit-gate="loadingSubmitGate"
          :submit-gate-load-error="submitGateLoadError"
          :binding-gate-result="bindingGateResult"
          :publish-gate-load-error="publishGateLoadError"
          @update:publish-bump-level="emit('update:publishBumpLevel', $event)"
          @retry-publish-gate="emit('retryPublishGate')"
          @retry-submit-gate="emit('retrySubmitGate')"
        />
      </template>
    </WorkspaceTabShell>

    <LifecycleCommentDialog
      v-model="submitForTestDialogOpen"
      :title="t('templates.testPreview.workflow.submitDialogTitle')"
      :message="t('templates.testPreview.workflow.submitDialogMessage')"
      :confirm-label="t('templates.lifecycle.submitTest')"
      :loading="submitting"
      @confirm="handleSubmitForTestConfirm"
    />

    <BatchTestProgressDialog
      v-if="batchDialogVisible"
      v-model="batchDialogVisible"
      :template-id="templateId"
      :run-id="batchDialogRunId"
      :stream-url="batchDialogStreamUrl"
      :data-set-count="testDataSetCount"
      @completed="handleBatchCompleted"
    />
  </section>
</template>

<style scoped lang="scss" src="./TemplateDetailDevWorkspace.scss"></style>
