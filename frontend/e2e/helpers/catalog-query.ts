/**
 * E2E catalog list helpers for LR-C5 PageView responses.
 * Prefer server-side search/filter query params; paginate when a full filtered set is needed.
 */

export const E2E_CATALOG_PAGE_SIZE = 100
/** 50 × 100 = 5000 — covers catalog-load seed without unbounded loops. */
export const E2E_CATALOG_MAX_PAGES = 50

export type CatalogPageView<T> = {
  content: T[]
  page?: number
  size?: number
  totalElements?: number
  totalPages?: number
}

export function unwrapCatalogPage<T>(page: CatalogPageView<T> | T[]): T[] {
  return Array.isArray(page) ? page : (page.content ?? [])
}

export function catalogTotalPages<T>(page: CatalogPageView<T> | T[]): number {
  if (Array.isArray(page)) {
    return 1
  }
  if (typeof page.totalPages === 'number' && page.totalPages > 0) {
    return page.totalPages
  }
  return page.content?.length ? 1 : 0
}

/**
 * Walks pages until exhausted (or maxPages). Use after applying server-side filters
 * so callers do not silently truncate under DOCGEN_SEED_CATALOG_LOAD.
 */
export async function collectCatalogPages<T>(
  fetchPage: (page: number, size: number) => Promise<CatalogPageView<T> | T[]>,
  options: { pageSize?: number; maxPages?: number } = {},
): Promise<T[]> {
  const pageSize = options.pageSize ?? E2E_CATALOG_PAGE_SIZE
  const maxPages = options.maxPages ?? E2E_CATALOG_MAX_PAGES
  const content: T[] = []

  for (let page = 0; page < maxPages; page += 1) {
    const pageView = await fetchPage(page, pageSize)
    const rows = unwrapCatalogPage(pageView)
    content.push(...rows)

    const totalPages = catalogTotalPages(pageView)
    const reachedEnd = totalPages <= 0 || page + 1 >= totalPages || rows.length === 0
    if (reachedEnd) {
      return content
    }
  }

  return content
}

export async function findInCatalogPages<T>(
  fetchPage: (page: number, size: number) => Promise<CatalogPageView<T> | T[]>,
  predicate: (item: T) => boolean,
  options: { pageSize?: number; maxPages?: number } = {},
): Promise<T | undefined> {
  const pageSize = options.pageSize ?? E2E_CATALOG_PAGE_SIZE
  const maxPages = options.maxPages ?? E2E_CATALOG_MAX_PAGES

  for (let page = 0; page < maxPages; page += 1) {
    const pageView = await fetchPage(page, pageSize)
    const rows = unwrapCatalogPage(pageView)
    const match = rows.find(predicate)
    if (match) {
      return match
    }

    const totalPages = catalogTotalPages(pageView)
    const reachedEnd = totalPages <= 0 || page + 1 >= totalPages || rows.length === 0
    if (reachedEnd) {
      return undefined
    }
  }

  return undefined
}

export function buildCatalogQuery(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') {
      continue
    }
    search.set(key, String(value))
  }
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}
