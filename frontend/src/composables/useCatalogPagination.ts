import { computed, watch, type ComputedRef, type Ref } from 'vue'

export function useCatalogPagination<T>(
  filteredRows: Ref<T[]> | ComputedRef<T[]>,
  currentPage: Ref<number>,
  pageSize: number,
) {
  const totalRows = computed(() => filteredRows.value.length)

  const paginatedRows = computed(() => {
    const start = (currentPage.value - 1) * pageSize
    return filteredRows.value.slice(start, start + pageSize)
  })

  watch(
    totalRows,
    (newTotal, oldTotal) => {
      if (oldTotal !== undefined && newTotal !== oldTotal) {
        currentPage.value = 1
      }
    },
    { flush: 'sync' },
  )

  return { paginatedRows, totalRows }
}
