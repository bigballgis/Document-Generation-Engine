import { computed, type ComputedRef, type Ref } from 'vue'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import type { TableColumnFilterOption } from '@/composables/useTableFilterOptions'
import type { LibraryAssetView } from '@/types/libraryAsset'

type StatusOption = TableColumnFilterOption
type ClassOption = TableColumnFilterOption

export function createAssetLibraryCatalogControls(
  allAssets: ComputedRef<LibraryAssetView[]>,
  classFilterOptions: Ref<ClassOption[]>,
  statusFilterOptions: Ref<StatusOption[]>,
) {
  const {
    searchQuery,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
  } = useCatalogTableControls(allAssets, {
    searchGetters: [
      (row) => row.assetKey,
      (row) => row.originalFileName,
      (row) => row.uploadedBy,
    ],
    filters: [
      {
        key: 'assetClass',
        labelKey: 'assetLibrary.list.columns.assetClass',
        getValue: (row) => row.assetClass,
        matchMode: 'exact',
      },
      {
        key: 'status',
        labelKey: 'assetLibrary.list.columns.status',
        getValue: (row) => row.status,
        matchMode: 'exact',
      },
    ],
    sortOptions: [
      {
        key: 'uploadedAtDesc',
        labelKey: 'table.sort.updatedAtDesc',
        getter: (row) => row.uploadedAt,
        order: 'desc',
      },
      {
        key: 'uploadedAtAsc',
        labelKey: 'table.sort.updatedAtAsc',
        getter: (row) => row.uploadedAt,
        order: 'asc',
      },
      {
        key: 'assetKeyAsc',
        labelKey: 'assetLibrary.list.sort.assetKeyAsc',
        getter: (row) => row.assetKey,
        order: 'asc',
      },
    ],
    defaultSortKey: 'uploadedAtDesc',
  })

  const catalogToolbarFilters = computed(() => {
    const filtersDef = [
      {
        key: 'assetClass',
        labelKey: 'assetLibrary.list.columns.assetClass',
        type: 'select' as const,
        options: classFilterOptions.value,
      },
    ]
    if (statusFilterOptions.value.length > 0) {
      filtersDef.push({
        key: 'status',
        labelKey: 'assetLibrary.list.columns.status',
        type: 'select' as const,
        options: statusFilterOptions.value,
      })
    }
    return filtersDef
  })

  const catalogSortOptions = computed(() => [
    { key: 'uploadedAtDesc', labelKey: 'table.sort.updatedAtDesc' },
    { key: 'uploadedAtAsc', labelKey: 'table.sort.updatedAtAsc' },
    { key: 'assetKeyAsc', labelKey: 'assetLibrary.list.sort.assetKeyAsc' },
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
