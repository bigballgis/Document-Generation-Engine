import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as mastersApi from '@/api/masters'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('masters API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
  })

  it('lists masters as PageView with page/size defaults', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [
            {
              id: 'master-1',
              groupCode: 'RETAIL',
              name: 'Retail letterhead',
              status: 'DRAFT',
              originalFilename: 'letterhead.docx',
              anchorCount: 2,
              updatedAt: '2026-06-23T10:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      },
    })

    const pageView = await mastersApi.listMasters()

    expect(http.get).toHaveBeenCalledWith('/masters', {
      params: { page: 0, size: 20 },
      signal: undefined,
    })
    expect(pageView.content).toHaveLength(1)
    expect(pageView.content[0]?.name).toBe('Retail letterhead')
    expect(pageView.totalElements).toBe(1)
  })

  it('forwards search/groupCode/status/sort query params', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [],
          page: 1,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        },
      },
    })

    await mastersApi.listMasters(1, 20, {
      search: 'letter',
      groupCode: 'retail',
      status: 'DRAFT',
      sort: 'groupCodeAsc',
    })

    expect(http.get).toHaveBeenCalledWith('/masters', {
      params: {
        page: 1,
        size: 20,
        search: 'letter',
        groupCode: 'RETAIL',
        status: 'DRAFT',
        sort: 'groupCodeAsc',
      },
      signal: undefined,
    })
  })

  it('listAllMasters merges pages via collectAllPageContent', async () => {
    vi.mocked(http.get)
      .mockResolvedValueOnce({
        data: {
          metadata: {},
          result: {
            content: [
              {
                id: 'master-1',
                groupCode: 'RETAIL',
                name: 'One',
                status: 'DRAFT',
                originalFilename: 'a.docx',
                anchorCount: 1,
                updatedAt: '2026-06-23T10:00:00Z',
              },
            ],
            page: 0,
            size: 100,
            totalElements: 2,
            totalPages: 2,
          },
        },
      })
      .mockResolvedValueOnce({
        data: {
          metadata: {},
          result: {
            content: [
              {
                id: 'master-2',
                groupCode: 'CORPORATE',
                name: 'Two',
                status: 'APPROVED',
                originalFilename: 'b.docx',
                anchorCount: 2,
                updatedAt: '2026-06-23T11:00:00Z',
              },
            ],
            page: 1,
            size: 100,
            totalElements: 2,
            totalPages: 2,
          },
        },
      })

    const collected = await mastersApi.listAllMasters({ sort: 'groupCodeAsc' })

    expect(http.get).toHaveBeenCalledTimes(2)
    expect(collected.content).toHaveLength(2)
    expect(collected.totalElements).toBe(2)
    expect(collected.truncated).toBe(false)
  })

  it('creates a master with multipart form data', async () => {
    const file = new File(['docx'], 'letterhead.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })

    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          id: 'master-1',
          groupCode: 'RETAIL',
          name: 'Retail letterhead',
          description: null,
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          changeSummary: null,
          anchors: [],
          reviewHistory: [],
          createdAt: '2026-06-23T10:00:00Z',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      },
    })

    const created = await mastersApi.createMaster(
      { groupCode: 'RETAIL', name: 'Retail letterhead' },
      file,
    )

    expect(http.post).toHaveBeenCalledWith(
      '/masters',
      expect.any(FormData),
      expect.objectContaining({ headers: { 'Content-Type': 'multipart/form-data' } }),
    )
    expect(created.id).toBe('master-1')
  })

  it('forwards multipart upload progress callbacks', async () => {
    const file = new File(['docx'], 'letterhead.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    const onUploadProgress = vi.fn()

    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          id: 'master-1',
          groupCode: 'RETAIL',
          name: 'Retail letterhead',
          description: null,
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          changeSummary: null,
          anchors: [],
          reviewHistory: [],
          createdAt: '2026-06-23T10:00:00Z',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      },
    })

    await mastersApi.createMaster({ groupCode: 'RETAIL', name: 'Retail letterhead' }, file, {
      onUploadProgress,
    })

    expect(http.post).toHaveBeenCalledWith(
      '/masters',
      expect.any(FormData),
      expect.objectContaining({
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: expect.any(Function),
      }),
    )

    const config = vi.mocked(http.post).mock.calls[0]?.[2] as {
      onUploadProgress?: (event: { loaded: number; total?: number }) => void
    }
    config.onUploadProgress?.({ loaded: 50, total: 100 })
    expect(onUploadProgress).toHaveBeenCalledWith(50)
  })

  it('lists revision lines with pagination params', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
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
        },
      },
    })

    const page = await mastersApi.listMasterRevisionLines('master-1', 0, 20)

    expect(http.get).toHaveBeenCalledWith('/masters/master-1/revision-lines', {
      params: { page: 0, size: 20 },
    })
    expect(page.content).toHaveLength(1)
    expect(page.content[0]?.current).toBe(true)
  })
})
