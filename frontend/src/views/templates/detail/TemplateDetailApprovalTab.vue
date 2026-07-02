<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import TemplateRiskPromptConfigPanel from '@/components/templates/TemplateRiskPromptConfigPanel.vue'
import type { SemverBumpLevel } from '@/utils/semver'
import {
  listInvalidBindings,
  mapBindingGateIssueItems,
  type BindingGateIssueItem,
} from '@/utils/templateBindingGateDisplay'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type { BindingValidationResult } from '@/types/template'
import {
  buildDevWorkspaceQuery,
  resolveApprovalSubTabFromQuery,
} from '@/views/templates/templateDevWorkspaceTabs'
import {
  templateApprovalSubTabLabelKey,
  type TemplateApprovalSubTab,
} from '@/views/templates/templateApprovalSubTabs'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

const props = defineProps<{
  templateId: string
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

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()

const activeSubTab = ref<TemplateApprovalSubTab>(resolveApprovalSubTabFromQuery(route.query))

watch(
  () => [route.query.approvalTab, route.query.focus],
  () => {
    activeSubTab.value = resolveApprovalSubTabFromQuery(route.query)
  },
)

watch(activeSubTab, (tab) => {
  if (resolveApprovalSubTabFromQuery(route.query) === tab) {
    return
  }
  void router.replace({
    query: buildDevWorkspaceQuery(route.query, 'approval', tab),
  })
})

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
  <el-card shadow="never" class="section-card">
    <div class="section-card__heading">
      <h2>{{ t('templates.devWorkspace.approval.title') }}</h2>
      <ContextHelpTrigger
        :title="t('templates.devEditor.releaseWorkflowHelpTitle')"
        :content="t('templates.devEditor.releaseWorkflowHelpContent')"
      />
    </div>

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
        <template v-else-if="showPublishActions">
          <el-card v-if="bindingGateResult" shadow="never" class="gate-card">
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

          <el-card shadow="never" class="gate-card">
            <h3>{{ t('templates.publishGate.title') }}</h3>
            <p>{{ t('templates.publishGate.description') }}</p>
            <el-skeleton v-if="loadingPublishGate" :rows="2" animated />
            <ul v-else class="gate-list">
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
