<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import LifecycleCommentDialog from '@/components/common/LifecycleCommentDialog.vue'
import BatchTestProgressDialog from '@/components/template/BatchTestProgressDialog.vue'
import TemplateDetailDesignTab from '@/views/templates/detail/TemplateDetailDesignTab.vue'
import TemplateDetailTestingTab from '@/views/templates/detail/TemplateDetailTestingTab.vue'
import TemplateDetailApprovalTab from '@/views/templates/detail/TemplateDetailApprovalTab.vue'
import * as templatesApi from '@/api/templates'
import { useSubmitTestEligibility } from '@/composables/useSubmitTestEligibility'
import {
  TEMPLATE_DEV_WORKSPACE_TABS,
  buildDevWorkspaceQuery,
  resolveTemplateDevWorkspaceTabFromQuery,
  templateDevWorkspaceTabLabelKey,
  type TemplateDevWorkspaceTab,
} from '@/views/templates/templateDevWorkspaceTabs'
import type { SemverBumpLevel } from '@/utils/semver'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type {
  AnchorBinding,
  BindingValidationResult,
  CompositionRule,
  PreviewRecord,
  TemplateLifecycleStatus,
  VariableSchema,
} from '@/types/template'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

type GovernanceAction = 'stop' | 'restore' | 'deprecate'

const props = defineProps<{
  templateId: string
  masterId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[] | null
  groupCode: string | null
  lifecycleStatus: TemplateLifecycleStatus
  canEditContentModuleReferences: boolean
  coverageRefreshToken: number
  lastPreview: PreviewRecord | null
  selectedPreviewId: string | null
  selectedTestDataSetId: string | null
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showSubmitForApproval: boolean
  showApprovalDecisionActions: boolean
  showPublishActions: boolean
  showTestGenerate: boolean
  showStopAction: boolean
  showRestoreAction: boolean
  showDeprecateAction: boolean
  showGovernanceSection: boolean
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishBumpLevel: SemverBumpLevel
  publishVersionConflict: boolean
  publishGateReady: boolean
  publishBumpOptions: PublishBumpOption[]
  submitGateItems: PublishGateDisplayItem[]
  loadingSubmitGate: boolean
  submitGateReady: boolean
  submitGateLoadError: string | null
  bindingGateResult: BindingValidationResult | null
  publishGateLoadError: string | null
  submitting: boolean
  generatingPreview: boolean
  generatingPreviewId: string | null
  batchTesting: boolean
  openSubmitForTestDialog?: boolean
}>()

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
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const activeWorkspaceTab = ref<TemplateDevWorkspaceTab>(
  resolveTemplateDevWorkspaceTabFromQuery(route.query),
)
const testDataSetCount = ref(0)
const submitForTestDialogOpen = ref(false)

// T09: Batch test dialog state
const batchDialogVisible = ref(false)
const batchDialogRunId = ref('')
const batchDialogStreamUrl = ref('')
const batchRunning = ref(false)

// T10: Submit eligibility composable (only load when templateId is known)
  const { isEligible, tooltipContent, refresh: refreshEligibility } = useSubmitTestEligibility(
  props.templateId,
)

const submitTooltipContent = computed(() => {
  if (testDataSetCount.value === 0) {
    return t('templates.testPreview.workflow.noDataSetsTooltip')
  }
  return tooltipContent.value
})

const submitTooltipDisabled = computed(() => isEligible.value && testDataSetCount.value > 0)

const workspaceTabs = computed(() =>
  TEMPLATE_DEV_WORKSPACE_TABS.map((name) => ({
    name,
    labelKey: templateDevWorkspaceTabLabelKey(name),
  })),
)

watch(
  () => route.query,
  () => {
    const resolved = resolveTemplateDevWorkspaceTabFromQuery(route.query)
    if (activeWorkspaceTab.value !== resolved) {
      activeWorkspaceTab.value = resolved
    }
  },
  { deep: true },
)

watch(activeWorkspaceTab, (tab) => {
  if (resolveTemplateDevWorkspaceTabFromQuery(route.query) === tab) {
    return
  }
  void router.replace({ query: buildDevWorkspaceQuery(route.query, tab) })
})

watch(
  () => props.openSubmitForTestDialog,
  (requested) => {
    if (requested) {
      activeWorkspaceTab.value = 'testing'
      submitForTestDialogOpen.value = true
      emit('update:openSubmitForTestDialog', false)
    }
  },
)

function handleSubmitForTestConfirm(comment: string) {
  submitForTestDialogOpen.value = false
  emit('submit-for-test', comment)
}

function requestSubmitForTestDialog() {
  submitForTestDialogOpen.value = true
}

