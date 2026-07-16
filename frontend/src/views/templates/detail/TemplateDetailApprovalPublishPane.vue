<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { SemverBumpLevel } from '@/utils/semver'
import type { BindingGateIssueItem } from '@/utils/templateBindingGateDisplay'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type { AnchorBinding, BindingValidationResult } from '@/types/template'
import { resolvePublishGateGoFixTarget } from '@/utils/publishGateGoFixLink'
import {
  buildDevWorkspaceQuery,
  type TemplateDevWorkspaceSubTab,
} from '@/views/templates/templateDevWorkspaceTabs'
import type { TemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

const props = defineProps<{
  bindingGateResult: BindingValidationResult | null
  bindingGateIssues: BindingGateIssueItem[]
  bindingGateIssueMessageKey: Record<BindingGateIssueItem['issueKey'], string>
  invalidBindings: AnchorBinding[]
  resolveBindingStatusLabel: (status: string | undefined) => string
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishBumpLevel: SemverBumpLevel
  publishVersionConflict: boolean
  publishBumpOptions: PublishBumpOption[]
}>()

const emit = defineEmits<{
  'update:publishBumpLevel': [value: SemverBumpLevel]
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const showAdGroupsNotConfiguredWarning = computed(() =>
  props.publishGateItems.some(
    (item) => item.key === 'API_POLICY' && item.adGroupsConfigured === false,
  ),
)

function goFixQueryFor(item: PublishGateDisplayItem): TemplateJourneyWorkspaceQuery | null {
  return resolvePublishGateGoFixTarget(item.key, item.ready)
}

function resolveGoFixSubTab(target: TemplateJourneyWorkspaceQuery): TemplateDevWorkspaceSubTab | undefined {
  if (typeof target.designTab === 'string') {
    return target.designTab as TemplateDevWorkspaceSubTab
  }
  if (typeof target.testingTab === 'string') {
    return target.testingTab as TemplateDevWorkspaceSubTab
  }
  if (typeof target.approvalTab === 'string') {
    return target.approvalTab as TemplateDevWorkspaceSubTab
  }
  return undefined
}

function navigateGoFix(item: PublishGateDisplayItem) {
  const target = goFixQueryFor(item)
  if (!target) {
    return
  }
  void router.replace({
    query: buildDevWorkspaceQuery(route.query, target.workspaceTab, resolveGoFixSubTab(target)),
  })
}
</script>

<template>
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
    <template v-else>
      <el-alert
        v-if="showAdGroupsNotConfiguredWarning"
        class="ad-groups-warning"
        type="warning"
        :title="t('templates.publishGate.adGroupsNotConfiguredTitle')"
        :description="t('templates.publishGate.adGroupsNotConfiguredDescription')"
        show-icon
        :closable="false"
        data-testid="publish-gate-ad-groups-warning"
      />
      <ul class="gate-list">
        <li v-for="item in publishGateItems" :key="item.key">
          <span>{{ item.label }}</span>
          <el-tag v-if="item.informational" type="info" size="small">
            {{ t('templates.publishGate.informational') }}
          </el-tag>
          <el-tag v-else :type="item.ready ? 'success' : 'warning'" size="small">
            {{ item.ready ? t('templates.publishGate.ready') : t('templates.publishGate.pending') }}
          </el-tag>
          <el-button
            v-if="goFixQueryFor(item)"
            link
            type="primary"
            size="small"
            :data-testid="`publish-gate-go-fix-${item.key}`"
            @click="navigateGoFix(item)"
          >
            {{ t('templates.lifecycleStepper.goFix') }}
          </el-button>
        </li>
      </ul>
    </template>
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

<style scoped lang="scss">
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

.ad-groups-warning {
  margin-bottom: 0.75rem;
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
</style>
