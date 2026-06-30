<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { SemverBumpLevel } from '@/utils/semver'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

type GovernanceAction = 'stop' | 'restore' | 'deprecate'

defineProps<{
  showLifecycleSection: boolean
  showGovernanceSection: boolean
  lifecycleComment: string
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showSubmitForApproval: boolean
  showApprovalDecisionActions: boolean
  showPublishActions: boolean
  showTestGenerate: boolean
  showStopAction: boolean
  showRestoreAction: boolean
  showDeprecateAction: boolean
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishBumpLevel: SemverBumpLevel
  publishVersionConflict: boolean
  publishGateReady: boolean
  publishBumpOptions: PublishBumpOption[]
  submitting: boolean
}>()

const emit = defineEmits<{
  'update:lifecycleComment': [value: string]
  'update:publishBumpLevel': [value: SemverBumpLevel]
  submitForTest: []
  testDecision: [decision: 'PASSED' | 'FAILED']
  submitForApproval: []
  approvalDecision: [decision: 'APPROVED' | 'REJECTED']
  publish: []
  testGenerate: []
  governanceAction: [action: GovernanceAction]
}>()

const { t } = useI18n()
</script>

<template>
  <el-card
    v-if="showLifecycleSection"
    id="template-lifecycle-panel"
    shadow="never"
    class="section-card"
  >
    <h2>{{ t('templates.lifecycle.title') }}</h2>
    <el-input
      :model-value="lifecycleComment"
      type="textarea"
      :rows="2"
      :placeholder="t('templates.lifecycle.commentPlaceholder')"
      class="lifecycle-comment"
      @update:model-value="emit('update:lifecycleComment', $event)"
    />
    <div class="action-row">
      <el-button
        v-if="showDraftActions"
        type="primary"
        :loading="submitting"
        @click="emit('submitForTest')"
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
      <template v-if="showPublishActions">
        <el-card shadow="never" class="publish-gate-card">
          <h3>{{ t('templates.publishGate.title') }}</h3>
          <p>{{ t('templates.publishGate.description') }}</p>
          <el-skeleton v-if="loadingPublishGate" :rows="3" animated />
          <ul v-else class="publish-gate-list">
            <li v-for="item in publishGateItems" :key="item.key">
              <span>{{ item.label }}</span>
              <el-tag v-if="item.informational" type="info" size="small">
                {{ t('templates.publishGate.informational') }}
              </el-tag>
              <el-tag v-else :type="item.ready ? 'success' : 'warning'" size="small">
                {{ item.ready ? t('templates.publishGate.ready') : t('templates.publishGate.pending') }}
              </el-tag>
            </li>
          </ul>
        </el-card>
        <el-radio-group
          :model-value="publishBumpLevel"
          class="publish-bump-picker"
          @update:model-value="emit('update:publishBumpLevel', $event)"
        >
          <el-radio-button
            v-for="option in publishBumpOptions"
            :key="option.level"
            :value="option.level"
          >
            {{ option.label }} ({{ option.version }})
          </el-radio-button>
        </el-radio-group>
        <el-alert
          v-if="publishVersionConflict"
          class="publish-conflict-alert"
          type="warning"
          :title="t('templates.lifecycle.releaseVersionConflict')"
          show-icon
          :closable="false"
        />
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!publishGateReady"
          @click="emit('publish')"
        >
          {{ t('templates.lifecycle.publish') }}
        </el-button>
      </template>
      <el-button v-if="showTestGenerate" :loading="submitting" @click="emit('testGenerate')">
        {{ t('templates.testGenerate.action') }}
      </el-button>
    </div>
  </el-card>

  <el-card v-if="showGovernanceSection" shadow="never" class="section-card">
    <h2>{{ t('templates.governance.title') }}</h2>
    <p class="governance-description">{{ t('templates.governance.description') }}</p>
    <div class="action-row">
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
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.125rem;
  }
}

.governance-description {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.lifecycle-comment {
  margin-bottom: 1rem;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.publish-bump-picker {
  width: 100%;
}

.publish-conflict-alert {
  width: 100%;
}

.publish-gate-card {
  width: 100%;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border-subtle, #e5e7eb);
  border-radius: 8px;

  h3 {
    margin: 0 0 0.5rem;
    font-size: 1rem;
  }

  p {
    margin: 0 0 0.75rem;
    color: var(--text-muted);
  }
}

.publish-gate-list {
  margin: 0;
  padding-left: 1.25rem;
}
</style>
