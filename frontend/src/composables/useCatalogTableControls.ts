import { computed, ref, type ComputedRef, type Ref } from 'vue'
import { compareSortValues, useDataTableFilters } from '@/composables/useDataTableFilters'
import type { TableColumnFilterOption } from '@/composables/useTableFilterOptions'

export interface CatalogFilterField<T> {
  key: string
  labelKey: string
  getValue: (row: T) => string
  matchMode?: 'contains' | 'exact'
}

export interface CatalogSortOption<T> {
  key: string
  labelKey: string
  getter: (row: T) => string | number | boolean | null | undefined
  order: 'asc' | 'desc'
}

export interface CatalogFilterChip {
  key: string
  labelKey: string
  value: string
}

export interface CatalogTableControlsConfig<T> {
  searchGetters: Array<(row: T) => string>
  filters: CatalogFilterField<T>[]
  sortOptions: CatalogSortOption<T>[]
  defaultSortKey: string
}

export function useCatalogTableControls<T>(
  source: Ref<T[]> | ComputedRef<T[]>,
  config: CatalogTableControlsConfig<T>,
) {
  const searchQuery = ref('')
  const activeSortKey = ref(config.defaultSortKey)

  const searchFilteredSource = computed(() => {
    const needle = searchQuery.value.trim().toLowerCase()
    if (!needle) {
      return source.value
    }
    return source.value.filter((row) =>
      config.searchGetters.some((getter) => getter(row).toLowerCase().includes(needle)),
    )
  })

  const { filters, filteredRows, hasActiveFilters, clearFilters } = useDataTableFilters(
    searchFilteredSource,
    config.filters.map((field) => ({
      key: field.key,
      getValue: field.getValue,
      matchMode: field.matchMode,
    })),
  )

  const activeSortOption = computed(
    () =>
      config.sortOptions.find((option) => option.key === activeSortKey.value) ??
      config.sortOptions[0],
  )

  const sortedRows = computed(() => {
    const sortOption = activeSortOption.value
    if (!sortOption) {
      return filteredRows.value
    }
    return [...filteredRows.value].sort((left, right) => {
      const comparison = compareSortValues(sortOption.getter(left), sortOption.getter(right))
      return sortOption.order === 'desc' ? -comparison : comparison
    })
  })

  const hasAnyActive = computed(
    () => hasActiveFilters.value || searchQuery.value.trim().length > 0,
  )

  const activeFilterChips = computed((): CatalogFilterChip[] => {
    const chips: CatalogFilterChip[] = []
    if (searchQuery.value.trim()) {
      chips.push({
        key: '__search__',
        labelKey: 'table.activeFilter.search',
        value: searchQuery.value.trim(),
      })
    }
    for (const field of config.filters) {
      const value = filters[field.key]?.trim()
      if (value) {
        chips.push({
          key: field.key,
          labelKey: field.labelKey,
          value,
        })
      }
    }
    return chips
  })

  function clearAll() {
    searchQuery.value = ''
    clearFilters()
    activeSortKey.value = config.defaultSortKey
  }

  function removeFilterChip(key: string) {
    if (key === '__search__') {
      searchQuery.value = ''
      return
    }
    filters[key] = ''
  }

  return {
    searchQuery,
    filters,
    activeSortKey,
    sortedRows,
    hasActiveFilters,
    hasAnyActive,
    activeFilterChips,
    clearFilters,
    clearAll,
    removeFilterChip,
  }
}

export type { TableColumnFilterOption }
