<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
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

const hasWorkflowActions = computed(
  () =>
    props.showDraftActions ||
    props.showTestingDecisionActions ||
    props.showSubmitForApproval ||
    props.showApprovalDecisionActions ||
    props.showPublishActions ||
    props.showTestGenerate,
)

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
  <section
    v-if="hasWorkflowActions || showGovernanceSection"
    id="dev-version-actions"
    class="dev-version-actions"
  >
    <div v-if="hasWorkflowActions" class="dev-version-actions__primary">
      <p class="dev-version-actions__hint">{{ t('templates.devEditor.actionsHint') }}</p>
      <div class="dev-version-actions__row">
        <el-input
          :model-value="lifecycleComment"
          type="textarea"
          :rows="1"
          autosize
          :placeholder="t('templates.lifecycle.commentPlaceholder')"
          class="dev-version-actions__comment"
          @update:model-value="emit('update:lifecycleComment', $event)"
        />
        <div class="dev-version-actions__buttons">
          <el-button
            v-if="showDraftActions"
            type="primary"
            :loading="submitting"
            @click="emit('submitForTest')"
          >
            {{ t('templates.lifecycle.submitTest') }}
          </el-button>
          <template v-if="showTestingDecisionActions">
            <el-button type="success" :loading="submitting" @click="emit('testDecision', 'PASSED')">
              {{ t('templates.lifecycle.passTest') }}
            </el-button>
            <el-button type="danger" :loading="submitting" @click="emit('testDecision', 'FAILED')">
              {{ t('templates.lifecycle.failTest') }}
            </el-button>
          </template>
          <template v-if="showSubmitForApproval">
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!submitGateReady || loadingSubmitGate || Boolean(submitGateLoadError)"
              @click="emit('submitForApproval')"
            >
              {{ t('templates.lifecycle.submitApproval') }}
            </el-button>
          </template>
          <template v-if="showApprovalDecisionActions">
            <el-button type="success" :loading="submitting" @click="emit('approvalDecision', 'APPROVED')">
              {{ t('templates.lifecycle.approve') }}
            </el-button>
            <el-button type="danger" :loading="submitting" @click="emit('approvalDecision', 'REJECTED')">
              {{ t('templates.lifecycle.reject') }}
            </el-button>
          </template>
          <template v-if="showPublishActions">
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!publishGateReady || loadingPublishGate"
              @click="emit('publish')"
            >
              {{ t('templates.lifecycle.publish') }}
            </el-button>
          </template>
          <el-button v-if="showTestGenerate" :loading="submitting" @click="emit('testGenerate')">
            {{ t('templates.testGenerate.action') }}
          </el-button>
        </div>
      </div>
    </div>

    <LoadErrorPanel
      v-if="showSubmitForApproval && submitGateLoadError"
      :message-key="submitGateLoadError"
      class="dev-version-actions__gate-error"
      @retry="emit('retrySubmitGate')"
    />
    <el-card
      v-else-if="showSubmitForApproval && submitGateItems.length"
      shadow="never"
      class="dev-version-actions__gate-card"
    >
      <h3>{{ t('templates.submitGate.title') }}</h3>
      <el-skeleton v-if="loadingSubmitGate" :rows="2" animated />
      <ul v-else class="gate-list">
        <li v-for="item in submitGateItems" :key="item.key">
          <span>{{ item.label }}</span>
          <el-tag :type="item.ready ? 'success' : 'warning'" size="small">
            {{ item.ready ? t('templates.submitGate.ready') : t('templates.submitGate.pending') }}
          </el-tag>
        </li>
      </ul>
    </el-card>

    <LoadErrorPanel
      v-if="showPublishActions && publishGateLoadError"
      :message-key="publishGateLoadError"
      class="dev-version-actions__gate-error"
      @retry="emit('retryPublishGate')"
    />
    <template v-else-if="showPublishActions">
      <el-card v-if="bindingGateResult" shadow="never" class="dev-version-actions__gate-card">
        <h3>{{ t('templates.bindingGate.title') }}</h3>
        <p>
          {{
            t('templates.bindingGate.summary', {
              valid: bindingGateResult.summary.validCount,
              total: bindingGateResult.summary.totalBindings,
            })
          }}
        </p>
        <ul v-if="bindingGateIssues.length" class="gate-list">
          <li v-for="issue in bindingGateIssues" :key="issue.issueKey">
            {{ t(bindingGateIssueMessageKey[issue.issueKey], { count: issue.count }) }}
          </li>
        </ul>
        <ul v-if="invalidBindings.length" class="gate-list">
          <li v-for="binding in invalidBindings" :key="`${binding.anchorId}-${binding.validationStatus}`">
            {{
              t('templates.bindingGate.invalidBindingLine', {
                anchorId: binding.anchorId,
                statusLabel: resolveBindingStatusLabel(binding.validationStatus),
              })
            }}
          </li>
        </ul>
      </el-card>
      <el-card shadow="never" class="dev-version-actions__gate-card">
        <h3>{{ t('templates.publishGate.title') }}</h3>
        <el-skeleton v-if="loadingPublishGate" :rows="2" animated />
        <ul v-else class="gate-list">
          <li v-for="item in publishGateItems" :key="item.key">
            <span>{{ item.label }}</span>
            <el-tag :type="item.ready ? 'success' : 'warning'" size="small">
              {{ item.ready ? t('templates.publishGate.ready') : t('templates.publishGate.pending') }}
            </el-tag>
          </li>
        </ul>
        <el-radio-group
          :model-value="publishBumpLevel"
          class="publish-bump-picker publish-bump-picker--wrap"
          @update:model-value="emit('update:publishBumpLevel', $event)"
        >
          <el-radio-button v-for="option in publishBumpOptions" :key="option.level" :value="option.level">
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
      </el-card>
    </template>

    <div v-if="showGovernanceSection" class="dev-version-actions__governance">
      <span class="dev-version-actions__governance-label">{{ t('templates.governance.title') }}</span>
      <el-button v-if="showStopAction" type="warning" :loading="submitting" @click="emit('governanceAction', 'stop')">
        {{ t('templates.governance.stop') }}
      </el-button>
      <el-button v-if="showRestoreAction" type="primary" :loading="submitting" @click="emit('governanceAction', 'restore')">
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
  </section>
</template>

<style scoped lang="scss">
.dev-version-actions {
  margin-bottom: 1.25rem;
  padding: 1rem 1.25rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted, #fafbfc);
}

.dev-version-actions__hint {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.dev-version-actions__row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 0.75rem;
}

.dev-version-actions__comment {
  flex: 1 1 14rem;
  min-width: 12rem;
  max-width: 28rem;
}

.dev-version-actions__buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.dev-version-actions__gate-card {
  margin-top: 0.75rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--border-color);

  h3 {
    margin: 0 0 0.5rem;
    font-size: 0.9375rem;
  }
}

.dev-version-actions__gate-error {
  margin-top: 0.75rem;
}

.gate-list {
  margin: 0;
  padding-left: 1.25rem;

  li {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.35rem;
  }
}

.publish-bump-picker--wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.75rem;
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
  margin-top: 0.75rem;
}

.dev-version-actions__governance {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-color);
}

.dev-version-actions__governance-label {
  font-weight: 600;
  margin-right: 0.25rem;
}
</style>
