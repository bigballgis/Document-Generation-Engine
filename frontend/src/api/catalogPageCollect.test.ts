import { describe, expect, it, vi } from 'vitest'
import { collectAllPageContent } from '@/api/catalogPageCollect'
import type { PageView } from '@/types/identity'

function page<T>(
  content: T[],
  pageIndex: number,
  size: number,
  totalElements: number,
): PageView<T> {
  return {
    content,
    page: pageIndex,
    size,
    totalElements,
    totalPages: totalElements === 0 ? 0 : Math.ceil(totalElements / size),
  }
}

describe('collectAllPageContent', () => {
  it('merges multiple pages until totalPages is exhausted', async () => {
    const fetchPage = vi
      .fn<(page: number, size: number) => Promise<PageView<{ id: string }>>>()
      .mockResolvedValueOnce(page([{ id: 'a' }, { id: 'b' }], 0, 2, 5))
      .mockResolvedValueOnce(page([{ id: 'c' }, { id: 'd' }], 1, 2, 5))
      .mockResolvedValueOnce(page([{ id: 'e' }], 2, 2, 5))

    const collected = await collectAllPageContent(fetchPage, { pageSize: 2 })

    expect(fetchPage).toHaveBeenCalledTimes(3)
    expect(fetchPage).toHaveBeenNthCalledWith(1, 0, 2)
    expect(fetchPage).toHaveBeenNthCalledWith(2, 1, 2)
    expect(fetchPage).toHaveBeenNthCalledWith(3, 2, 2)
    expect(collected.content.map((item) => item.id)).toEqual(['a', 'b', 'c', 'd', 'e'])
    expect(collected.totalElements).toBe(5)
    expect(collected.truncated).toBe(false)
  })

  it('stops after a single page when totalPages is 1', async () => {
    const fetchPage = vi
      .fn<(page: number, size: number) => Promise<PageView<{ id: string }>>>()
      .mockResolvedValue(page([{ id: 'only' }], 0, 100, 1))

    const collected = await collectAllPageContent(fetchPage)

    expect(fetchPage).toHaveBeenCalledTimes(1)
    expect(collected.content).toHaveLength(1)
    expect(collected.truncated).toBe(false)
  })

  it('marks truncated when maxPages is hit before all rows are collected', async () => {
    const fetchPage = vi
      .fn<(page: number, size: number) => Promise<PageView<{ id: string }>>>()
      .mockResolvedValueOnce(page([{ id: 'p0' }], 0, 1, 3))
      .mockResolvedValueOnce(page([{ id: 'p1' }], 1, 1, 3))

    const collected = await collectAllPageContent(fetchPage, { pageSize: 1, maxPages: 2 })

    expect(fetchPage).toHaveBeenCalledTimes(2)
    expect(collected.content.map((item) => item.id)).toEqual(['p0', 'p1'])
    expect(collected.totalElements).toBe(3)
    expect(collected.truncated).toBe(true)
  })
})
