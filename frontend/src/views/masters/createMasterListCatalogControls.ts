import { computed, type ComputedRef, type Ref } from 'vue'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import type { MasterDocumentSummary } from '@/types/master'

type StatusOption = { label: string; value: string }

export function createMasterListCatalogControls(
  allMasters: ComputedRef<MasterDocumentSummary[]>,
  masterStatusFilterOptions: Ref<StatusOption[]>,
) {
  const {
    searchQuery,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
  } = useCatalogTableControls(allMasters, {
    searchGetters: [(row) => row.name, (row) => row.groupCode],
    filters: [
      {
        key: 'groupCode',
        labelKey: 'masters.list.columns.group',
        getValue: (row) => row.groupCode,
      },
      {
        key: 'status',
        labelKey: 'masters.list.columns.status',
        getValue: (row) => row.status,
        matchMode: 'exact',
      },
    ],
    sortOptions: [
      {
        key: 'groupCodeAsc',
        labelKey: 'table.sort.groupAsc',
        getter: (row) => row.groupCode,
        order: 'asc',
      },
      {
        key: 'updatedAtDesc',
        labelKey: 'table.sort.updatedAtDesc',
        getter: (row) => row.updatedAt,
        order: 'desc',
      },
      {
        key: 'updatedAtAsc',
        labelKey: 'table.sort.updatedAtAsc',
        getter: (row) => row.updatedAt,
        order: 'asc',
      },
      {
        key: 'nameAsc',
        labelKey: 'table.sort.nameAsc',
        getter: (row) => row.name,
        order: 'asc',
      },
      {
        key: 'groupAsc',
        labelKey: 'table.sort.groupAsc',
        getter: (row) => row.groupCode,
        order: 'asc',
      },
    ],
    defaultSortKey: 'groupCodeAsc',
  })

  const catalogToolbarFilters = computed(() => [
    {
      key: 'groupCode',
      labelKey: 'masters.list.columns.group',
      type: 'text' as const,
    },
    {
      key: 'status',
      labelKey: 'masters.list.columns.status',
      type: 'select' as const,
      options: masterStatusFilterOptions.value,
    },
  ])

  const catalogSortOptions = computed(() => [
    { key: 'groupCodeAsc', labelKey: 'table.sort.groupAsc' },
    { key: 'updatedAtDesc', labelKey: 'table.sort.updatedAtDesc' },
    { key: 'updatedAtAsc', labelKey: 'table.sort.updatedAtAsc' },
    { key: 'nameAsc', labelKey: 'table.sort.nameAsc' },
    { key: 'groupAsc', labelKey: 'table.sort.groupAsc' },
  ])

  return {
    searchQuery,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
    catalogToolbarFilters,
    catalogSortOptions,
  }
}
