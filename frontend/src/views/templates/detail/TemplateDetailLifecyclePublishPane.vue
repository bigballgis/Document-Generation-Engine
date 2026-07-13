<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import type { SemverBumpLevel } from '@/utils/semver'
import type { BindingGateIssueItem } from '@/utils/templateBindingGateDisplay'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type { AnchorBinding, BindingValidationResult } from '@/types/template'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

const props = defineProps<{
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishBumpLevel: SemverBumpLevel
  publishVersionConflict: boolean
  publishBumpOptions: PublishBumpOption[]
  bindingGateResult: BindingValidationResult | null
  publishGateLoadError: string | null
  bindingGateIssues: BindingGateIssueItem[]
  bindingGateIssueMessageKey: Record<BindingGateIssueItem['issueKey'], string>
  invalidBindings: AnchorBinding[]
  resolveBindingStatusLabel: (status: string | undefined) => string
}>()

const emit = defineEmits<{
  'update:publishBumpLevel': [value: SemverBumpLevel]
  retryPublishGate: []
}>()

const { t } = useI18n()

const showAdGroupsNotConfiguredWarning = computed(() =>
  props.publishGateItems.some(
    (item) => item.key === 'API_POLICY' && item.adGroupsConfigured === false,
  ),
)
</script>

<template>
  <el-card shadow="never" class="section-card">
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
            </li>
          </ul>
        </template>
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
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.125rem;
  }
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

.ad-groups-warning {
  margin-bottom: 0.75rem;
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
