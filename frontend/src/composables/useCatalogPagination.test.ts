import { computed, ref } from 'vue'
import { describe, expect, it } from 'vitest'
import { useCatalogPagination } from '@/composables/useCatalogPagination'

describe('useCatalogPagination', () => {
  it('slices filtered rows by page', () => {
    const filteredRows = computed(() => ['a', 'b', 'c', 'd', 'e'])
    const currentPage = ref(2)
    const { paginatedRows, totalRows } = useCatalogPagination(filteredRows, currentPage, 2)

    expect(totalRows.value).toBe(5)
    expect(paginatedRows.value).toEqual(['c', 'd'])
  })

  it('returns empty slice when page is out of range', () => {
    const filteredRows = computed(() => ['a', 'b'])
    const currentPage = ref(3)
    const { paginatedRows, totalRows } = useCatalogPagination(filteredRows, currentPage, 2)

    expect(totalRows.value).toBe(2)
    expect(paginatedRows.value).toEqual([])
  })

  it('resets current page when filtered row count changes', () => {
    const source = ref(['a', 'b', 'c', 'd', 'e'])
    const filteredRows = computed(() => source.value)
    const currentPage = ref(2)
    useCatalogPagination(filteredRows, currentPage, 2)

    source.value = ['a', 'b']
    expect(currentPage.value).toBe(1)
  })
})
