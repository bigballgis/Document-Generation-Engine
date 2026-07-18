import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as dashboardApi from '@/api/dashboard'
import { useDashboardStore } from '@/stores/dashboard'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/dashboard', () => ({
  fetchDashboardSummary: vi.fn(),
}))

describe('dashboard store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(dashboardApi.fetchDashboardSummary).mockReset()
  })

  it('stores summary counts from the API', async () => {
    const payload = {
      masterPendingReview: 1,
      masterVersionsInProgress: 2,
      templateVersionsInWorkflow: 3,
      publishedVersions: 4,
      stoppedVersions: 0,
      catalogMasters: 5,
      catalogTemplates: 6,
    }
    vi.mocked(dashboardApi.fetchDashboardSummary).mockResolvedValue(payload)

    const store = useDashboardStore()
    await store.fetchSummary()

    expect(store.summary).toEqual(payload)
    expect(store.summaryErrorMessageKey).toBeNull()
  })

  it('clears summary and records messageKey on failure', async () => {
    vi.mocked(dashboardApi.fetchDashboardSummary).mockRejectedValue(
      axiosEnvelopeError(500, 'api.error.system.internalError', {
        code: 'INTERNAL_ERROR',
        category: 'SYSTEM',
        message: 'Internal error',
        retryable: true,
      }),
    )

    const store = useDashboardStore()
    await expect(store.fetchSummary()).rejects.toBeTruthy()

    expect(store.summary).toBeNull()
    expect(store.summaryErrorMessageKey).toBe('api.error.system.internalError')
  })
})
