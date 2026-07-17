import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import {
  listAnnualReviewDueAuthorTasks,
  listOutdatedClauseReferenceAuthorTasks,
} from '@/api/authorWorkflow'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
  },
}))

describe('authorWorkflow api', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
  })

  it('lists outdated clause reference author tasks', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        result: [
          {
            templateId: 'tpl-1',
            externalId: 'TPL-1',
            groupCode: 'RETAIL',
            name: 'Loan Notice',
            outdatedReferenceCount: 2,
            updatedAt: '2026-07-14T10:00:00Z',
            inFlightDevVersionId: 'dev-1',
          },
        ],
      },
    })

    const tasks = await listOutdatedClauseReferenceAuthorTasks()

    expect(http.get).toHaveBeenCalledWith('/author-workflow/outdated-clause-references')
    expect(tasks).toHaveLength(1)
    expect(tasks[0].outdatedReferenceCount).toBe(2)
  })

  it('lists annual review due author tasks (CE-G05)', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        result: [
          {
            templateId: 'tpl-2',
            externalId: 'TPL-2',
            groupCode: 'RETAIL',
            name: 'Facility Letter',
            nextReviewDue: '2026-07-17',
            lifecycleStatus: 'PUBLISHED',
            updatedAt: '2026-07-10T10:00:00Z',
          },
        ],
      },
    })

    const tasks = await listAnnualReviewDueAuthorTasks()

    expect(http.get).toHaveBeenCalledWith('/author-workflow/annual-review-due-tasks')
    expect(tasks).toHaveLength(1)
    expect(tasks[0]?.nextReviewDue).toBe('2026-07-17')
  })

  it('returns empty array when result is missing', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: {} })

    await expect(listOutdatedClauseReferenceAuthorTasks()).resolves.toEqual([])
    await expect(listAnnualReviewDueAuthorTasks()).resolves.toEqual([])
  })
})