async function handleRunFullTest() {
  try {
    await ElMessageBox.confirm(
      t('templates.batchTest.confirmMessage', { count: testDataSetCount.value }),
      t('templates.batchTest.confirmTitle'),
      {
        confirmButtonText: t('templates.batchTest.confirmButton'),
        cancelButtonText: t('templates.batchTest.cancelButton'),
        type: 'info',
      },
    )
  } catch {
    return
  }

  batchRunning.value = true
  try {
    const result = await templatesApi.runBatchTest(props.templateId)
    batchDialogRunId.value = result.runId
    batchDialogStreamUrl.value = result.streamUrl
    batchDialogVisible.value = true
  } catch {
    ElMessage.error(t('templates.batchTest.error.start'))
  } finally {
    batchRunning.value = false
  }
}

function handleBatchCompleted() {
  emit('test-generate-batch')
  void refreshEligibility()
}

watch(batchDialogVisible, (visible, wasVisible) => {
  if (wasVisible && !visible) {
    void refreshEligibility()
  }
})

watch(
  activeWorkspaceTab,
  (tab) => {
    if (tab === 'testing') {
      void refreshEligibility()
    }
  },
  { immediate: false },
)
</script>

<template>
  <section id="dev-workspace" class="dev-workspace">
    <WorkspaceTabShell v-model="activeWorkspaceTab" :tabs="workspaceTabs">
      <template #actions>
        <template v-if="activeWorkspaceTab === 'testing'">
          <el-tooltip
            v-if="showTestGenerate"
            :content="testDataSetCount === 0 ? t('templates.testPreview.workflow.noDataSetsTooltip') : ''"
            :disabled="testDataSetCount > 0"
            placement="bottom"
          >
            <span>
              <el-button
                type="primary"
                :loading="batchRunning"
                :disabled="testDataSetCount === 0"
                @click="handleRunFullTest"
              >
                {{ t('templates.testPreview.workflow.runAll') }}
              </el-button>
            </span>
          </el-tooltip>

          <el-tooltip
            v-if="showDraftActions"
            :content="submitTooltipContent"
            :disabled="submitTooltipDisabled"
            placement="bottom"
            effect="light"
            popper-class="submit-test-tooltip"
          >
            <span>
              <el-button
                type="success"
                :loading="submitting"
                :disabled="!isEligible || testDataSetCount === 0"
                @click="requestSubmitForTestDialog"
              >
                {{ t('templates.lifecycle.submitTest') }}
              </el-button>
            </span>
          </el-tooltip>

          <template v-if="showTestingDecisionActions">
            <el-button type="success" :loading="submitting" @click="emit('test-decision', 'PASSED')">
              {{ t('templates.lifecycle.passTest') }}
            </el-button>
            <el-button type="danger" :loading="submitting" @click="emit('test-decision', 'FAILED')">
              {{ t('templates.lifecycle.failTest') }}
            </el-button>
          </template>
        </template>

        <template v-else-if="activeWorkspaceTab === 'approval'">
          <el-button
            v-if="showSubmitForApproval"
            type="primary"
            :loading="submitting"
            :disabled="!submitGateReady || loadingSubmitGate || Boolean(submitGateLoadError)"
            @click="emit('submitForApproval')"
          >
            {{ t('templates.lifecycle.submitApproval') }}
          </el-button>
          <template v-if="showApprovalDecisionActions">
            <el-button type="success" :loading="submitting" @click="emit('approvalDecision', 'APPROVED')">
              {{ t('templates.lifecycle.approve') }}
            </el-button>
            <el-button type="danger" :loading="submitting" @click="emit('approvalDecision', 'REJECTED')">
              {{ t('templates.lifecycle.reject') }}
            </el-button>
          </template>
          <el-button
            v-if="showPublishActions"
            type="primary"
            :loading="submitting"
            :disabled="!publishGateReady || loadingPublishGate"
            @click="emit('publish')"
          >
            {{ t('templates.lifecycle.publish') }}
          </el-button>
          <el-button
            v-if="showStopAction"
            type="warning"
            :loading="submitting"
            @click="emit('governanceAction', 'stop')"
          >
            {{ t('templates.governance.stop') }}
          </el-button>
          <el-button
            v-if="showRestoreAction"
            type="primary"
            :loading="submitting"
            @click="emit('governanceAction', 'restore')"
          >
            {{ t('templates.governance.restore') }}
          </el-button>
          <el-button
            v-if="showDeprecateAction"
            type="danger"
            :loading="submitting"
            @click="emit('governanceAction', 'deprecate')"
          >
            {{ t('templates.governance.deprecate') }}
          </el-button>
        </template>
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
          @updated="emit('updated')"
        />
      </template>

      <template #testing>
        <TemplateDetailTestingTab
          :template-id="templateId"
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

<style scoped lang="scss">
.dev-workspace {
  margin-top: 0.25rem;
}
</style>
