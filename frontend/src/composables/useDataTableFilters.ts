import { computed, reactive, type ComputedRef, type Ref } from 'vue'

export interface DataTableColumnFilter<T> {
  key: string
  getValue: (row: T) => string
  matchMode?: 'contains' | 'exact'
}

export function useDataTableFilters<T>(
  source: Ref<T[]> | ComputedRef<T[]>,
  columns: DataTableColumnFilter<T>[],
) {
  const filters = reactive<Record<string, string>>(
    Object.fromEntries(columns.map((column) => [column.key, ''])),
  )

  const filteredRows = computed(() => {
    const activeFilters = columns.filter((column) => filters[column.key]?.trim())
    if (activeFilters.length === 0) {
      return source.value
    }

    return source.value.filter((row) => {
      for (const column of activeFilters) {
        const needle = filters[column.key].trim().toLowerCase()
        const haystack = column.getValue(row).toLowerCase()
        if (column.matchMode === 'exact') {
          if (haystack !== needle) {
            return false
          }
        } else if (!haystack.includes(needle)) {
          return false
        }
      }
      return true
    })
  })

  const hasActiveFilters = computed(() =>
    columns.some((column) => filters[column.key]?.trim().length > 0),
  )

  function clearFilters() {
    for (const column of columns) {
      filters[column.key] = ''
    }
  }

  return {
    filters,
    filteredRows,
    hasActiveFilters,
    clearFilters,
  }
}

export function compareSortValues(
  left: string | number | boolean | null | undefined,
  right: string | number | boolean | null | undefined,
): number {
  if (typeof left === 'number' && typeof right === 'number') {
    return left - right
  }
  return String(left ?? '').localeCompare(String(right ?? ''), undefined, { sensitivity: 'base' })
}

export function rowSortMethod<T>(
  getter: (row: T) => string | number | boolean | null | undefined,
): (left: T, right: T) => number {
  return (left, right) => compareSortValues(getter(left), getter(right))
}
