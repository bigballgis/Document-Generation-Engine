<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ChangeDiffSummary, CoverageSummary, PreviewComparison } from '@/types/template'
import {
  isPublishSummaryConfirmReady,
  type PublishGateDisplayItem,
} from '@/utils/templateLifecycleDecisionForm'

const props = defineProps<{
  modelValue: boolean
  templateName: string
  releaseVersion: string
  gateItems: PublishGateDisplayItem[]
  coverageSummary: CoverageSummary | null
  changeDiffSummary: ChangeDiffSummary | null
  previewComparison: PreviewComparison | null
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [payload: { fidelityViewedConfirmed: boolean }]
}>()

const { t } = useI18n()

const fidelityViewedConfirmed = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const requiredItems = computed(() => props.gateItems.filter((item) => !item.informational))
const readyCount = computed(() => requiredItems.value.filter((item) => item.ready).length)
const requiredCount = computed(() => requiredItems.value.length)
const hasBlockers = computed(() => requiredItems.value.some((item) => !item.ready))

const confirmDisabled = computed(
  () =>
    !isPublishSummaryConfirmReady({
      hasBlockers: hasBlockers.value,
      fidelityViewedConfirmed: fidelityViewedConfirmed.value,
    }),
)

const coverageStatusKey = computed(() => {
  if (!props.coverageSummary) {
    return 'templates.publishSummary.coverageUnavailable'
  }
  return props.coverageSummary.belowThreshold
    ? 'templates.publishSummary.coverageBelowThreshold'
    : 'templates.publishSummary.coverageMeetsThreshold'
})

const changeDiffStatusKey = computed(() => {
  if (!props.changeDiffSummary) {
    return 'templates.publishSummary.changeDiffUnavailable'
  }
  return props.changeDiffSummary.hasChanges
    ? 'templates.publishSummary.changeDiffHasChanges'
    : 'templates.publishSummary.changeDiffNoChanges'
})

const previewComparisonStatusKey = computed(() => {
  if (!props.previewComparison) {
    return 'templates.publishSummary.previewComparisonUnavailable'
  }
  if (props.previewComparison.blockerCount > 0) {
    return 'templates.publishSummary.previewComparisonHasBlockers'
  }
  if (props.previewComparison.totalDiffCount > 0) {
    return 'templates.publishSummary.previewComparisonHasWarnings'
  }
  return 'templates.publishSummary.previewComparisonClean'
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      fidelityViewedConfirmed.value = false
    }
  },
)

function close() {
  visible.value = false
}

function confirm() {
  if (confirmDisabled.value) {
    return
  }
  emit('confirm', { fidelityViewedConfirmed: fidelityViewedConfirmed.value })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.publishSummary.title')"
    width="560px"
    :close-on-click-modal="false"
    @close="close"
  >
    <p class="publish-summary-intro">
      {{ t('templates.publishSummary.description', { name: templateName }) }}
    </p>

    <dl class="publish-summary-release">
      <dt>{{ t('templates.publishSummary.releaseVersion') }}</dt>
      <dd>{{ releaseVersion }}</dd>
    </dl>

    <section class="publish-summary-section">
      <h3>{{ t('templates.publishSummary.checklistTitle') }}</h3>
      <p class="publish-summary-progress">
        {{ t('templates.publishSummary.checklistProgress', { ready: readyCount, total: requiredCount }) }}
      </p>
      <ul class="publish-summary-list">
        <li v-for="item in gateItems" :key="item.key">
          <span>{{ item.label }}</span>
          <el-tag v-if="item.informational" type="info" size="small">
            {{ t('templates.publishGate.informational') }}
          </el-tag>
          <el-tag v-else :type="item.ready ? 'success' : 'warning'" size="small">
            {{ item.ready ? t('templates.publishGate.ready') : t('templates.publishGate.pending') }}
          </el-tag>
        </li>
      </ul>
    </section>

    <section class="publish-summary-section">
      <h3>{{ t('templates.publishSummary.validationTitle') }}</h3>
      <p>
        {{
          coverageSummary
            ? t(coverageStatusKey, { percentage: coverageSummary.aggregatePercentage })
            : t('templates.publishSummary.coverageUnavailable')
        }}
      </p>
      <p>
        {{
          changeDiffSummary
            ? t(changeDiffStatusKey, { count: changeDiffSummary.totalChangeCount })
            : t('templates.publishSummary.changeDiffUnavailable')
        }}
      </p>
      <p>
        {{
          previewComparison
            ? t(previewComparisonStatusKey, {
                total: previewComparison.totalDiffCount,
                blockers: previewComparison.blockerCount,
                warnings: previewComparison.warningCount,
              })
            : t('templates.publishSummary.previewComparisonUnavailable')
        }}
      </p>
      <p v-if="hasBlockers" class="publish-summary-note publish-summary-blocker">
        {{ t('templates.publishSummary.blockersPresent') }}
      </p>
    </section>

    <section class="publish-summary-section">
      <el-checkbox
        v-model="fidelityViewedConfirmed"
        data-testid="confirm-fidelity-viewed"
      >
        {{ t('templates.lifecycle.decisionForm.confirmFidelityViewed') }}
      </el-checkbox>
    </section>

    <template #footer>
      <el-button @click="close">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" :disabled="confirmDisabled" @click="confirm">
        {{ t('templates.publishSummary.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.publish-summary-intro {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.publish-summary-release {
  display: grid;
  gap: 0.25rem;
  margin: 0 0 1rem;

  dt {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0;
    font-size: 1.125rem;
    font-weight: 600;
    font-family: monospace;
  }
}

.publish-summary-section {
  margin-bottom: 1rem;

  h3 {
    margin: 0 0 0.5rem;
    font-size: 0.95rem;
  }

  p {
    margin: 0;
  }
}

.publish-summary-progress {
  margin-bottom: 0.5rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.publish-summary-list {
  margin: 0;
  padding-left: 1.25rem;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    margin-bottom: 0.35rem;
  }
}

.publish-summary-note {
  margin-top: 0.35rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.publish-summary-blocker {
  color: var(--color-danger, #c45656);
}
</style>
