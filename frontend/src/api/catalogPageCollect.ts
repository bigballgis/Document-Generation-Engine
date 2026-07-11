import type { PageView } from '@/types/identity'

/** Catalog list API max page size (LR-C5 / CatalogPageSupport.MAX_SIZE). */
export const CATALOG_COLLECT_PAGE_SIZE = 100

/**
 * Safety valve for multi-page consumers (dashboard / authoring pickers).
 * 50 × 100 = 5000 rows — enough for current catalog seeds without unbounded loops.
 */
export const CATALOG_COLLECT_MAX_PAGES = 50

export type CollectedCatalogPage<T> = {
  content: T[]
  totalElements: number
  truncated: boolean
}

/**
 * Walks PageView pages until exhausted (or {@link CATALOG_COLLECT_MAX_PAGES}).
 * Prefer this over a single size=100 call so pickers/dashboard do not silently truncate.
 */
export async function collectAllPageContent<T>(
  fetchPage: (page: number, size: number) => Promise<PageView<T>>,
  options: { pageSize?: number; maxPages?: number } = {},
): Promise<CollectedCatalogPage<T>> {
  const pageSize = options.pageSize ?? CATALOG_COLLECT_PAGE_SIZE
  const maxPages = options.maxPages ?? CATALOG_COLLECT_MAX_PAGES
  const content: T[] = []
  let totalElements = 0

  for (let page = 0; page < maxPages; page += 1) {
    const pageView = await fetchPage(page, pageSize)
    totalElements = pageView.totalElements
    content.push(...pageView.content)

    const reachedEnd =
      pageView.totalPages <= 0 ||
      page + 1 >= pageView.totalPages ||
      pageView.content.length === 0

    if (reachedEnd) {
      return { content, totalElements, truncated: false }
    }
  }

  return {
    content,
    totalElements,
    truncated: content.length < totalElements,
  }
}
