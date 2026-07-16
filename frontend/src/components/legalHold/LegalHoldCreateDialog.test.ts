import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useLegalHoldsStore } from '@/stores/legalHolds'
import { parseLegalHoldInvocationIds } from '@/utils/legalHoldInvocationIds'

describe('LegalHoldCreateDialog payload assembly', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('builds TEMPLATE_WINDOW create payload', async () => {
    const store = useLegalHoldsStore()
    const createHold = vi.spyOn(store, 'createHold').mockResolvedValue({
      id: 'hold-1',
      holdExternalId: 'LH-001',
      scopeType: 'TEMPLATE_WINDOW',
      status: 'ACTIVE',
      reason: 'Litigation freeze',
      templateId: 'tpl-1',
      templateExternalId: 'TPL-001',
      effectiveFrom: '2026-01-01T00:00:00Z',
      effectiveTo: null,
      invocationExternalIds: [],
      invocationCount: 0,
      createdAt: '2026-07-16T10:00:00Z',
      createdByUsername: '10000001',
      releasedAt: null,
      releasedByUsername: null,
    })

    await store.createHold({
      scopeType: 'TEMPLATE_WINDOW',
      reason: 'Litigation freeze',
      templateId: 'tpl-1',
      effectiveFrom: '2026-01-01T00:00:00Z',
      effectiveTo: null,
    })

    expect(createHold).toHaveBeenCalledWith({
      scopeType: 'TEMPLATE_WINDOW',
      reason: 'Litigation freeze',
      templateId: 'tpl-1',
      effectiveFrom: '2026-01-01T00:00:00Z',
      effectiveTo: null,
    })
  })

  it('parses multiline invocation IDs for INVOCATION_SET', async () => {
    const store = useLegalHoldsStore()
    const createHold = vi.spyOn(store, 'createHold').mockResolvedValue({
      id: 'hold-2',
      holdExternalId: 'LH-002',
      scopeType: 'INVOCATION_SET',
      status: 'ACTIVE',
      reason: null,
      templateId: null,
      templateExternalId: null,
      effectiveFrom: null,
      effectiveTo: null,
      invocationExternalIds: ['INV-1', 'INV-2', 'INV-3'],
      invocationCount: 3,
      createdAt: '2026-07-16T10:00:00Z',
      createdByUsername: '10000001',
      releasedAt: null,
      releasedByUsername: null,
    })

    const ids = parseLegalHoldInvocationIds('INV-1\nINV-2, INV-3 ')
    await store.createHold({
      scopeType: 'INVOCATION_SET',
      reason: null,
      invocationExternalIds: ids,
    })

    expect(ids).toEqual(['INV-1', 'INV-2', 'INV-3'])
    expect(createHold).toHaveBeenCalledWith({
      scopeType: 'INVOCATION_SET',
      reason: null,
      invocationExternalIds: ['INV-1', 'INV-2', 'INV-3'],
    })
  })
})
