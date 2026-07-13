import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ChangeDiffSummary, CoverageSummary, PreviewComparison } from '@/types/template'
import {
  isPublishSummaryConfirmReady,
  type PublishGateDisplayItem,
} from '@/utils/templateLifecycleDecisionForm'

export function useTemplatePublishSummaryDialog(options: {
  modelValue: () => boolean
  gateItems: () => PublishGateDisplayItem[]
  coverageSummary: () => CoverageSummary | null
  changeDiffSummary: () => ChangeDiffSummary | null
  previewComparison: () => PreviewComparison | null
  emitUpdateModelValue: (value: boolean) => void
  emitConfirm: (payload: { fidelityViewedConfirmed: boolean }) => void
}) {
  const { t } = useI18n()
  const fidelityViewedConfirmed = ref(false)

  const visible = computed({
    get: () => options.modelValue(),
    set: (value: boolean) => options.emitUpdateModelValue(value),
  })

  const requiredItems = computed(() => options.gateItems().filter((item) => !item.informational))
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
    if (!options.coverageSummary()) {
      return 'templates.publishSummary.coverageUnavailable'
    }
    return options.coverageSummary()!.belowThreshold
      ? 'templates.publishSummary.coverageBelowThreshold'
      : 'templates.publishSummary.coverageMeetsThreshold'
  })

  const changeDiffStatusKey = computed(() => {
    if (!options.changeDiffSummary()) {
      return 'templates.publishSummary.changeDiffUnavailable'
    }
    return options.changeDiffSummary()!.hasChanges
      ? 'templates.publishSummary.changeDiffHasChanges'
      : 'templates.publishSummary.changeDiffNoChanges'
  })

  const previewComparisonStatusKey = computed(() => {
    const preview = options.previewComparison()
    if (!preview) {
      return 'templates.publishSummary.previewComparisonUnavailable'
    }
    if (preview.blockerCount > 0) {
      return 'templates.publishSummary.previewComparisonHasBlockers'
    }
    if (preview.totalDiffCount > 0) {
      return 'templates.publishSummary.previewComparisonHasWarnings'
    }
    return 'templates.publishSummary.previewComparisonClean'
  })

  watch(
    () => options.modelValue(),
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
    options.emitConfirm({ fidelityViewedConfirmed: fidelityViewedConfirmed.value })
  }

  return {
    t,
    fidelityViewedConfirmed,
    visible,
    readyCount,
    requiredCount,
    hasBlockers,
    confirmDisabled,
    coverageStatusKey,
    changeDiffStatusKey,
    previewComparisonStatusKey,
    close,
    confirm,
  }
}
