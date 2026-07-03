import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import * as templatesApi from '@/api/templates'
import type { SubmitTestEligibility } from '@/types/template'

const MAX_TOOLTIP_NAMES = 5

function truncatedList(names: string[], t: (key: string, vals?: Record<string, unknown>) => string): string {
  if (names.length === 0) return ''
  if (names.length <= MAX_TOOLTIP_NAMES) return names.join(', ')
  const shown = names.slice(0, MAX_TOOLTIP_NAMES).join(', ')
  const extra = names.length - MAX_TOOLTIP_NAMES
  return t('templates.submitTestEligibility.uncoveredExtra', { names: shown, count: extra })
}

export function useSubmitTestEligibility(templateId: string) {
  const { t } = useI18n()
  const eligibility = ref<SubmitTestEligibility | null>(null)
  const loading = ref(false)
  const loadError = ref<string | null>(null)

  async function refresh() {
    loading.value = true
    loadError.value = null
    try {
      eligibility.value = await templatesApi.getSubmitTestEligibility(templateId)
    } catch {
      loadError.value = t('templates.submitTestEligibility.error.load')
    } finally {
      loading.value = false
    }
  }

  const isEligible = computed(() => eligibility.value?.eligible ?? false)

  const tooltipLines = computed<string[]>(() => {
    if (!eligibility.value || eligibility.value.eligible) return []
    const lines: string[] = []
    const e = eligibility.value
    if (!e.hasValidTestResult) {
      lines.push(t('templates.submitTestEligibility.noValidTestResult'))
    }
    if (!e.allSamplesSucceeded && e.failedDataSetNames.length > 0) {
      const names = truncatedList(e.failedDataSetNames, t)
      lines.push(t('templates.submitTestEligibility.hasFailed', { names }))
    }
    if (!e.coverageGatePassed) {
      const anchors = e.uncoveredAnchors.length > 0
        ? t('templates.submitTestEligibility.uncoveredAnchors', {
            names: truncatedList(e.uncoveredAnchors, t),
          })
        : ''
      const vars = e.uncoveredVariables.length > 0
        ? t('templates.submitTestEligibility.uncoveredVariables', {
            names: truncatedList(e.uncoveredVariables, t),
          })
        : ''
      lines.push(t('templates.submitTestEligibility.coverageNotMet'))
      if (anchors) lines.push(anchors)
      if (vars) lines.push(vars)
    }
    return lines
  })

  const tooltipContent = computed(() => tooltipLines.value.join('\n'))

  return { eligibility, loading, loadError, isEligible, tooltipContent, refresh }
}
