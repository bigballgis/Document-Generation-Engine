<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import TemplateRiskPromptConfigPanel from '@/components/templates/TemplateRiskPromptConfigPanel.vue'
import type { SemverBumpLevel } from '@/utils/semver'
import {
  listInvalidBindings,
  mapBindingGateIssueItems,
  type BindingGateIssueItem,
} from '@/utils/templateBindingGateDisplay'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type { BindingValidationResult } from '@/types/template'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

type GovernanceAction = 'stop' | 'restore' | 'deprecate'

const props = defineProps<{
  templateId: string
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
  submitGateItems: PublishGateDisplayItem[]
  loadingSubmitGate: boolean
  submitGateReady: boolean
  submitGateLoadError: string | null
  submitting: boolean
  bindingGateResult: BindingValidationResult | null
  publishGateLoadError: string | null
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
  retryPublishGate: []
  retrySubmitGate: []
}>()

const { t, te } = useI18n()

const bindingGateIssues = computed(() =>
  props.bindingGateResult ? mapBindingGateIssueItems(props.bindingGateResult.summary) : [],
)

const bindingGateIssueMessageKey: Record<BindingGateIssueItem['issueKey'], string> = {
  missingAnchor: 'templates.bindingGate.issueMissingAnchor',
  duplicateBinding: 'templates.bindingGate.issueDuplicateBinding',
  incompatibleContentType: 'templates.bindingGate.issueIncompatibleContentType',
}

const invalidBindings = computed(() =>
  props.bindingGateResult ? listInvalidBindings(props.bindingGateResult.bindings) : [],
)

function resolveBindingStatusLabel(status: string | undefined): string {
  if (!status) {
    return status ?? ''
  }
  const key = `templates.bindingGate.status.${status}`
  return te(key) ? t(key) : status
}
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
      <template v-if="showSubmitForApproval">
        <LoadErrorPanel
          v-if="submitGateLoadError"
          :message-key="submitGateLoadError"
          class="submit-gate-error"
          @retry="emit('retrySubmitGate')"
        />
        <template v-else>
          <el-card shadow="never" class="submit-gate-card">
            <h3>{{ t('templates.submitGate.title') }}</h3>
            <p>{{ t('templates.submitGate.description') }}</p>
            <el-skeleton v-if="loadingSubmitGate" :rows="3" animated />
            <ul v-else class="submit-gate-list">
              <li v-for="item in submitGateItems" :key="item.key">
                <span>{{ item.label }}</span>
                <el-tag v-if="item.informational" type="info" size="small">
                  {{ t('templates.submitGate.informational') }}
                </el-tag>
                <el-tag v-else :type="item.ready ? 'success' : 'warning'" size="small">
                  {{ item.ready ? t('templates.submitGate.ready') : t('templates.submitGate.pending') }}
                </el-tag>
              </li>
            </ul>
          </el-card>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!submitGateReady || loadingSubmitGate"
            @click="emit('submitForApproval')"
          >
            {{ t('templates.lifecycle.submitApproval') }}
          </el-button>
        </template>
      </template>
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
        <LoadErrorPanel
          v-if="publishGateLoadError"
          :message-key="publishGateLoadError"
          class="publish-gate-error"
          @retry="emit('retryPublishGate')"
        />
        <template v-else>
          <el-card v-if="bindingGateResult" shadow="never" class="binding-gate-card">
            <h3>{{ t('templates.bindingGate.title') }}</h3>
            <p>
              {{
                t('templates.bindingGate.summary', {
                  valid: bindingGateResult.summary.validCount,
                  total: bindingGateResult.summary.totalBindings,
                })
              }}
            </p>
            <ul v-if="bindingGateIssues.length" class="binding-gate-issues">
              <li v-for="issue in bindingGateIssues" :key="issue.issueKey">
                {{ t(bindingGateIssueMessageKey[issue.issueKey], { count: issue.count }) }}
              </li>
            </ul>
            <ul v-if="invalidBindings.length" class="binding-gate-invalid-list">
              <li v-for="binding in invalidBindings" :key="`${binding.anchorId}-${binding.validationStatus}`">
                {{
                  t('templates.bindingGate.invalidBindingLine', {
                    anchorId: binding.anchorId,
                    statusLabel: resolveBindingStatusLabel(binding.validationStatus),
                  })
                }}
              </li>
            </ul>
            <el-alert
              v-if="bindingGateResult.summary.blocking"
              type="warning"
              :title="t('templates.authoring.bindingValidationBlocking')"
              show-icon
              :closable="false"
            />
          </el-card>
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
            class="publish-bump-picker publish-bump-picker--wrap"
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
            :disabled="!publishGateReady || loadingPublishGate"
            @click="emit('publish')"
          >
            {{ t('templates.lifecycle.publish') }}
          </el-button>
        </template>
      </template>
      <el-button v-if="showTestGenerate" :loading="submitting" @click="emit('testGenerate')">
        {{ t('templates.testGenerate.action') }}
      </el-button>
    </div>
  </el-card>

  <el-card v-if="showLifecycleSection" shadow="never" class="section-card">
    <TemplateRiskPromptConfigPanel :template-id="templateId" />
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

.publish-bump-picker--wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  height: auto;

  :deep(.el-radio-button) {
    margin-left: 0;
  }

  :deep(.el-radio-button__inner) {
    border-left: 1px solid var(--el-border-color);
    border-radius: var(--el-border-radius-base);
  }
}

.publish-conflict-alert {
  width: 100%;
}

.publish-gate-error {
  width: 100%;
}

.submit-gate-error {
  width: 100%;
}

.binding-gate-card,
.publish-gate-card,
.submit-gate-card {
  width: 100%;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);

  h3 {
    margin: 0 0 0.5rem;
    font-size: 1rem;
  }

  p {
    margin: 0 0 0.75rem;
    color: var(--text-muted);
  }
}

.publish-gate-list,
.submit-gate-list,
.binding-gate-issues,
.binding-gate-invalid-list {
  margin: 0 0 0.75rem;
  padding-left: 1.25rem;
}
</style>
