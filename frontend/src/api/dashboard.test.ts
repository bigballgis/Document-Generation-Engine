import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as dashboardApi from '@/api/dashboard'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
  },
}))

describe('dashboard API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
  })

  it('fetchDashboardSummary unwraps the envelope result', async () => {
    const summary = {
      masterPendingReview: 2,
      masterVersionsInProgress: 3,
      templateVersionsInWorkflow: 4,
      publishedVersions: 5,
      stoppedVersions: 1,
      catalogMasters: 10,
      catalogTemplates: 20,
    }
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: { traceId: 'tr-1' },
        result: summary,
      },
    })

    await expect(dashboardApi.fetchDashboardSummary()).resolves.toEqual(summary)
    expect(http.get).toHaveBeenCalledWith('/dashboard/summary')
  })
})
