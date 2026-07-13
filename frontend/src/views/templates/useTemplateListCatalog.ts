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
import type { TemplateSummary, TemplateLifecycleStatus } from '@/types/template'
import { ElMessage } from 'element-plus'
import { createTemplateListCatalogControls } from '@/views/templates/createTemplateListCatalogControls'

export type WorkflowFilterKey = 'awaitingTest' | 'awaitingApproval' | 'awaitingPublish'

const WORKFLOW_CHIP_QUERY: Record<
  WorkflowFilterKey,
  { lifecycleStatus: TemplateLifecycleStatus; approvalSubState?: string }
> = {
  awaitingTest: { lifecycleStatus: 'TESTING' },
  awaitingApproval: { lifecycleStatus: 'APPROVAL', approvalSubState: 'PENDING_DECISION' },
  awaitingPublish: { lifecycleStatus: 'PENDING_RELEASE' },
}

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
      chips.push({
        key: 'awaitingTest',
        labelKey: 'templates.list.workflowFilters.awaitingTest',
      })
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

  const {
    searchQuery,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
    catalogToolbarFilters,
    catalogSortOptions,
  } = createTemplateListCatalogControls(catalogTemplates, lifecycleStatusFilterOptions)

  const hasActiveQuery = computed(
    () => hasAnyActive.value || activeWorkflowFilter.value !== null,
  )

  const showCatalogChrome = computed(
    () =>
      listHydrated.value &&
      !templatesStore.lastErrorMessageKey &&
      (templatesStore.templateListTotalElements > 0 || hasActiveQuery.value),
  )

  function buildListQuery() {
    const search = searchQuery.value.trim() || undefined
    const groupCode = filters.groupCode?.trim() || undefined
    const statusFilter = filters.status?.trim() || undefined
    const chip = activeWorkflowFilter.value
      ? WORKFLOW_CHIP_QUERY[activeWorkflowFilter.value]
      : null

    let lifecycleStatus: string | undefined
    let approvalSubState: string | undefined
    if (chip && statusFilter && chip.lifecycleStatus !== statusFilter) {
      // Impossible AND — still send both intents via chip status; backend returns empty.
      lifecycleStatus = statusFilter
    } else {
      lifecycleStatus = statusFilter || chip?.lifecycleStatus
      approvalSubState = chip?.approvalSubState
    }

    return {
      search,
      groupCode,
      lifecycleStatus,
      approvalSubState,
      sort: activeSortKey.value || 'groupCodeAsc',
    }
  }

  const { reload: reloadTemplates, signal: abortSignal } = useAbortableCatalogLoader(async (signal) => {
    await templatesStore.fetchTemplates(currentPage.value - 1, SERVER_TABLE_PAGE_SIZE, {
      signal,
      ...buildListQuery(),
    })
    listHydrated.value = true
  })

  watch(currentPage, async (page) => {
    const serverPage = page - 1
    if (serverPage === templatesStore.templateListPage) {
      return
    }
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
    [searchQuery, filters, activeSortKey, activeWorkflowFilter],
    async () => {
      if (!listHydrated.value) {
        return
      }
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

  function clearWorkflowFilter() {
    activeWorkflowFilter.value = null
  }

  function onWorkflowFilterChange(key: WorkflowFilterKey, checked: boolean) {
    activeWorkflowFilter.value = checked ? key : null
  }

  function handleCreated(templateId: string) {
    ElMessage.success(t('templates.create.success'))
    router.push(templateDetailPath(templateId))
  }

  function handleImported(templateId: string) {
    ElMessage.success(t('templates.import.success'))
    router.push(templateDetailPath(templateId))
  }

  function openTemplate(templateId: string) {
    router.push(templateDetailPath(templateId))
  }

  const { onRowClick: activateTemplateRow } = useActivatableTableRow<TemplateSummary>((row) =>
    openTemplate(row.id),
  )

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
    reloadTemplates,
    clearWorkflowFilter,
    onWorkflowFilterChange,
    handleCreated,
    handleImported,
    activateTemplateRow,
  }
}
