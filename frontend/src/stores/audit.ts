import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { isGlobalAdmin } from '@/auth/identityRoles'
import { isGroupScopedAuditRole, resolveAuditActorRole } from '@/auth/roles'
import { createAuditQueryActions } from '@/stores/createAuditQueryActions'
import { useSessionStore } from '@/stores/session'
import type {
  AuditQueryFilters,
  LifecycleAuditEvent,
  LifecycleAuditExportResult,
  ManagementAuditEvent,
  ManagementAuditExportResult,
} from '@/types/audit'

const DEFAULT_PAGE_SIZE = 20

export const useAuditStore = defineStore('audit', () => {
  const managementEvents = ref<ManagementAuditEvent[]>([])
  const lifecycleEvents = ref<LifecycleAuditEvent[]>([])
  const managementTotalElements = ref(0)
  const lifecycleTotalElements = ref(0)
  const managementPage = ref(0)
  const lifecyclePage = ref(0)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  const exportResult = ref<ManagementAuditExportResult | LifecycleAuditExportResult | null>(null)
  const loadingManagement = ref(false)
  const loadingLifecycle = ref(false)
  const exporting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)
  const lastListErrorRetryable = ref(false)

  const filters = ref<AuditQueryFilters>({
    actorRole: 'GLOBAL_ADMIN',
    eventType: '',
    templateId: '',
    requestId: '',
    groupScope: '',
    eventAtFrom: '',
    eventAtTo: '',
  })

  const actorRole = computed(() => {
    const sessionStore = useSessionStore()
    return resolveAuditActorRole(sessionStore.session?.roles ?? [])
  })

  const requiresGroupScope = computed(() => isGroupScopedAuditRole(actorRole.value))

  function buildQueryFilters(page: number, size: number): AuditQueryFilters {
    const role = actorRole.value
    if (!role) {
      throw new Error('No audit actor role available')
    }
    const query: AuditQueryFilters = { actorRole: role, page, size }
    if (filters.value.eventType?.trim()) {
      query.eventType = filters.value.eventType.trim()
    }
    if (filters.value.eventAtFrom?.trim()) {
      query.eventAtFrom = filters.value.eventAtFrom.trim()
    }
    if (filters.value.eventAtTo?.trim()) {
      query.eventAtTo = filters.value.eventAtTo.trim()
    }
    if (filters.value.requestId?.trim()) {
      query.requestId = filters.value.requestId.trim()
    }
    if (requiresGroupScope.value) {
      if (filters.value.groupScope?.trim()) {
        query.groupScope = filters.value.groupScope.trim()
      }
      if (filters.value.templateId?.trim()) {
        query.templateId = filters.value.templateId.trim()
      }
    } else if (filters.value.templateId?.trim()) {
      query.templateId = filters.value.templateId.trim()
    }
    return query
  }

  function initializeFiltersFromSession() {
    const sessionStore = useSessionStore()
    const role = resolveAuditActorRole(sessionStore.session?.roles ?? [])
    if (role) {
      filters.value.actorRole = role
    }
    if (!isGroupScopedAuditRole(role) || !sessionStore.session) {
      return
    }
    const authorizedGroups = sessionStore.session.authorizedGroupCodes.filter((code) => code !== '*')
    if (authorizedGroups.length === 1 && !isGlobalAdmin(sessionStore.session.roles)) {
      filters.value.groupScope = authorizedGroups[0] ?? ''
    }
  }

  function resetFilters() {
    filters.value = {
      actorRole: actorRole.value ?? 'GLOBAL_ADMIN',
      eventType: '',
      templateId: '',
      requestId: '',
      groupScope: '',
      eventAtFrom: '',
      eventAtTo: '',
    }
    managementPage.value = 0
    lifecyclePage.value = 0
    initializeFiltersFromSession()
  }

  const queryActions = createAuditQueryActions({
    managementEvents,
    lifecycleEvents,
    managementTotalElements,
    lifecycleTotalElements,
    managementPage,
    lifecyclePage,
    pageSize,
    exportResult,
    loadingManagement,
    loadingLifecycle,
    exporting,
    lastErrorMessageKey,
    lastListErrorRetryable,
    buildQueryFilters,
  })

  return {
    managementEvents,
    lifecycleEvents,
    managementTotalElements,
    lifecycleTotalElements,
    managementPage,
    lifecyclePage,
    pageSize,
    exportResult,
    filters,
    loadingManagement,
    loadingLifecycle,
    exporting,
    lastErrorMessageKey,
    lastListErrorRetryable,
    actorRole,
    requiresGroupScope,
    initializeFiltersFromSession,
    resetFilters,
    ...queryActions,
  }
})
