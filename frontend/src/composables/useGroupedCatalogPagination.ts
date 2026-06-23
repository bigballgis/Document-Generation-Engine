import { computed, type Ref } from 'vue'

export function useGroupedCatalogPagination<T>(
  filteredRows: Ref<T[]>,
  getGroupCode: (row: T) => string,
  currentPage: Ref<number>,
  pageSize: number,
) {
  const groupedEntries = computed(() => {
    const grouped = new Map<string, T[]>()
    for (const row of filteredRows.value) {
      const groupCode = getGroupCode(row)
      const existing = grouped.get(groupCode) ?? []
      existing.push(row)
      grouped.set(groupCode, existing)
    }
    return [...grouped.entries()]
  })

  const paginatedGroups = computed(() => {
    const start = (currentPage.value - 1) * pageSize
    return groupedEntries.value.slice(start, start + pageSize)
  })

  const totalGroups = computed(() => groupedEntries.value.length)

  return { paginatedGroups, totalGroups }
}
