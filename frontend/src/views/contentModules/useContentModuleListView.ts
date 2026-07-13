import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useCapabilities } from '@/composables/useCapabilities'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { contentModuleDetailPath } from '@/routing/routeKeys'
import { useContentModulesStore } from '@/stores/contentModules'
import type { ContentModuleSummary } from '@/types/contentModule'
import { ElMessage } from 'element-plus'

export function useContentModuleListView() {
  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const router = useRouter()
  const contentModulesStore = useContentModulesStore()
  const { authorContentModules } = useCapabilities()
  const { contentModuleDetailLink } = useEntityLinkTargets()

  const createDialogOpen = ref(false)
  const currentPage = ref(1)
  const listHydrated = ref(false)

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

  function buildListQuery() {
    return {
      search: searchQuery.value.trim() || undefined,
      groupCode: filters.groupCode?.trim() || undefined,
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
    [searchQuery, filters, activeSortKey],
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
    createDialogOpen,
    currentPage,
    allModules,
    searchQuery,
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
