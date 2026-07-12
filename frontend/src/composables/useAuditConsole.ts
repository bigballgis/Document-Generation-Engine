import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import type { RouteLocationRaw } from 'vue-router'
import { rowSortMethod } from '@/composables/useDataTableFilters'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useAuditEventTypeOptions } from '@/composables/useAuditEventTypeOptions'
import { useAuditTemplateFilterOptions } from '@/composables/useAuditTemplateFilterOptions'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { isGroupScopedAuditRole } from '@/auth/roles'
import { ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import { useAuditStore } from '@/stores/audit'
import { useSessionStore } from '@/stores/session'
import type { LifecycleAuditEvent, ManagementAuditEvent } from '@/types/audit'
import type { TemplateLifecycleStatus } from '@/types/template'
import { resolveAuditActorDisplay, resolveAuditTemplateDisplay } from '@/utils/auditEntityDisplay'
import type { AuditActorDisplayFields } from '@/utils/auditEntityDisplay'
import { downloadJsonExport } from '@/utils/downloadExport'
import { shouldShowAuditAdminJourney } from '@/utils/auditAdminJourney'
import { formatAuditEventType } from '@/utils/auditEventLabels'
import { validateGroupAdminAuditFilters } from '@/views/audit/auditFilterValidation'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useAuditConsole() {
  const { t, te } = useI18n()
  const route = useRoute()
  const { formatDateTime } = useLocaleFormatters()
  const auditStore = useAuditStore()
  const sessionStore = useSessionStore()

  const activeTab = ref<'management' | 'lifecycle'>('management')
  const loadFailed = ref(false)
  const filterValidationKey = ref<string | null>(null)

  const { reload: reloadActiveTab, signal: auditAbortSignal } = useAbortableCatalogLoader(async (signal) => {
    if (activeTab.value === 'management') {
      await auditStore.fetchManagementEvents(auditStore.managementPage, { signal })
      return
    }
    await auditStore.fetchLifecycleEvents(auditStore.lifecyclePage, { signal })
  })

  const showAuditAdminJourney = computed(() =>
    shouldShowAuditAdminJourney({ roles: sessionStore.session?.roles ?? [] }),
  )

  const eventLabelTranslator = computed(() => ({
    translate: t,
    hasKey: te,
  }))

  const loadErrorMessageKey = computed(() => {
    if (auditStore.lastErrorMessageKey) {
      return auditStore.lastErrorMessageKey
    }
    return activeTab.value === 'management'
      ? 'audit.error.loadManagement'
      : 'audit.error.loadLifecycle'
  })

  const errorMessage = computed(() => {
    const key = auditStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('audit.error.loadManagement')
  })

  const showGroupFilters = computed(() => isGroupScopedAuditRole(auditStore.actorRole))
  const { isGroupLocked: isAuditGroupLocked } = useScopedGroupOptions()
  const auditEventTypeOptions = useAuditEventTypeOptions()
  const { templateOptions, loadingTemplates, searchTemplates } = useAuditTemplateFilterOptions()
  const canLinkTemplates = computed(() => sessionStore.canAccessRoute(ROUTE_KEYS.templateManagement))

  const managementSource = computed(() => auditStore.managementEvents)
  const lifecycleSource = computed(() => auditStore.lifecycleEvents)

  const managementUiPage = computed({
    get: () => auditStore.managementPage + 1,
    set: (page: number) => {
      void auditStore.fetchManagementEvents(page - 1, { signal: auditAbortSignal.value })
    },
  })

  const lifecycleUiPage = computed({
    get: () => auditStore.lifecyclePage + 1,
    set: (page: number) => {
      void auditStore.fetchLifecycleEvents(page - 1, { signal: auditAbortSignal.value })
    },
  })

  function formatLifecycleState(state?: string) {
    if (!state) {
      return '—'
    }
    const key = `templates.status.${state as TemplateLifecycleStatus}`
    return te(key) ? t(key) : state
  }

  function formatDate(value: string) {
    return formatDateTime(value)
  }

  function formatEventType(eventType?: string) {
    if (!eventType) {
      return '—'
    }
    return formatAuditEventType(eventType, eventLabelTranslator.value)
  }

  function formatActor(event: AuditActorDisplayFields) {
    return resolveAuditActorDisplay(event)
  }

  function resolveTemplateCell(
    event: Pick<
      ManagementAuditEvent,
      'templateId' | 'templateDisplayName' | 'templateExternalId'
    >,
  ) {
    const display = resolveAuditTemplateDisplay(event)
    const to: RouteLocationRaw | undefined =
      event.templateId && canLinkTemplates.value
        ? templatePackageHubPath(event.templateId)
        : undefined
    return { ...display, to }
  }

  function handleTemplateFilterSearch(query: string) {
    void searchTemplates(query)
  }

  function exportScopeSummary(): string {
    const parts: string[] = []
    if (auditStore.filters.eventType?.trim()) {
      parts.push(`${t('audit.filters.eventType')}: ${auditStore.filters.eventType.trim()}`)
    }
    if (auditStore.filters.eventAtFrom?.trim()) {
      parts.push(`${t('audit.filters.eventAtFrom')}: ${auditStore.filters.eventAtFrom.trim()}`)
    }
    if (auditStore.filters.eventAtTo?.trim()) {
      parts.push(`${t('audit.filters.eventAtTo')}: ${auditStore.filters.eventAtTo.trim()}`)
    }
    if (auditStore.filters.groupScope?.trim()) {
      parts.push(`${t('audit.filters.groupScope')}: ${auditStore.filters.groupScope.trim()}`)
    }
    if (auditStore.filters.templateId?.trim()) {
      parts.push(`${t('audit.filters.templateId')}: ${auditStore.filters.templateId.trim()}`)
    }
    if (auditStore.filters.requestId?.trim()) {
      parts.push(`${t('audit.filters.requestId')}: ${auditStore.filters.requestId.trim()}`)
    }
    if (parts.length === 0) {
      return t('audit.export.scopeAll')
    }
    return parts.join('\n')
  }

  function applyRequestIdFromRouteQuery() {
    const raw = route.query.requestId
    const requestId = Array.isArray(raw) ? raw[0] : raw
    if (typeof requestId === 'string' && requestId.trim().length > 0) {
      auditStore.filters.requestId = requestId.trim()
    }
  }

  onMounted(async () => {
    auditStore.initializeFiltersFromSession()
    applyRequestIdFromRouteQuery()
    if (showGroupFilters.value) {
      await searchTemplates('')
    }
    await refreshActiveTab()
  })

  watch(activeTab, () => {
    void refreshActiveTab()
  })

  async function refreshActiveTab() {
    if (showGroupFilters.value) {
      filterValidationKey.value = validateGroupAdminAuditFilters(auditStore.filters)
      if (filterValidationKey.value) {
        return
      }
    } else {
      filterValidationKey.value = null
    }

    loadFailed.value = false
    try {
      await reloadActiveTab()
    } catch {
      loadFailed.value = true
    }
  }

  async function handleTabChange(tab: string | number | boolean) {
    activeTab.value = tab as 'management' | 'lifecycle'
  }

  async function applyFilters() {
    if (showGroupFilters.value) {
      filterValidationKey.value = validateGroupAdminAuditFilters(auditStore.filters)
      if (filterValidationKey.value) {
        return
      }
    } else {
      filterValidationKey.value = null
    }

    loadFailed.value = false
    try {
      if (activeTab.value === 'management') {
        await auditStore.fetchManagementEvents(0, { signal: auditAbortSignal.value })
      } else {
        await auditStore.fetchLifecycleEvents(0, { signal: auditAbortSignal.value })
      }
    } catch {
      loadFailed.value = true
    }
  }

  async function resetFilters() {
    auditStore.resetFilters()
    filterValidationKey.value = null
    await applyFilters()
  }

  async function handleExport() {
    const isManagement = activeTab.value === 'management'
    try {
      await ElMessageBox.confirm(exportScopeSummary(), t('audit.export.confirmTitle'), {
        type: 'info',
        confirmButtonText: t('audit.export.confirmAction'),
        cancelButtonText: t('audit.export.cancelAction'),
      })
    } catch {
      return
    }
    try {
      const result = isManagement
        ? await auditStore.exportManagementEvents()
        : await auditStore.exportLifecycleEvents()
      downloadJsonExport(
        t(isManagement ? 'audit.export.managementFilename' : 'audit.export.lifecycleFilename'),
        result,
      )
      ElMessage.success(
        t(isManagement ? 'audit.export.success' : 'audit.export.lifecycleSuccess'),
      )
    } catch {
      ElMessage.error(
        errorMessage.value ||
          t(isManagement ? 'audit.error.export' : 'audit.error.exportLifecycle'),
      )
    }
  }

  const sortManagementByActor = rowSortMethod<ManagementAuditEvent>((row) => formatActor(row))
  const sortManagementByTemplate = rowSortMethod<ManagementAuditEvent>(
    (row) => resolveAuditTemplateDisplay(row).label,
  )
  const sortLifecycleByActor = rowSortMethod<LifecycleAuditEvent>((row) => formatActor(row))
  const sortLifecycleByTemplate = rowSortMethod<LifecycleAuditEvent>(
    (row) => resolveAuditTemplateDisplay(row).label,
  )
  const sortManagementByEventType = rowSortMethod<ManagementAuditEvent>((row) =>
    formatEventType(row.eventType),
  )
  const sortManagementByEventAt = rowSortMethod<ManagementAuditEvent>((row) => row.eventAt)
  const sortLifecycleByEventType = rowSortMethod<LifecycleAuditEvent>((row) =>
    formatEventType(row.eventType),
  )
  const sortLifecycleByEventAt = rowSortMethod<LifecycleAuditEvent>((row) => row.eventAt)
  const sortLifecycleFromState = rowSortMethod<LifecycleAuditEvent>((row) =>
    formatLifecycleState(row.fromState),
  )
  const sortLifecycleToState = rowSortMethod<LifecycleAuditEvent>((row) =>
    formatLifecycleState(row.toState),
  )

  return {
    t,
    auditStore,
    activeTab,
    loadFailed,
    filterValidationKey,
    showAuditAdminJourney,
    loadErrorMessageKey,
    showGroupFilters,
    isAuditGroupLocked,
    auditEventTypeOptions,
    templateOptions,
    loadingTemplates,
    managementSource,
    lifecycleSource,
    managementUiPage,
    lifecycleUiPage,
    formatLifecycleState,
    formatDate,
    formatEventType,
    formatActor,
    resolveTemplateCell,
    handleTemplateFilterSearch,
    refreshActiveTab,
    handleTabChange,
    applyFilters,
    resetFilters,
    handleExport,
    sortManagementByActor,
    sortManagementByTemplate,
    sortLifecycleByActor,
    sortLifecycleByTemplate,
    sortManagementByEventType,
    sortManagementByEventAt,
    sortLifecycleByEventType,
    sortLifecycleByEventAt,
    sortLifecycleFromState,
    sortLifecycleToState,
  }
}
