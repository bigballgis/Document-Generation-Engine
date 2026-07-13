import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLifecycleStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { useCapabilities } from '@/composables/useCapabilities'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { templateDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateSummary } from '@/types/template'
import { ElMessage } from 'element-plus'
import { createTemplateListCatalogControls } from '@/views/templates/createTemplateListCatalogControls'
import {
  buildTemplateListQuery,
  type WorkflowFilterKey,
} from '@/views/templates/templateListCatalogQuery'

export type { WorkflowFilterKey }

export function useTemplateListCatalog() {
  const { t } = useI18n()
  const lifecycleStatusFilterOptions = useLifecycleStatusFilterOptions()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const { authorTemplates, exportTemplates, decideTests, decideApprovals, publishTemplates } =
    useCapabilities()
  const { templateDetailLink } = useEntityLinkTargets()

  const activeWorkflowFilter = ref<WorkflowFilterKey | null>(null)
  const createDialogOpen = ref(false)
  const importDialogOpen = ref(false)
  const currentPage = ref(1)
  const listHydrated = ref(false)

  const workflowFilterChips = computed(() => {
    const chips: Array<{ key: WorkflowFilterKey; labelKey: string }> = []
    if (decideTests.value) {
      chips.push({ key: 'awaitingTest', labelKey: 'templates.list.workflowFilters.awaitingTest' })
    }
    if (decideApprovals.value) {
      chips.push({
        key: 'awaitingApproval',
        labelKey: 'templates.list.workflowFilters.awaitingApproval',
      })
    }
    if (publishTemplates.value) {
      chips.push({
        key: 'awaitingPublish',
        labelKey: 'templates.list.workflowFilters.awaitingPublish',
      })
    }
    return chips
  })

  const catalogTemplates = computed(() => templatesStore.templates)
  const controls = createTemplateListCatalogControls(catalogTemplates, lifecycleStatusFilterOptions)
  const hasActiveQuery = computed(
    () => controls.hasAnyActive.value || activeWorkflowFilter.value !== null,
  )
  const showCatalogChrome = computed(
    () =>
      listHydrated.value &&
      !templatesStore.lastErrorMessageKey &&
      (templatesStore.templateListTotalElements > 0 || hasActiveQuery.value),
  )

  function buildListQuery() {
    return buildTemplateListQuery({
      searchQuery: controls.searchQuery.value,
      groupCode: controls.filters.groupCode,
      statusFilter: controls.filters.status,
      activeWorkflowFilter: activeWorkflowFilter.value,
      activeSortKey: controls.activeSortKey.value,
    })
  }

  const { reload: reloadTemplates, signal: abortSignal } = useAbortableCatalogLoader(
    async (signal) => {
      await templatesStore.fetchTemplates(currentPage.value - 1, SERVER_TABLE_PAGE_SIZE, {
        signal,
        ...buildListQuery(),
      })
      listHydrated.value = true
    },
  )

  watch(currentPage, async (page) => {
    const serverPage = page - 1
    if (serverPage === templatesStore.templateListPage) return
    try {
      await templatesStore.fetchTemplates(serverPage, SERVER_TABLE_PAGE_SIZE, {
        signal: abortSignal.value,
        ...buildListQuery(),
      })
    } catch {
      // Error surfaced via store message key.
    }
  })

  watch(
    [controls.searchQuery, controls.filters, controls.activeSortKey, activeWorkflowFilter],
    async () => {
      if (!listHydrated.value) return
      if (currentPage.value !== 1) {
        currentPage.value = 1
        return
      }
      await reloadTemplates()
    },
    { deep: true },
  )

  onMounted(async () => {
    await reloadTemplates()
  })

  const { onRowClick: activateTemplateRow } = useActivatableTableRow<TemplateSummary>((row) => {
    router.push(templateDetailPath(row.id))
  })

  return {
    t,
    templatesStore,
    authorTemplates,
    exportTemplates,
    templateDetailLink,
    activeWorkflowFilter,
    createDialogOpen,
    importDialogOpen,
    currentPage,
    workflowFilterChips,
    catalogTemplates,
    searchQuery: controls.searchQuery,
    filters: controls.filters,
    activeSortKey: controls.activeSortKey,
    hasAnyActive: controls.hasAnyActive,
    activeFilterChips: controls.activeFilterChips,
    clearAll: controls.clearAll,
    removeFilterChip: controls.removeFilterChip,
    catalogToolbarFilters: controls.catalogToolbarFilters,
    catalogSortOptions: controls.catalogSortOptions,
    showCatalogChrome,
    reloadTemplates,
    clearWorkflowFilter: () => {
      activeWorkflowFilter.value = null
    },
    onWorkflowFilterChange: (key: WorkflowFilterKey, checked: boolean) => {
      activeWorkflowFilter.value = checked ? key : null
    },
    handleCreated: (templateId: string) => {
      ElMessage.success(t('templates.create.success'))
      router.push(templateDetailPath(templateId))
    },
    handleImported: (templateId: string) => {
      ElMessage.success(t('templates.import.success'))
      router.push(templateDetailPath(templateId))
    },
    activateTemplateRow,
  }
}
