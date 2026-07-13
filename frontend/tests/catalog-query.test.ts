import { describe, expect, it, vi } from 'vitest'
import {
  buildCatalogQuery,
  catalogTotalPages,
  collectCatalogPages,
  findInCatalogPages,
  unwrapCatalogPage,
} from '../e2e/helpers/catalog-query'

describe('catalog-query E2E helpers', () => {
  it('unwrapCatalogPage accepts PageView and legacy arrays', () => {
    expect(unwrapCatalogPage([{ id: 'a' }])).toEqual([{ id: 'a' }])
    expect(unwrapCatalogPage({ content: [{ id: 'b' }], totalPages: 1 })).toEqual([{ id: 'b' }])
  })

  it('buildCatalogQuery omits empty values', () => {
    expect(buildCatalogQuery({ search: 'DEMO', page: 0, size: 100, groupCode: undefined })).toBe(
      '?search=DEMO&page=0&size=100',
    )
  })

  it('collectCatalogPages walks until totalPages exhausted', async () => {
    type Row = { id: string }
    const fetchPage = vi
      .fn<(page: number, size: number) => Promise<{ content: Row[]; totalPages: number; totalElements: number }>>()
      .mockResolvedValueOnce({
        content: [{ id: '1' }, { id: '2' }],
        totalPages: 2,
        totalElements: 3,
      })
      .mockResolvedValueOnce({
        content: [{ id: '3' }],
        totalPages: 2,
        totalElements: 3,
      })

    const rows = await collectCatalogPages(fetchPage, { pageSize: 2 })
    expect(rows.map((row: Row) => row.id)).toEqual(['1', '2', '3'])
    expect(fetchPage).toHaveBeenCalledTimes(2)
  })

  it('findInCatalogPages stops when predicate matches on later page', async () => {
    type Row = { externalId: string }
    const fetchPage = vi
      .fn<(page: number, size: number) => Promise<{ content: Row[]; totalPages: number }>>()
      .mockResolvedValueOnce({
        content: [{ externalId: 'LOAD-TPL-0001' }],
        totalPages: 3,
      })
      .mockResolvedValueOnce({
        content: [{ externalId: 'DEMO-RETAIL-LETTER' }],
        totalPages: 3,
      })

    const match = await findInCatalogPages(
      fetchPage,
      (row: Row) => row.externalId === 'DEMO-RETAIL-LETTER',
      { pageSize: 1 },
    )
    expect(match?.externalId).toBe('DEMO-RETAIL-LETTER')
    expect(fetchPage).toHaveBeenCalledTimes(2)
  })

  it('catalogTotalPages treats legacy arrays as a single page', () => {
    expect(catalogTotalPages([{ id: 'x' }])).toBe(1)
    expect(catalogTotalPages({ content: [], totalPages: 0 })).toBe(0)
  })
})
