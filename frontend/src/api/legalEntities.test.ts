import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as legalEntitiesApi from '@/api/legalEntities'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

const sampleEntity = {
  groupCode: 'RETAIL',
  legalEntityCode: 'LE-HK-001',
  displayName: 'Hong Kong Retail Entity',
  status: 'ACTIVE' as const,
  documentBrandCode: 'HK-RETAIL-LETTER',
}

describe('legalEntities API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
    vi.mocked(http.put).mockReset()
  })

  it('lists legal entities with groupCode and optional status', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [sampleEntity],
          page: 0,
          size: 1,
          totalElements: 1,
          totalPages: 1,
        },
      },
    })

    const pageView = await legalEntitiesApi.listLegalEntities('RETAIL', { status: 'ACTIVE' })

    expect(http.get).toHaveBeenCalledWith('/legal-entities', {
      params: { groupCode: 'RETAIL', status: 'ACTIVE' },
      signal: undefined,
    })
    expect(pageView.content[0]?.legalEntityCode).toBe('LE-HK-001')
  })

  it('creates a legal entity with required document brand binding', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: { metadata: {}, result: sampleEntity },
    })

    const result = await legalEntitiesApi.createLegalEntity({
      groupCode: 'RETAIL',
      legalEntityCode: 'LE-HK-001',
      displayName: 'Hong Kong Retail Entity',
      documentBrandCode: 'HK-RETAIL-LETTER',
      status: 'ACTIVE',
    })

    expect(http.post).toHaveBeenCalledWith('/legal-entities', {
      groupCode: 'RETAIL',
      legalEntityCode: 'LE-HK-001',
      displayName: 'Hong Kong Retail Entity',
      documentBrandCode: 'HK-RETAIL-LETTER',
      status: 'ACTIVE',
    })
    expect(result.documentBrandCode).toBe('HK-RETAIL-LETTER')
  })

  it('reads and writes group default legal entity', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: { groupCode: 'RETAIL', defaultLegalEntityCode: 'LE-HK-001' },
      },
    })
    vi.mocked(http.put).mockResolvedValue({
      data: {
        metadata: {},
        result: { groupCode: 'RETAIL', defaultLegalEntityCode: null },
      },
    })

    const current = await legalEntitiesApi.getGroupDefaultLegalEntity('RETAIL')
    expect(http.get).toHaveBeenCalledWith('/groups/RETAIL/default-legal-entity', {
      signal: undefined,
    })
    expect(current.defaultLegalEntityCode).toBe('LE-HK-001')

    const cleared = await legalEntitiesApi.putGroupDefaultLegalEntity('RETAIL', {
      defaultLegalEntityCode: null,
    })
    expect(http.put).toHaveBeenCalledWith('/groups/RETAIL/default-legal-entity', {
      defaultLegalEntityCode: null,
    })
    expect(cleared.defaultLegalEntityCode).toBeNull()
  })
})
