import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as dashboardApi from '@/api/dashboard'
import type { DashboardSummaryView } from '@/api/dashboard'
import { resolveApiErrorMessageKey } from '@/api/http'

const ZERO_SUMMARY: DashboardSummaryView = {
  masterPendingReview: 0,
  masterVersionsInProgress: 0,
  templateVersionsInWorkflow: 0,
  publishedVersions: 0,
  stoppedVersions: 0,
  catalogMasters: 0,
  catalogTemplates: 0,
}

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<DashboardSummaryView | null>(null)
  const loadingSummary = ref(false)
  const summaryErrorMessageKey = ref<string | null>(null)

  async function fetchSummary(): Promise<void> {
    loadingSummary.value = true
    summaryErrorMessageKey.value = null
    try {
      summary.value = await dashboardApi.fetchDashboardSummary()
    } catch (error) {
      summary.value = null
      summaryErrorMessageKey.value = resolveApiErrorMessageKey(
        error,
        'dashboard.summary.error.load',
      )
      throw error
    } finally {
      loadingSummary.value = false
    }
  }

  function clearSummary(): void {
    summary.value = null
    summaryErrorMessageKey.value = null
  }

  return {
    summary,
    loadingSummary,
    summaryErrorMessageKey,
    fetchSummary,
    clearSummary,
  }
})

export { ZERO_SUMMARY as DASHBOARD_ZERO_SUMMARY }
