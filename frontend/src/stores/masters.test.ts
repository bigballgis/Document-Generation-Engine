import { AxiosError, AxiosHeaders } from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useMastersStore } from '@/stores/masters'
import * as mastersApi from '@/api/masters'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/masters', () => ({
  listMasters: vi.fn(),
  listAllMasters: vi.fn(),
  getMaster: vi.fn(),
  createMaster: vi.fn(),
  replaceMasterFile: vi.fn(),
  submitMasterReview: vi.fn(),
  decideMasterReview: vi.fn(),
  getMasterImpactAnalysis: vi.fn(),
  listMasterRevisionLines: vi.fn(),
  getMasterRevisionLine: vi.fn(),
  downloadMasterRevisionLineFile: vi.fn(),
  updateMasterRevisionLineAnchorDisplayLabel: vi.fn(),
}))

const sampleDetail = {
  id: 'master-1',
  groupCode: 'RETAIL',
  name: 'Retail letterhead',
  description: null,
  status: 'DRAFT' as const,
  originalFilename: 'letterhead.docx',
  changeSummary: null,
  anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block', documentSequence: 0 }],
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
    vi.mocked(mastersApi.listAllMasters).mockReset()
    vi.mocked(mastersApi.submitMasterReview).mockReset()
  })

  it('groups masters by group code', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue({
      content: [
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
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })

    const store = useMastersStore()
    await store.fetchMasters()

    expect(store.mastersByGroup.get('RETAIL')).toHaveLength(1)
    expect(store.mastersByGroup.get('CORPORATE')).toHaveLength(1)
    expect(store.masterListTotalElements).toBe(2)
  })

  it('forwards page/size and catalog filters to listMasters', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue({
      content: [],
      page: 1,
      size: 20,
      totalElements: 40,
      totalPages: 2,
    })

    const store = useMastersStore()
    await store.fetchMasters(1, 20, {
      search: 'letter',
      groupCode: 'RETAIL',
      status: 'DRAFT',
      sort: 'groupCodeAsc',
    })

    expect(mastersApi.listMasters).toHaveBeenCalledWith(1, 20, {
      search: 'letter',
      groupCode: 'RETAIL',
      status: 'DRAFT',
      sort: 'groupCodeAsc',
    })
    expect(store.masterListPage).toBe(1)
    expect(store.masterListTotalElements).toBe(40)
  })

  it('fetchAllMasters merges every page into the store list', async () => {
    vi.mocked(mastersApi.listAllMasters).mockResolvedValue({
      content: [
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
      ],
      totalElements: 125,
      truncated: false,
    })

    const store = useMastersStore()
    await store.fetchAllMasters({ sort: 'groupCodeAsc' })

    expect(mastersApi.listAllMasters).toHaveBeenCalledWith({ sort: 'groupCodeAsc' })
    expect(store.masters).toHaveLength(2)
    expect(store.masterListTotalElements).toBe(125)
  })

  it('fetchDashboardWorkflowMasters collects status-filtered candidates only', async () => {
    vi.mocked(mastersApi.listAllMasters).mockImplementation(async (options = {}) => {
      if (options.status === 'PENDING_REVIEW') {
        return {
          content: [
            {
              id: 'master-pending',
              groupCode: 'RETAIL',
              name: 'Pending letterhead',
              status: 'PENDING_REVIEW',
              originalFilename: 'pending.docx',
              anchorCount: 1,
              updatedBy: '10000001',
              updatedAt: '2026-06-23T10:00:00Z',
            },
          ],
          totalElements: 1,
          truncated: false,
        }
      }
      if (options.status === 'DRAFT') {
        return {
          content: [
            {
              id: 'master-draft',
              groupCode: 'RETAIL',
              name: 'Draft letterhead',
              status: 'DRAFT',
              originalFilename: 'draft.docx',
              anchorCount: 1,
              updatedBy: '10000001',
              updatedAt: '2026-06-23T10:00:00Z',
            },
          ],
          totalElements: 1,
          truncated: false,
        }
      }
      return { content: [], totalElements: 0, truncated: false }
    })

    const store = useMastersStore()
    await store.fetchDashboardWorkflowMasters({
      includePendingReview: true,
      includeDraftOrRejected: true,
    })

    expect(mastersApi.listAllMasters).toHaveBeenCalledWith({ status: 'PENDING_REVIEW' })
    expect(mastersApi.listAllMasters).toHaveBeenCalledWith({ status: 'DRAFT' })
    expect(mastersApi.listAllMasters).toHaveBeenCalledWith({ status: 'REJECTED' })
    expect(store.masters.map((master) => master.id).sort()).toEqual([
      'master-draft',
      'master-pending',
    ])
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

  it('records list retryable flag on load failure', async () => {
    vi.mocked(mastersApi.listMasters).mockRejectedValue(
      axiosEnvelopeError(503, 'api.error.generation.serviceUnavailable', {
        retryable: true,
      }),
    )
    const store = useMastersStore()

    await expect(store.fetchMasters()).rejects.toBeTruthy()
    expect(store.lastListErrorRetryable).toBe(true)
  })

  it('loads revision lines page into store state', async () => {
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'revision-1',
          lineLabel: 'CURRENT',
          status: 'APPROVED',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000001',
          current: true,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const store = useMastersStore()
    const page = await store.fetchRevisionLines('master-1', 0, 20)

    expect(page.content).toHaveLength(1)
    expect(store.revisionLinesPage?.content[0]?.id).toBe('revision-1')
  })

  it('maps nginx HTML 413 on replace to readable upload size key', async () => {
    vi.mocked(mastersApi.replaceMasterFile).mockRejectedValue(
      new AxiosError('Request failed', '413', undefined, undefined, {
        status: 413,
        statusText: 'Payload Too Large',
        headers: { 'content-type': 'text/html' },
        config: { headers: new AxiosHeaders() },
        data: '<html><body>413 Request Entity Too Large</body></html>',
      }),
    )
    const store = useMastersStore()
    const file = new File([new Uint8Array(8)], 'huge.docx')

    await expect(store.replaceMasterFile('master-1', file)).rejects.toBeTruthy()
    expect(store.lastErrorMessageKey).toBe('masters.upload.errorTooLarge')
  })

  it('records Spring envelope docxTooLarge on upload failure', async () => {
    vi.mocked(mastersApi.createMaster).mockRejectedValue(
      axiosEnvelopeError(413, 'api.error.master.docxTooLarge', {
        code: 'MASTER_VALIDATION_FAILED',
        category: 'VALIDATION',
        message: 'The uploaded DOCX exceeds the maximum allowed size.',
      }),
    )
    const store = useMastersStore()
    const file = new File([new Uint8Array(8)], 'letterhead.docx')

    await expect(
      store.uploadMaster({ groupCode: 'RETAIL', name: 'Retail letterhead' }, file),
    ).rejects.toBeTruthy()
    expect(store.lastErrorMessageKey).toBe('api.error.master.docxTooLarge')
  })

  it('loads revision line detail into store state', async () => {
    vi.mocked(mastersApi.getMasterRevisionLine).mockResolvedValue({
      id: 'revision-1',
      masterId: 'master-1',
      lineLabel: 'CURRENT',
      status: 'APPROVED',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      current: true,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })

    const store = useMastersStore()
    const detail = await store.fetchRevisionLine('master-1', 'revision-1')

    expect(detail.id).toBe('revision-1')
    expect(store.selectedRevisionLine?.current).toBe(true)
  })

  it('CE-U06 — updates displayLabel on selected revision line and live master', async () => {
    vi.mocked(mastersApi.updateMasterRevisionLineAnchorDisplayLabel).mockResolvedValue({
      anchorId: 'HEADER',
      displayLabel: 'Readable header',
      documentSequence: 0,
    })

    const store = useMastersStore()
    store.selectedMaster = { ...sampleDetail }
    store.selectedRevisionLine = {
      id: 'revision-1',
      masterId: 'master-1',
      lineLabel: 'CURRENT',
      status: 'DRAFT',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      current: true,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block', documentSequence: 0 }],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    }

    await store.updateRevisionLineAnchorDisplayLabel('master-1', 'revision-1', 'HEADER', {
      displayLabel: 'Readable header',
    })

    expect(mastersApi.updateMasterRevisionLineAnchorDisplayLabel).toHaveBeenCalledWith(
      'master-1',
      'revision-1',
      'HEADER',
      { displayLabel: 'Readable header' },
    )
    expect(store.selectedRevisionLine?.anchors[0]?.displayLabel).toBe('Readable header')
    expect(store.selectedMaster?.anchors[0]?.displayLabel).toBe('Readable header')
  })
})
