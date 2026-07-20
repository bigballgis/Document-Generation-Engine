import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as documentBrandsApi from '@/api/documentBrands'
import { useDocumentBrandsStore } from '@/stores/documentBrands'

vi.mock('@/api/documentBrands', () => ({
  listDocumentBrands: vi.fn(),
  createDocumentBrand: vi.fn(),
  updateDocumentBrand: vi.fn(),
}))

describe('documentBrands store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(documentBrandsApi.listDocumentBrands).mockReset()
    vi.mocked(documentBrandsApi.createDocumentBrand).mockReset()
  })

  it('loads brands for a group', async () => {
    vi.mocked(documentBrandsApi.listDocumentBrands).mockResolvedValue({
      content: [
        {
          groupCode: 'RETAIL',
          documentBrandCode: 'PLATFORM_DEFAULT',
          displayName: 'Platform default',
          status: 'ACTIVE',
          logoObjectRef: 'platform/document-brands/PLATFORM_DEFAULT/logo',
        },
      ],
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
    })

    const store = useDocumentBrandsStore()
    await store.fetchBrands('RETAIL')

    expect(documentBrandsApi.listDocumentBrands).toHaveBeenCalledWith('RETAIL', {
      status: undefined,
      signal: undefined,
    })
    expect(store.brands).toHaveLength(1)
    expect(store.brands[0]?.documentBrandCode).toBe('PLATFORM_DEFAULT')
  })
})
