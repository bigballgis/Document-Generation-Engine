import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as collaborationApi from '@/api/collaboration'
import { useCollaborationStore } from '@/stores/collaboration'

vi.mock('@/api/collaboration', () => ({
  listCollaborationWorkItems: vi.fn(),
}))

describe('collaboration store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(collaborationApi.listCollaborationWorkItems).mockReset()
  })

  it('loads work items into state', async () => {
    vi.mocked(collaborationApi.listCollaborationWorkItems).mockResolvedValue([
      {
        workItemId: 'wi-1',
        templateId: 'tpl-1',
        templateName: 'Loan Notice',
        groupCode: 'RETAIL',
        queue: 'TEST',
        triggerType: 'SUBMIT_FOR_TEST',
        submitterUserId: '10000003',
        summaryText: 'Template submitted for testing',
        createdAt: '2026-06-26T10:00:00Z',
        ageSeconds: 120,
      },
    ])

    const store = useCollaborationStore()
    await store.fetchWorkItems({ queue: 'TEST' })

    expect(collaborationApi.listCollaborationWorkItems).toHaveBeenCalledWith({ queue: 'TEST' })
    expect(store.workItems).toHaveLength(1)
    expect(store.loadingWorkItems).toBe(false)
  })

  it('records error message key when fetch fails', async () => {
    vi.mocked(collaborationApi.listCollaborationWorkItems).mockRejectedValue(new Error('network'))

    const store = useCollaborationStore()
    await expect(store.fetchWorkItems()).rejects.toThrow('network')
    expect(store.workItemsErrorMessageKey).toBeTruthy()
  })
})
