import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import { listOutdatedClauseReferenceAuthorTasks } from '@/api/authorWorkflow'

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

  it('returns empty array when result is missing', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: {} })

    await expect(listOutdatedClauseReferenceAuthorTasks()).resolves.toEqual([])
  })
})
