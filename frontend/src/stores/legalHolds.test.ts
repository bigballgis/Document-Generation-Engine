import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as legalHoldsApi from '@/api/legalHolds'
import { useLegalHoldsStore } from '@/stores/legalHolds'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/legalHolds', () => ({
  listLegalHolds: vi.fn(),
  createLegalHold: vi.fn(),
  releaseLegalHold: vi.fn(),
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
  effectiveTo: null,
  invocationExternalIds: [] as string[],
  invocationCount: 0,
  createdAt: '2026-07-16T10:00:00Z',
  createdByUsername: '10000001',
  releasedAt: null,
  releasedByUsername: null,
}

describe('legalHolds store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(legalHoldsApi.listLegalHolds).mockReset()
    vi.mocked(legalHoldsApi.createLegalHold).mockReset()
    vi.mocked(legalHoldsApi.releaseLegalHold).mockReset()
  })

  it('loads a page of holds into state', async () => {
    vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue({
      content: [sampleHold],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const store = useLegalHoldsStore()
    await store.fetchHolds(0, 20, { status: 'ACTIVE' })

    expect(legalHoldsApi.listLegalHolds).toHaveBeenCalledWith(0, 20, {
      status: 'ACTIVE',
      signal: undefined,
    })
    expect(store.holds).toHaveLength(1)
    expect(store.listTotalElements).toBe(1)
    expect(store.statusFilter).toBe('ACTIVE')
    expect(store.lastErrorMessageKey).toBeNull()
  })

  it('records a list error message key on failure', async () => {
    vi.mocked(legalHoldsApi.listLegalHolds).mockRejectedValue(
      axiosEnvelopeError(503, 'api.error.generation.serviceUnavailable', {
        retryable: true,
      }),
    )
    const store = useLegalHoldsStore()
    await expect(store.fetchHolds()).rejects.toBeTruthy()
    expect(store.lastListErrorRetryable).toBe(true)
    expect(store.lastErrorMessageKey).toBeTruthy()
  })

  it('creates and releases holds through the API module', async () => {
    vi.mocked(legalHoldsApi.createLegalHold).mockResolvedValue(sampleHold)
    vi.mocked(legalHoldsApi.releaseLegalHold).mockResolvedValue({
      ...sampleHold,
      status: 'RELEASED',
      releasedAt: '2026-07-16T12:00:00Z',
      releasedByUsername: '10000001',
    })

    const store = useLegalHoldsStore()
    const created = await store.createHold({
      scopeType: 'TEMPLATE_WINDOW',
      templateId: 'tpl-1',
      effectiveFrom: '2026-01-01T00:00:00Z',
    })
    expect(created.status).toBe('ACTIVE')

    const released = await store.releaseHold('hold-1')
    expect(legalHoldsApi.releaseLegalHold).toHaveBeenCalledWith('hold-1')
    expect(released.status).toBe('RELEASED')
  })
})
