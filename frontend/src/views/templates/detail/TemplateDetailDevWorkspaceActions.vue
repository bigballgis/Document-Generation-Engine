<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'

type GovernanceAction = 'stop' | 'restore' | 'deprecate'

const props = defineProps<{
  activeWorkspaceTab: TemplateDevWorkspaceTab
  showTestGenerate: boolean
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showSubmitForApproval: boolean
  showApprovalDecisionActions: boolean
  showPublishActions: boolean
  showStopAction: boolean
  showRestoreAction: boolean
  showDeprecateAction: boolean
  testDataSetCount: number
  batchRunning: boolean
  submitting: boolean
  isEligible: boolean
  submitTooltipContent: string
  submitTooltipDisabled: boolean
  submitEligibilityLoadError: string | null
  submitGateReady: boolean
  loadingSubmitGate: boolean
  submitGateLoadError: string | null
  submitGateItems: PublishGateDisplayItem[]
  publishGateReady: boolean
  loadingPublishGate: boolean
  publishGateLoadError: string | null
  publishGateItems: PublishGateDisplayItem[]
}>()

const emit = defineEmits<{
  'run-full-test': []
  'request-submit-for-test': []
  'test-decision': [decision: 'PASSED' | 'FAILED']
  submitForApproval: []
  approvalDecision: [decision: 'APPROVED' | 'REJECTED']
  publish: []
  governanceAction: [action: GovernanceAction]
}>()

const { t, te } = useI18n()

function firstBlockingGateTooltip(
  items: PublishGateDisplayItem[],
  loadErrorKey: string | null,
): string {
  if (loadErrorKey) {
    return te(loadErrorKey) ? t(loadErrorKey) : loadErrorKey
  }
  const blocking = items.find((item) => !item.ready && !item.informational)
  return blocking?.label ?? ''
}

const submitApprovalTooltipContent = computed(() =>
  firstBlockingGateTooltip(props.submitGateItems, props.submitGateLoadError),
)

const submitApprovalDisabled = computed(
  () => !props.submitGateReady || props.loadingSubmitGate || Boolean(props.submitGateLoadError),
)

const submitApprovalTooltipDisabled = computed(
  () => !submitApprovalDisabled.value || !submitApprovalTooltipContent.value,
)

const publishTooltipContent = computed(() =>
  firstBlockingGateTooltip(props.publishGateItems, props.publishGateLoadError),
)

const publishDisabled = computed(
  () => !props.publishGateReady || props.loadingPublishGate || Boolean(props.publishGateLoadError),
)

const publishTooltipDisabled = computed(
  () => !publishDisabled.value || !publishTooltipContent.value,
)
</script>

<template>
  <div class="dev-workspace-actions">
    <el-alert
      v-if="activeWorkspaceTab === 'testing' && submitEligibilityLoadError"
      class="eligibility-load-error"
      type="error"
      :closable="false"
      show-icon
      data-testid="submit-eligibility-load-error"
      :title="submitEligibilityLoadError"
    />

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
            @click="emit('run-full-test')"
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
            :disabled="!isEligible || testDataSetCount === 0 || Boolean(submitEligibilityLoadError)"
            @click="emit('request-submit-for-test')"
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
      <el-tooltip
        v-if="showSubmitForApproval"
        :content="submitApprovalTooltipContent"
        :disabled="submitApprovalTooltipDisabled"
        placement="bottom"
        effect="light"
      >
        <span>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="submitApprovalDisabled"
            @click="emit('submitForApproval')"
          >
            {{ t('templates.lifecycle.submitApproval') }}
          </el-button>
        </span>
      </el-tooltip>
      <template v-if="showApprovalDecisionActions">
        <el-button type="success" :loading="submitting" @click="emit('approvalDecision', 'APPROVED')">
          {{ t('templates.lifecycle.approve') }}
        </el-button>
        <el-button type="danger" :loading="submitting" @click="emit('approvalDecision', 'REJECTED')">
          {{ t('templates.lifecycle.reject') }}
        </el-button>
      </template>
      <el-tooltip
        v-if="showPublishActions"
        :content="publishTooltipContent"
        :disabled="publishTooltipDisabled"
        placement="bottom"
        effect="light"
      >
        <span>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="publishDisabled"
            @click="emit('publish')"
          >
            {{ t('templates.lifecycle.publish') }}
          </el-button>
        </span>
      </el-tooltip>
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
  </div>
</template>

<style scoped lang="scss">
.dev-workspace-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.eligibility-load-error {
  flex: 1 1 100%;
  margin-bottom: 0.25rem;
}
</style>
