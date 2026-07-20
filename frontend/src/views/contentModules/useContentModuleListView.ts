import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useCapabilities } from '@/composables/useCapabilities'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { useContentModuleStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { DOCUMENT_LOCALE_OPTIONS } from '@/constants/documentLocales'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { contentModuleDetailPath } from '@/routing/routeKeys'
import { useContentModulesStore } from '@/stores/contentModules'
import type { ContentModuleSearchMode, ContentModuleSummary } from '@/types/contentModule'
import { contentModuleCatalogDisplayStatus } from '@/utils/contentModuleCatalogDisplayStatus'
import { ElMessage } from 'element-plus'

export function useContentModuleListView() {
  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const router = useRouter()
  const contentModulesStore = useContentModulesStore()
  const { authorContentModules } = useCapabilities()
  const { contentModuleDetailLink, groupCatalogLink } = useEntityLinkTargets()
  const statusFilterOptions = useContentModuleStatusFilterOptions()

  const createDialogOpen = ref(false)
  const currentPage = ref(1)
  const listHydrated = ref(false)
  /** CE-G05 — NAME (default) or FULL_TEXT body search. */
  const searchMode = ref<ContentModuleSearchMode>('NAME')

  const allModules = computed(() => contentModulesStore.modules)
  const {
    searchQuery,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
  } = useCatalogTableControls(allModules, {
    searchGetters: [
      (row) => row.name,
      (row) => row.moduleCode,
      (row) => row.groupCode,
    ],
    filters: [
      {
        key: 'groupCode',
        labelKey: 'contentModules.list.columns.group',
        getValue: (row) => row.groupCode,
      },
      {
        key: 'status',
        labelKey: 'contentModules.list.columns.status',
        getValue: (row) => contentModuleCatalogDisplayStatus(row),
        matchMode: 'exact',
      },
      {
        key: 'locale',
        labelKey: 'contentModules.list.columns.locale',
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
        key: 'moduleCodeAsc',
        labelKey: 'table.sort.moduleCodeAsc',
        getter: (row) => row.moduleCode,
        order: 'asc',
      },
    ],
    defaultSortKey: 'groupCodeAsc',
  })

  const catalogToolbarFilters = computed(() => [
    {
      key: 'groupCode',
      labelKey: 'contentModules.list.columns.group',
      type: 'text' as const,
    },
    {
      key: 'status',
      labelKey: 'contentModules.list.columns.status',
      type: 'select' as const,
      options: statusFilterOptions.value,
    },
    {
      key: 'locale',
      labelKey: 'contentModules.list.columns.locale',
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
    { key: 'moduleCodeAsc', labelKey: 'table.sort.moduleCodeAsc' },
  ])

  const showCatalogChrome = computed(
    () =>
      listHydrated.value &&
      !contentModulesStore.lastErrorMessageKey &&
      (contentModulesStore.moduleListTotalElements > 0 || hasAnyActive.value),
  )

  const searchPlaceholderKey = computed(() =>
    searchMode.value === 'FULL_TEXT'
      ? 'contentModules.list.searchPlaceholderFullText'
      : 'contentModules.list.searchPlaceholderName',
  )

  function buildListQuery() {
    return {
      search: searchQuery.value.trim() || undefined,
      searchMode: searchMode.value,
      groupCode: filters.groupCode?.trim() || undefined,
      status: filters.status?.trim() || undefined,
      locale: filters.locale?.trim() || undefined,
      sort: activeSortKey.value || 'groupCodeAsc',
    }
  }

  const canCreate = computed(() => authorContentModules.value)
  const errorMessage = computed(() => {
    const key = contentModulesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('contentModules.error.loadList')
  })

  const { reload: reloadModules, signal: abortSignal } = useAbortableCatalogLoader(async (signal) => {
    await contentModulesStore.fetchModules(currentPage.value - 1, SERVER_TABLE_PAGE_SIZE, {
      signal,
      ...buildListQuery(),
    })
    listHydrated.value = true
  })

  watch(currentPage, async (page) => {
    const serverPage = page - 1
    if (serverPage === contentModulesStore.moduleListPage) {
      return
    }
    try {
      await contentModulesStore.fetchModules(serverPage, SERVER_TABLE_PAGE_SIZE, {
        signal: abortSignal.value,
        ...buildListQuery(),
      })
    } catch {
      // Error surfaced via store message key.
    }
  })

  watch(
    [searchQuery, searchMode, filters, activeSortKey],
    async () => {
      if (!listHydrated.value) {
        return
      }
      if (currentPage.value !== 1) {
        currentPage.value = 1
        return
      }
      await reloadModules()
    },
    { deep: true },
  )

  onMounted(async () => {
    await reloadModules()
  })

  function openModule(moduleId: string) {
    router.push(contentModuleDetailPath(moduleId))
  }

  const { onRowClick: activateModuleRow } = useActivatableTableRow<ContentModuleSummary>((row) =>
    openModule(row.moduleId),
  )

  function handleCreated(moduleId: string) {
    ElMessage.success(t('contentModules.create.success'))
    router.push(contentModuleDetailPath(moduleId))
  }

  return {
    t,
    formatDateTime,
    contentModulesStore,
    contentModuleDetailLink,
    groupCatalogLink,
    createDialogOpen,
    currentPage,
    allModules,
    searchQuery,
    searchMode,
    searchPlaceholderKey,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
    catalogToolbarFilters,
    catalogSortOptions,
    showCatalogChrome,
    canCreate,
    errorMessage,
    reloadModules,
    activateModuleRow,
    handleCreated,
  }
}
