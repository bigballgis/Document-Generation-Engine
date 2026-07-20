import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as documentBrandsApi from '@/api/documentBrands'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

const sampleBrand = {
  groupCode: 'RETAIL',
  documentBrandCode: 'HK-RETAIL-LETTER',
  displayName: 'HK Retail Letter',
  status: 'ACTIVE' as const,
  logoObjectRef: 'platform/document-brands/HK-RETAIL-LETTER/logo',
  defaultSealObjectRef: null,
  letterheadLegalName: 'Retail Bank HK',
}

describe('documentBrands API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
    vi.mocked(http.put).mockReset()
  })

  it('lists document brands with groupCode and optional status', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [sampleBrand],
          page: 0,
          size: 1,
          totalElements: 1,
          totalPages: 1,
        },
      },
    })

    const pageView = await documentBrandsApi.listDocumentBrands('RETAIL', { status: 'ACTIVE' })

    expect(http.get).toHaveBeenCalledWith('/document-brands', {
      params: { groupCode: 'RETAIL', status: 'ACTIVE' },
      signal: undefined,
    })
    expect(pageView.content[0]?.documentBrandCode).toBe('HK-RETAIL-LETTER')
  })

  it('creates a document brand', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: { metadata: {}, result: sampleBrand },
    })

    const result = await documentBrandsApi.createDocumentBrand({
      groupCode: 'RETAIL',
      documentBrandCode: 'HK-RETAIL-LETTER',
      displayName: 'HK Retail Letter',
      logoObjectRef: 'platform/document-brands/HK-RETAIL-LETTER/logo',
      status: 'ACTIVE',
    })

    expect(http.post).toHaveBeenCalledWith('/document-brands', {
      groupCode: 'RETAIL',
      documentBrandCode: 'HK-RETAIL-LETTER',
      displayName: 'HK Retail Letter',
      logoObjectRef: 'platform/document-brands/HK-RETAIL-LETTER/logo',
      status: 'ACTIVE',
    })
    expect(result.status).toBe('ACTIVE')
  })

  it('updates a document brand by code', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: { metadata: {}, result: { ...sampleBrand, status: 'INACTIVE' } },
    })

    const result = await documentBrandsApi.updateDocumentBrand('HK-RETAIL-LETTER', {
      groupCode: 'RETAIL',
      status: 'INACTIVE',
    })

    expect(http.put).toHaveBeenCalledWith('/document-brands/HK-RETAIL-LETTER', {
      groupCode: 'RETAIL',
      status: 'INACTIVE',
    })
    expect(result.status).toBe('INACTIVE')
  })
})
