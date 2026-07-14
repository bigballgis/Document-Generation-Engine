import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as authorWorkflowApi from '@/api/authorWorkflow'
import { useAuthorWorkflowStore } from '@/stores/authorWorkflow'

vi.mock('@/api/authorWorkflow', () => ({
  listOutdatedClauseReferenceAuthorTasks: vi.fn(),
}))

describe('authorWorkflow store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(authorWorkflowApi.listOutdatedClauseReferenceAuthorTasks).mockReset()
  })

  it('loads outdated clause reference tasks', async () => {
    vi.mocked(authorWorkflowApi.listOutdatedClauseReferenceAuthorTasks).mockResolvedValue([
      {
        templateId: 'tpl-1',
        externalId: 'TPL-1',
        groupCode: 'RETAIL',
        name: 'Loan Notice',
        inFlightDevVersionId: 'dev-1',
        outdatedReferenceCount: 2,
        updatedAt: '2026-07-14T10:00:00Z',
      },
    ])

    const store = useAuthorWorkflowStore()
    await store.fetchOutdatedClauseReferenceTasks()

    expect(authorWorkflowApi.listOutdatedClauseReferenceAuthorTasks).toHaveBeenCalled()
    expect(store.outdatedClauseTasks).toHaveLength(1)
    expect(store.loadingOutdatedClauseTasks).toBe(false)
    expect(store.outdatedClauseTasksError).toBe(false)
  })

  it('clears tasks and sets error flag when fetch fails', async () => {
    vi.mocked(authorWorkflowApi.listOutdatedClauseReferenceAuthorTasks).mockRejectedValue(
      new Error('network'),
    )

    const store = useAuthorWorkflowStore()
    store.outdatedClauseTasks = [
      {
        templateId: 'tpl-old',
        externalId: 'OLD',
        groupCode: 'RETAIL',
        name: 'Old',
        inFlightDevVersionId: 'dev-old',
        outdatedReferenceCount: 1,
        updatedAt: '2026-07-01T00:00:00Z',
      },
    ]

    await store.fetchOutdatedClauseReferenceTasks()

    expect(store.outdatedClauseTasks).toEqual([])
    expect(store.outdatedClauseTasksError).toBe(true)
    expect(store.loadingOutdatedClauseTasks).toBe(false)
  })

  it('clears outdated clause tasks', () => {
    const store = useAuthorWorkflowStore()
    store.outdatedClauseTasks = [
      {
        templateId: 'tpl-1',
        externalId: 'TPL-1',
        groupCode: 'RETAIL',
        name: 'Loan Notice',
        inFlightDevVersionId: 'dev-1',
        outdatedReferenceCount: 1,
        updatedAt: '2026-07-14T10:00:00Z',
      },
    ]
    store.outdatedClauseTasksError = true

    store.clearOutdatedClauseReferenceTasks()

    expect(store.outdatedClauseTasks).toEqual([])
    expect(store.outdatedClauseTasksError).toBe(false)
  })
})
