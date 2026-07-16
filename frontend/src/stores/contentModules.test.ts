import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as contentModulesApi from '@/api/contentModules'
import { useContentModulesStore } from '@/stores/contentModules'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/contentModules', () => ({
  listContentModules: vi.fn(),
  listContentModuleWorkflowTasks: vi.fn(),
  getContentModule: vi.fn(),
  createContentModule: vi.fn(),
}))

describe('contentModules store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(contentModulesApi.listContentModules).mockReset()
  })

  it('loads modules from paginated API and stores totals', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue({
      content: [
        {
          moduleId: 'MOD-LOAN-DISCLOSURE',
          moduleCode: 'MOD-LOAN-DISCLOSURE',
          groupCode: 'RETAIL',
          name: 'Loan disclosure',
          reviewState: 'DRAFT',
          createdAt: '2026-06-26T10:00:00Z',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const store = useContentModulesStore()
    await store.fetchModules(0, 20, {
      groupCode: 'RETAIL',
      status: 'DRAFT',
      sort: 'groupCodeAsc',
    })

    expect(contentModulesApi.listContentModules).toHaveBeenCalledWith(0, 20, {
      groupCode: 'RETAIL',
      status: 'DRAFT',
      sort: 'groupCodeAsc',
    })
    expect(store.modules).toHaveLength(1)
    expect(store.moduleListTotalElements).toBe(1)
    expect(store.activeGroupCode).toBe('RETAIL')
  })

  it('records list retryable flag on load failure', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockRejectedValue(
      axiosEnvelopeError(503, 'api.error.generation.serviceUnavailable', {
        retryable: true,
      }),
    )
    const store = useContentModulesStore()

    await expect(store.fetchModules()).rejects.toBeTruthy()
    expect(store.lastListErrorRetryable).toBe(true)
  })
})
