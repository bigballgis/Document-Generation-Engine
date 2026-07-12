<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'

type GovernanceAction = 'stop' | 'restore' | 'deprecate'

defineProps<{
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
  submitGateReady: boolean
  loadingSubmitGate: boolean
  submitGateLoadError: string | null
  publishGateReady: boolean
  loadingPublishGate: boolean
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

const { t } = useI18n()
</script>

<template>
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
          :disabled="!isEligible || testDataSetCount === 0"
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
