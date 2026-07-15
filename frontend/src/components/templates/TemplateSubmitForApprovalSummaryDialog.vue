<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ChangeDiffHumanReadableList from '@/components/templates/ChangeDiffHumanReadableList.vue'
import type { ChangeDiffSummary, CoverageSummary, PreviewComparison } from '@/types/template'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'

const props = defineProps<{
  modelValue: boolean
  templateName: string
  gateItems: PublishGateDisplayItem[]
  coverageSummary: CoverageSummary | null
  changeDiffSummary: ChangeDiffSummary | null
  previewComparison: PreviewComparison | null
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const requiredItems = computed(() => props.gateItems.filter((item) => !item.informational))
const readyCount = computed(() => requiredItems.value.filter((item) => item.ready).length)
const requiredCount = computed(() => requiredItems.value.length)
const hasBlockers = computed(() => requiredItems.value.some((item) => !item.ready))

const coverageStatusKey = computed(() => {
  if (!props.coverageSummary) {
    return 'templates.submitApprovalSummary.coverageUnavailable'
  }
  return props.coverageSummary.belowThreshold
    ? 'templates.submitApprovalSummary.coverageBelowThreshold'
    : 'templates.submitApprovalSummary.coverageMeetsThreshold'
})

const changeDiffStatusKey = computed(() => {
  if (!props.changeDiffSummary) {
    return 'templates.submitApprovalSummary.changeDiffUnavailable'
  }
  return props.changeDiffSummary.hasChanges
    ? 'templates.submitApprovalSummary.changeDiffHasChanges'
    : 'templates.submitApprovalSummary.changeDiffNoChanges'
})

const previewComparisonStatusKey = computed(() => {
  if (!props.previewComparison) {
    return 'templates.submitApprovalSummary.previewComparisonUnavailable'
  }
  if (props.previewComparison.blockerCount > 0) {
    return 'templates.submitApprovalSummary.previewComparisonHasBlockers'
  }
  if (props.previewComparison.totalDiffCount > 0) {
    return 'templates.submitApprovalSummary.previewComparisonHasWarnings'
  }
  return 'templates.submitApprovalSummary.previewComparisonClean'
})

function close() {
  visible.value = false
}

function confirm() {
  emit('confirm')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.submitApprovalSummary.title')"
    width="640px"
    :close-on-click-modal="false"
    @close="close"
  >
    <p class="submit-summary-intro">
      {{ t('templates.submitApprovalSummary.description', { name: templateName }) }}
    </p>

    <section class="submit-summary-section">
      <h3>{{ t('templates.submitApprovalSummary.checklistTitle') }}</h3>
      <p class="submit-summary-progress">
        {{
          t('templates.submitApprovalSummary.checklistProgress', {
            ready: readyCount,
            total: requiredCount,
          })
        }}
      </p>
      <ul class="submit-summary-list">
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

    <section class="submit-summary-section">
      <h3>{{ t('templates.submitApprovalSummary.validationTitle') }}</h3>
      <p>
        {{
          coverageSummary
            ? t(coverageStatusKey, { percentage: coverageSummary.aggregatePercentage })
            : t('templates.submitApprovalSummary.coverageUnavailable')
        }}
      </p>
      <p>
        {{
          changeDiffSummary
            ? t(changeDiffStatusKey, { count: changeDiffSummary.totalChangeCount })
            : t('templates.submitApprovalSummary.changeDiffUnavailable')
        }}
      </p>
      <ChangeDiffHumanReadableList :change-diff-summary="changeDiffSummary" />
      <p>
        {{
          previewComparison
            ? t(previewComparisonStatusKey, {
                total: previewComparison.totalDiffCount,
                blockers: previewComparison.blockerCount,
                warnings: previewComparison.warningCount,
              })
            : t('templates.submitApprovalSummary.previewComparisonUnavailable')
        }}
      </p>
      <p v-if="hasBlockers" class="submit-summary-note submit-summary-blocker">
        {{ t('templates.submitApprovalSummary.blockersPresent') }}
      </p>
    </section>

    <template #footer>
      <el-button @click="close">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" :disabled="hasBlockers" @click="confirm">
        {{ t('templates.submitApprovalSummary.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.submit-summary-intro {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.submit-summary-section {
  margin-bottom: 1rem;

  h3 {
    margin: 0 0 0.5rem;
    font-size: 0.95rem;
  }

  p {
    margin: 0;
  }
}

.submit-summary-progress {
  margin-bottom: 0.5rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.submit-summary-list {
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

.submit-summary-note {
  margin-top: 0.35rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.submit-summary-blocker {
  color: var(--color-danger, #c45656);
}
</style>
