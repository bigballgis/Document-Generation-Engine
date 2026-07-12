<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  submitting: boolean
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showSubmitForApproval: boolean
  submitGateReady: boolean
  loadingSubmitGate: boolean
  showApprovalDecisionActions: boolean
  showPublishActions: boolean
  publishGateReady: boolean
  loadingPublishGate: boolean
  showTestGenerate: boolean
  showStopAction: boolean
  showRestoreAction: boolean
  showDeprecateAction: boolean
}>()

const emit = defineEmits<{
  openSubmitForTest: []
  testDecision: [decision: 'PASSED' | 'FAILED']
  submitForApproval: []
  approvalDecision: [decision: 'APPROVED' | 'REJECTED']
  publish: []
  testGenerate: []
  governanceAction: [action: 'stop' | 'restore' | 'deprecate']
}>()

const { t } = useI18n()
</script>

<template>
  <el-button
    v-if="showDraftActions"
    type="primary"
    :loading="submitting"
    @click="emit('openSubmitForTest')"
  >
    {{ t('templates.lifecycle.submitTest') }}
  </el-button>
  <template v-if="showTestingDecisionActions">
    <el-button
      type="success"
      :loading="submitting"
      @click="emit('testDecision', 'PASSED')"
    >
      {{ t('templates.lifecycle.passTest') }}
    </el-button>
    <el-button
      type="danger"
      :loading="submitting"
      @click="emit('testDecision', 'FAILED')"
    >
      {{ t('templates.lifecycle.failTest') }}
    </el-button>
  </template>
  <el-button
    v-if="showSubmitForApproval"
    type="primary"
    :loading="submitting"
    :disabled="!submitGateReady || loadingSubmitGate"
    @click="emit('submitForApproval')"
  >
    {{ t('templates.lifecycle.submitApproval') }}
  </el-button>
  <template v-if="showApprovalDecisionActions">
    <el-button
      type="success"
      :loading="submitting"
      @click="emit('approvalDecision', 'APPROVED')"
    >
      {{ t('templates.lifecycle.approve') }}
    </el-button>
    <el-button
      type="danger"
      :loading="submitting"
      @click="emit('approvalDecision', 'REJECTED')"
    >
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
    v-if="showTestGenerate"
    :loading="submitting"
    @click="emit('testGenerate')"
  >
    {{ t('templates.testGenerate.action') }}
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
