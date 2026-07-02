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

const props = defineProps<{
  templateId: string
  showLifecycleSection: boolean
  showGovernanceSection: boolean
  showSubmitForApproval: boolean
  showPublishActions: boolean
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
}>()

const emit = defineEmits<{
  'update:publishBumpLevel': [value: SemverBumpLevel]
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
  <div id="template-lifecycle-panel" class="lifecycle-tab">
    <template v-if="showLifecycleSection">
      <el-card v-if="showSubmitForApproval" shadow="never" class="section-card">
        <h2>{{ t('templates.lifecycle.title') }}</h2>

        <LoadErrorPanel
          v-if="submitGateLoadError"
          :message-key="submitGateLoadError"
          class="gate-error"
          @retry="emit('retrySubmitGate')"
        />
        <el-card v-else shadow="never" class="gate-card">
          <h3>{{ t('templates.submitGate.title') }}</h3>
          <p>{{ t('templates.submitGate.description') }}</p>
          <el-skeleton v-if="loadingSubmitGate" :rows="3" animated />
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
      </el-card>

      <el-card v-if="showPublishActions" shadow="never" class="section-card">
        <h2>{{ t('templates.lifecycle.title') }}</h2>

        <LoadErrorPanel
          v-if="publishGateLoadError"
          :message-key="publishGateLoadError"
          class="gate-error"
          @retry="emit('retryPublishGate')"
        />
        <template v-else>
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
            <el-alert
              v-if="bindingGateResult.summary.blocking"
              type="warning"
              :title="t('templates.authoring.bindingValidationBlocking')"
              show-icon
              :closable="false"
            />
          </el-card>

          <el-card shadow="never" class="gate-card">
            <h3>{{ t('templates.publishGate.title') }}</h3>
            <p>{{ t('templates.publishGate.description') }}</p>
            <el-skeleton v-if="loadingPublishGate" :rows="3" animated />
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
        </template>
      </el-card>

      <el-card shadow="never" class="section-card">
        <TemplateRiskPromptConfigPanel :template-id="templateId" />
      </el-card>
    </template>

    <el-card v-if="showGovernanceSection" shadow="never" class="section-card">
      <h2>{{ t('templates.governance.title') }}</h2>
      <p class="governance-description">{{ t('templates.governance.description') }}</p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.lifecycle-tab {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.125rem;
  }
}

.governance-description {
  margin: 0;
  color: var(--text-muted);
}

.gate-card {
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

.gate-list {
  margin: 0 0 0.75rem;
  padding-left: 1.25rem;

  li {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.35rem;
  }
}

.gate-error {
  width: 100%;
}

.publish-bump-picker {
  width: 100%;
}

.publish-bump-picker--wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  height: auto;
  margin-bottom: 1rem;

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
  margin-bottom: 1rem;
}
</style>
