import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as legalHoldsApi from '@/api/legalHolds'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const sampleHold = {
  id: 'hold-1',
  holdExternalId: 'LH-001',
  scopeType: 'TEMPLATE_WINDOW' as const,
  status: 'ACTIVE' as const,
  reason: 'Litigation freeze',
  templateId: 'tpl-1',
  templateExternalId: 'TPL-001',
  effectiveFrom: '2026-01-01T00:00:00Z',
  effectiveTo: '2026-12-31T23:59:59Z',
  invocationExternalIds: [] as string[],
  invocationCount: 0,
  createdAt: '2026-07-16T10:00:00Z',
  createdByUsername: '10000001',
  releasedAt: null,
  releasedByUsername: null,
}

describe('legalHolds API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
  })

  it('lists holds with page/size and optional status', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [sampleHold],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      },
    })

    const pageView = await legalHoldsApi.listLegalHolds(0, 20, { status: 'ACTIVE' })

    expect(http.get).toHaveBeenCalledWith('/legal-holds', {
      params: { page: 0, size: 20, status: 'ACTIVE' },
      signal: undefined,
    })
    expect(pageView.content).toHaveLength(1)
    expect(pageView.content[0]?.holdExternalId).toBe('LH-001')
  })

  it('creates a legal hold', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: { metadata: {}, result: sampleHold },
    })

    const result = await legalHoldsApi.createLegalHold({
      scopeType: 'TEMPLATE_WINDOW',
      templateId: 'tpl-1',
      effectiveFrom: '2026-01-01T00:00:00Z',
      reason: 'Litigation freeze',
    })

    expect(http.post).toHaveBeenCalledWith('/legal-holds', {
      scopeType: 'TEMPLATE_WINDOW',
      templateId: 'tpl-1',
      effectiveFrom: '2026-01-01T00:00:00Z',
      reason: 'Litigation freeze',
    })
    expect(result.status).toBe('ACTIVE')
  })

  it('releases a legal hold by id', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: { ...sampleHold, status: 'RELEASED', releasedAt: '2026-07-16T12:00:00Z' },
      },
    })

    const result = await legalHoldsApi.releaseLegalHold('hold-1')

    expect(http.post).toHaveBeenCalledWith('/legal-holds/hold-1/release')
    expect(result.status).toBe('RELEASED')
  })
})
