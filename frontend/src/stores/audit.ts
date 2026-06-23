import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as auditApi from '@/api/audit'
import { isApiError } from '@/api/http'
import { isGroupScopedAuditRole, resolveAuditActorRole } from '@/auth/roles'
import type {
  AuditQueryFilters,
  LifecycleAuditEvent,
  ManagementAuditEvent,
  ManagementAuditExportResult,
} from '@/types/audit'
import { useSessionStore } from '@/stores/session'

export const useAuditStore = defineStore('audit', () => {
  const managementEvents = ref<ManagementAuditEvent[]>([])
  const lifecycleEvents = ref<LifecycleAuditEvent[]>([])
  const exportResult = ref<ManagementAuditExportResult | null>(null)
  const loadingManagement = ref(false)
  const loadingLifecycle = ref(false)
  const exporting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)

  const filters = ref<AuditQueryFilters>({
    actorRole: 'GLOBAL_ADMIN',
    eventType: '',
    templateId: '',
    groupScope: '',
    eventAtFrom: '',
    eventAtTo: '',
  })

  const actorRole = computed(() => {
    const sessionStore = useSessionStore()
    return resolveAuditActorRole(sessionStore.session?.roles ?? [])
  })

  const requiresGroupScope = computed(() => isGroupScopedAuditRole(actorRole.value))

  function resolveErrorMessageKey(error: unknown, fallbackKey: string): string {
    if (isApiError(error) && error.response?.data.error?.messageKey) {
      return error.response.data.error.messageKey
    }
    return fallbackKey
  }

  function buildQueryFilters(): AuditQueryFilters {
    const role = actorRole.value
    if (!role) {
      throw new Error('No audit actor role available')
    }
    const query: AuditQueryFilters = { actorRole: role }
    if (filters.value.eventType?.trim()) {
      query.eventType = filters.value.eventType.trim()
    }
    if (filters.value.eventAtFrom?.trim()) {
      query.eventAtFrom = filters.value.eventAtFrom.trim()
    }
    if (filters.value.eventAtTo?.trim()) {
      query.eventAtTo = filters.value.eventAtTo.trim()
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
    if (isGroupScopedAuditRole(role) && sessionStore.session?.authorizedGroupCodes.length) {
      filters.value.groupScope = sessionStore.session.authorizedGroupCodes[0] ?? ''
    }
  }

  async function fetchManagementEvents(): Promise<void> {
    loadingManagement.value = true
    lastErrorMessageKey.value = null
    try {
      managementEvents.value = await auditApi.listManagementEvents(buildQueryFilters())
    } catch (error) {
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'audit.error.loadManagement')
      throw error
    } finally {
      loadingManagement.value = false
    }
  }

  async function fetchLifecycleEvents(): Promise<void> {
    loadingLifecycle.value = true
    lastErrorMessageKey.value = null
    try {
      lifecycleEvents.value = await auditApi.listLifecycleEvents(buildQueryFilters())
    } catch (error) {
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'audit.error.loadLifecycle')
      throw error
    } finally {
      loadingLifecycle.value = false
    }
  }

  async function exportManagementEvents(): Promise<ManagementAuditExportResult> {
    exporting.value = true
    lastErrorMessageKey.value = null
    try {
      exportResult.value = await auditApi.exportManagementEvents(buildQueryFilters())
      return exportResult.value
    } catch (error) {
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'audit.error.export')
      throw error
    } finally {
      exporting.value = false
    }
  }

  return {
    managementEvents,
    lifecycleEvents,
    exportResult,
    filters,
    loadingManagement,
    loadingLifecycle,
    exporting,
    lastErrorMessageKey,
    actorRole,
    requiresGroupScope,
    initializeFiltersFromSession,
    fetchManagementEvents,
    fetchLifecycleEvents,
    exportManagementEvents,
  }
})
