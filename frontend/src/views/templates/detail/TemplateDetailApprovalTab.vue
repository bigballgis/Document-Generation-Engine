<script setup lang="ts">
import { toRef } from 'vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import TemplateApprovalStageIndicator from '@/components/templates/TemplateApprovalStageIndicator.vue'
import TemplateRiskPromptConfigPanel from '@/components/templates/TemplateRiskPromptConfigPanel.vue'
import type { ApprovalMatrixMode, ApprovalStage, ApprovalSubState } from '@/types/approvalMatrix'
import type { SemverBumpLevel } from '@/utils/semver'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type { BindingValidationResult } from '@/types/template'
import { templateApprovalSubTabLabelKey } from '@/views/templates/templateApprovalSubTabs'
import TemplateDetailApprovalPublishPane from '@/views/templates/detail/TemplateDetailApprovalPublishPane.vue'
import { useTemplateDetailApprovalTab } from '@/views/templates/detail/useTemplateDetailApprovalTab'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

const props = defineProps<{
  templateId: string
  approvalMatrixMode?: ApprovalMatrixMode | null
  approvalSubState?: ApprovalSubState | null
  approvalStage?: ApprovalStage | null
  showSubmitForApproval: boolean
  showPublishActions: boolean
  showGovernanceSection: boolean
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishBumpLevel: SemverBumpLevel
  publishVersionConflict: boolean
  publishBumpOptions: PublishBumpOption[]
  submitGateItems: PublishGateDisplayItem[]
  loadingSubmitGate: boolean
  submitGateLoadError: string | null
  bindingGateResult: BindingValidationResult | null
  publishGateLoadError: string | null
}>()

const emit = defineEmits<{
  'update:publishBumpLevel': [value: SemverBumpLevel]
  retryPublishGate: []
  retrySubmitGate: []
}>()

const {
  t,
  activeSubTab,
  bindingGateIssues,
  bindingGateIssueMessageKey,
  invalidBindings,
  resolveBindingStatusLabel,
} = useTemplateDetailApprovalTab({
  bindingGateResult: toRef(props, 'bindingGateResult'),
})
</script>

<template>
  <el-card shadow="never" class="section-card">
    <div class="section-card__heading">
      <h2>{{ t('templates.devWorkspace.approval.title') }}</h2>
      <ContextHelpTrigger
        :title="t('templates.devEditor.releaseWorkflowHelpTitle')"
        :content="t('templates.devEditor.releaseWorkflowHelpContent')"
      />
    </div>

    <TemplateApprovalStageIndicator
      :approval-matrix-mode="approvalMatrixMode"
      :approval-sub-state="approvalSubState"
      :approval-stage="approvalStage"
    />

    <el-tabs v-model="activeSubTab" class="approval-sub-tabs">
      <el-tab-pane :label="t(templateApprovalSubTabLabelKey('submitApproval'))" name="submitApproval">
        <LoadErrorPanel
          v-if="showSubmitForApproval && submitGateLoadError"
          :message-key="submitGateLoadError"
          class="gate-error"
          @retry="emit('retrySubmitGate')"
        />
        <el-card
          v-else-if="showSubmitForApproval && submitGateItems.length"
          shadow="never"
          class="gate-card"
        >
          <h3>{{ t('templates.submitGate.title') }}</h3>
          <p>{{ t('templates.submitGate.description') }}</p>
          <el-skeleton v-if="loadingSubmitGate" :rows="2" animated />
          <ul v-else class="gate-list">
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
        <p v-else class="empty-hint">{{ t('templates.devWorkspace.approval.submitApprovalEmpty') }}</p>
      </el-tab-pane>

      <el-tab-pane :label="t(templateApprovalSubTabLabelKey('publishReadiness'))" name="publishReadiness">
        <LoadErrorPanel
          v-if="showPublishActions && publishGateLoadError"
          :message-key="publishGateLoadError"
          class="gate-error"
          @retry="emit('retryPublishGate')"
        />
        <TemplateDetailApprovalPublishPane
          v-else-if="showPublishActions"
          :binding-gate-result="bindingGateResult"
          :binding-gate-issues="bindingGateIssues"
          :binding-gate-issue-message-key="bindingGateIssueMessageKey"
          :invalid-bindings="invalidBindings"
          :resolve-binding-status-label="resolveBindingStatusLabel"
          :publish-gate-items="publishGateItems"
          :loading-publish-gate="loadingPublishGate"
          :publish-bump-level="publishBumpLevel"
          :publish-version-conflict="publishVersionConflict"
          :publish-bump-options="publishBumpOptions"
          @update:publish-bump-level="emit('update:publishBumpLevel', $event)"
        />
        <p v-else class="empty-hint">{{ t('templates.devWorkspace.approval.publishReadinessEmpty') }}</p>
      </el-tab-pane>

      <el-tab-pane :label="t(templateApprovalSubTabLabelKey('riskConfig'))" name="riskConfig">
        <TemplateRiskPromptConfigPanel :template-id="templateId" />
      </el-tab-pane>

      <el-tab-pane :label="t(templateApprovalSubTabLabelKey('governance'))" name="governance">
        <template v-if="showGovernanceSection">
          <h3 class="governance-title">{{ t('templates.governance.title') }}</h3>
          <p class="governance-description">{{ t('templates.governance.description') }}</p>
          <p class="governance-hint">{{ t('templates.devWorkspace.approval.governanceHint') }}</p>
        </template>
        <p v-else class="empty-hint">{{ t('templates.devWorkspace.approval.governanceEmpty') }}</p>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 0;

  &__heading {
    display: flex;
    align-items: center;
    gap: 0.25rem;
    margin-bottom: 1rem;
  }

  h2 {
    margin: 0;
    font-size: 1.125rem;
  }
}

.approval-sub-tabs {
  margin-top: 0.25rem;
}

.gate-card {
  margin-top: 0.75rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--border-color);

  h3 {
    margin: 0 0 0.5rem;
    font-size: 0.9375rem;
  }

  p {
    margin: 0 0 0.75rem;
    color: var(--text-muted);
  }
}

.gate-error {
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

.governance-title {
  margin: 0 0 0.5rem;
  font-size: 0.9375rem;
}

.governance-description,
.governance-hint,
.empty-hint {
  margin: 0;
  color: var(--text-muted);
}

.governance-hint {
  margin-top: 0.5rem;
  font-size: 0.875rem;
}
</style>
