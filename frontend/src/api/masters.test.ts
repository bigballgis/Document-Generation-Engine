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

  it('lists masters from the management endpoint', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: [
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
      },
    })

    const masters = await mastersApi.listMasters()

    expect(http.get).toHaveBeenCalledWith('/masters')
    expect(masters).toHaveLength(1)
    expect(masters[0]?.name).toBe('Retail letterhead')
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
})
