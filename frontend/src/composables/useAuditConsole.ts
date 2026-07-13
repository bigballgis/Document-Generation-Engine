import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useAuditEventTypeOptions } from '@/composables/useAuditEventTypeOptions'
import { useAuditTemplateFilterOptions } from '@/composables/useAuditTemplateFilterOptions'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { createAuditConsoleSorts } from '@/composables/createAuditConsoleSorts'
import { createAuditConsoleDisplayHelpers } from '@/composables/createAuditConsoleDisplayHelpers'
import { createAuditConsoleFilterActions } from '@/composables/createAuditConsoleFilterActions'
import { isGroupScopedAuditRole } from '@/auth/roles'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useAuditStore } from '@/stores/audit'
import { useSessionStore } from '@/stores/session'
import { shouldShowAuditAdminJourney } from '@/utils/auditAdminJourney'
import { useAuditConsoleExport } from '@/composables/useAuditConsoleExport'

export function useAuditConsole() {
  const { t, te } = useI18n()
  const route = useRoute()
  const { formatDateTime } = useLocaleFormatters()
  const auditStore = useAuditStore()
  const sessionStore = useSessionStore()

  const activeTab = ref<'management' | 'lifecycle'>('management')
  const loadFailed = ref(false)
  const filterValidationKey = ref<string | null>(null)

  const { reload: reloadActiveTab, signal: auditAbortSignal } = useAbortableCatalogLoader(
    async (signal) => {
      if (activeTab.value === 'management') {
        await auditStore.fetchManagementEvents(auditStore.managementPage, { signal })
        return
      }
      await auditStore.fetchLifecycleEvents(auditStore.lifecyclePage, { signal })
    },
  )

  const showAuditAdminJourney = computed(() =>
    shouldShowAuditAdminJourney({ roles: sessionStore.session?.roles ?? [] }),
  )
  const loadErrorMessageKey = computed(() =>
    auditStore.lastErrorMessageKey ??
    (activeTab.value === 'management' ? 'audit.error.loadManagement' : 'audit.error.loadLifecycle'),
  )
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

  const display = createAuditConsoleDisplayHelpers({
    t,
    te,
    formatDateTime,
    canLinkTemplates: () => canLinkTemplates.value,
  })

  const filterActions = createAuditConsoleFilterActions({
    auditStore,
    activeTab,
    filterValidationKey,
    loadFailed,
    showGroupFilters: () => showGroupFilters.value,
    reloadActiveTab,
    auditAbortSignal,
  })

  onMounted(async () => {
    auditStore.initializeFiltersFromSession()
    const raw = route.query.requestId
    const requestId = Array.isArray(raw) ? raw[0] : raw
    if (typeof requestId === 'string' && requestId.trim().length > 0) {
      auditStore.filters.requestId = requestId.trim()
    }
    if (showGroupFilters.value) {
      await searchTemplates('')
    }
    await filterActions.refreshActiveTab()
  })

  watch(activeTab, () => {
    void filterActions.refreshActiveTab()
  })

  const errorMessage = computed(() => {
    const key = auditStore.lastErrorMessageKey
    if (!key) return ''
    return te(key) ? t(key) : t('audit.error.loadManagement')
  })

  const { handleExport } = useAuditConsoleExport({
    t,
    auditStore,
    activeTab,
    errorMessage: errorMessage as unknown as import('vue').Ref<string>,
  })

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
    ...display,
    handleTemplateFilterSearch: (query: string) => {
      void searchTemplates(query)
    },
    ...filterActions,
    handleExport,
    ...createAuditConsoleSorts({
      formatActor: display.formatActor,
      formatEventType: display.formatEventType,
      formatLifecycleState: display.formatLifecycleState,
    }),
  }
}
