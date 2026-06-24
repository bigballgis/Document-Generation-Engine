import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useMastersStore } from '@/stores/masters'
import * as mastersApi from '@/api/masters'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/masters', () => ({
  listMasters: vi.fn(),
  getMaster: vi.fn(),
  createMaster: vi.fn(),
  submitMasterReview: vi.fn(),
  decideMasterReview: vi.fn(),
  getMasterImpactAnalysis: vi.fn(),
}))

const sampleDetail = {
  id: 'master-1',
  groupCode: 'RETAIL',
  name: 'Retail letterhead',
  description: null,
  status: 'DRAFT' as const,
  originalFilename: 'letterhead.docx',
  changeSummary: null,
  anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block' }],
  reviewHistory: [],
  createdBy: '10000001',
  updatedBy: '10000001',
  createdAt: '2026-06-23T10:00:00Z',
  updatedAt: '2026-06-23T10:00:00Z',
}

describe('masters store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(mastersApi.listMasters).mockReset()
    vi.mocked(mastersApi.submitMasterReview).mockReset()
  })

  it('groups masters by group code', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue([
      {
        id: 'master-1',
        groupCode: 'RETAIL',
        name: 'Retail letterhead',
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 1,
        updatedBy: '10000001',
        updatedAt: '2026-06-23T10:00:00Z',
      },
      {
        id: 'master-2',
        groupCode: 'CORPORATE',
        name: 'Corporate letterhead',
        status: 'APPROVED',
        originalFilename: 'corp.docx',
        anchorCount: 3,
        updatedBy: '10000002',
        updatedAt: '2026-06-23T11:00:00Z',
      },
    ])

    const store = useMastersStore()
    await store.fetchMasters()

    expect(store.mastersByGroup.get('RETAIL')).toHaveLength(1)
    expect(store.mastersByGroup.get('CORPORATE')).toHaveLength(1)
  })

  it('updates list and detail after submit review', async () => {
    vi.mocked(mastersApi.submitMasterReview).mockResolvedValue({
      ...sampleDetail,
      status: 'PENDING_REVIEW',
      changeSummary: 'Updated anchors',
    })

    const store = useMastersStore()
    store.$patch({
      masters: [
        {
          id: 'master-1',
          groupCode: 'RETAIL',
          name: 'Retail letterhead',
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      selectedMaster: sampleDetail,
    })

    await store.submitReview('master-1', { changeSummary: 'Updated anchors' })

    expect(store.selectedMaster?.status).toBe('PENDING_REVIEW')
    expect(store.masters[0]?.status).toBe('PENDING_REVIEW')
  })

  it('records api error message key on list load failure', async () => {
    vi.mocked(mastersApi.listMasters).mockRejectedValue(
      axiosEnvelopeError(500, 'api.error.storage.operationFailed', {
        code: 'STORAGE_OPERATION_FAILED',
        category: 'STORAGE',
        message: 'Object storage operation failed.',
      }),
    )
    const store = useMastersStore()

    await expect(store.fetchMasters()).rejects.toBeTruthy()
    expect(store.lastErrorMessageKey).toBe('api.error.storage.operationFailed')
  })
})
