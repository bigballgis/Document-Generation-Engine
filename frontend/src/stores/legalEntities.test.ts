import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as legalEntitiesApi from '@/api/legalEntities'
import { useLegalEntitiesStore } from '@/stores/legalEntities'

vi.mock('@/api/legalEntities', () => ({
  listLegalEntities: vi.fn(),
  createLegalEntity: vi.fn(),
  updateLegalEntity: vi.fn(),
  getGroupDefaultLegalEntity: vi.fn(),
  putGroupDefaultLegalEntity: vi.fn(),
}))

describe('legalEntities store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(legalEntitiesApi.listLegalEntities).mockReset()
    vi.mocked(legalEntitiesApi.getGroupDefaultLegalEntity).mockReset()
    vi.mocked(legalEntitiesApi.putGroupDefaultLegalEntity).mockReset()
  })

  it('loads entities and group default', async () => {
    vi.mocked(legalEntitiesApi.listLegalEntities).mockResolvedValue({
      content: [
        {
          groupCode: 'RETAIL',
          legalEntityCode: 'LE-HK-001',
          displayName: 'HK Entity',
          status: 'ACTIVE',
          documentBrandCode: 'HK-RETAIL-LETTER',
        },
      ],
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
    })
    vi.mocked(legalEntitiesApi.getGroupDefaultLegalEntity).mockResolvedValue({
      groupCode: 'RETAIL',
      defaultLegalEntityCode: 'LE-HK-001',
    })

    const store = useLegalEntitiesStore()
    await store.fetchEntities('RETAIL')
    await store.fetchDefault('RETAIL')

    expect(store.entities[0]?.legalEntityCode).toBe('LE-HK-001')
    expect(store.defaultLegalEntityCode).toBe('LE-HK-001')
  })

  it('saves group default legal entity', async () => {
    vi.mocked(legalEntitiesApi.putGroupDefaultLegalEntity).mockResolvedValue({
      groupCode: 'RETAIL',
      defaultLegalEntityCode: 'LE-HK-001',
    })

    const store = useLegalEntitiesStore()
    await store.putDefault('RETAIL', { defaultLegalEntityCode: 'LE-HK-001' })

    expect(legalEntitiesApi.putGroupDefaultLegalEntity).toHaveBeenCalledWith('RETAIL', {
      defaultLegalEntityCode: 'LE-HK-001',
    })
    expect(store.defaultLegalEntityCode).toBe('LE-HK-001')
  })
})
