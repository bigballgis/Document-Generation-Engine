import { computed, type ComputedRef, type Ref } from 'vue'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import { DOCUMENT_LOCALE_OPTIONS } from '@/constants/documentLocales'
import type { TemplateSummary } from '@/types/template'

type LifecycleStatusOption = { label: string; value: string }

export function createTemplateListCatalogControls(
  catalogTemplates: ComputedRef<TemplateSummary[]>,
  lifecycleStatusFilterOptions: Ref<LifecycleStatusOption[]>,
) {
  const {
    searchQuery,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
  } = useCatalogTableControls(catalogTemplates, {
    searchGetters: [(row) => row.name, (row) => row.externalId, (row) => row.groupCode],
    filters: [
      {
        key: 'groupCode',
        labelKey: 'templates.list.columns.group',
        getValue: (row) => row.groupCode,
      },
      {
        key: 'status',
        labelKey: 'templates.list.columns.status',
        getValue: (row) => row.lifecycleStatus,
        matchMode: 'exact',
      },
      {
        key: 'locale',
        labelKey: 'templates.list.columns.locale',
        getValue: (row) => row.locale ?? '',
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
        key: 'externalIdAsc',
        labelKey: 'table.sort.externalIdAsc',
        getter: (row) => row.externalId,
        order: 'asc',
      },
    ],
    defaultSortKey: 'groupCodeAsc',
  })

  const catalogToolbarFilters = computed(() => [
    { key: 'groupCode', labelKey: 'templates.list.columns.group', type: 'text' as const },
    {
      key: 'status',
      labelKey: 'templates.list.columns.status',
      type: 'select' as const,
      options: lifecycleStatusFilterOptions.value,
    },
    {
      key: 'locale',
      labelKey: 'templates.list.columns.locale',
      type: 'select' as const,
      options: DOCUMENT_LOCALE_OPTIONS.map((option) => ({
        value: option.value,
        label: option.value,
      })),
    },
  ])

  const catalogSortOptions = computed(() => [
    { key: 'groupCodeAsc', labelKey: 'table.sort.groupAsc' },
    { key: 'updatedAtDesc', labelKey: 'table.sort.updatedAtDesc' },
    { key: 'updatedAtAsc', labelKey: 'table.sort.updatedAtAsc' },
    { key: 'nameAsc', labelKey: 'table.sort.nameAsc' },
    { key: 'externalIdAsc', labelKey: 'table.sort.externalIdAsc' },
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
