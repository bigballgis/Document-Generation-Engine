import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as auditApi from '@/api/audit'
import { resolveApiErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import { isGlobalAdmin } from '@/auth/identityRoles'
import { isGroupScopedAuditRole, resolveAuditActorRole } from '@/auth/roles'
import type {
  AuditQueryFilters,
  LifecycleAuditEvent,
  LifecycleAuditExportResult,
  ManagementAuditEvent,
  ManagementAuditExportResult,
} from '@/types/audit'
import { useSessionStore } from '@/stores/session'

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
      groupScope: '',
      eventAtFrom: '',
      eventAtTo: '',
    }
    managementPage.value = 0
    lifecyclePage.value = 0
    initializeFiltersFromSession()
  }

  async function fetchManagementEvents(
    page = managementPage.value,
    options: AbortableRequestOptions = {},
  ): Promise<void> {
    loadingManagement.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    managementPage.value = page
    try {
      const result = await auditApi.listManagementEvents(
        buildQueryFilters(page, pageSize.value),
        options,
      )
      managementEvents.value = result.events
      managementTotalElements.value = result.totalElements
    } catch (error) {
      handleStoreListFailure(
        error,
        'audit.error.loadManagement',
        lastErrorMessageKey,
        lastListErrorRetryable,
      )
    } finally {
      loadingManagement.value = false
    }
  }

  async function fetchLifecycleEvents(
    page = lifecyclePage.value,
    options: AbortableRequestOptions = {},
  ): Promise<void> {
    loadingLifecycle.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    lifecyclePage.value = page
    try {
      const result = await auditApi.listLifecycleEvents(
        buildQueryFilters(page, pageSize.value),
        options,
      )
      lifecycleEvents.value = result.events
      lifecycleTotalElements.value = result.totalElements
    } catch (error) {
      handleStoreListFailure(
        error,
        'audit.error.loadLifecycle',
        lastErrorMessageKey,
        lastListErrorRetryable,
      )
    } finally {
      loadingLifecycle.value = false
    }
  }

  async function exportManagementEvents(): Promise<ManagementAuditExportResult> {
    exporting.value = true
    lastErrorMessageKey.value = null
    try {
      exportResult.value = await auditApi.exportManagementEvents(buildQueryFilters(0, pageSize.value))
      return exportResult.value as ManagementAuditExportResult
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'audit.error.export')
      throw error
    } finally {
      exporting.value = false
    }
  }

  async function exportLifecycleEvents(): Promise<LifecycleAuditExportResult> {
    exporting.value = true
    lastErrorMessageKey.value = null
    try {
      exportResult.value = await auditApi.exportLifecycleEvents(buildQueryFilters(0, pageSize.value))
      return exportResult.value as LifecycleAuditExportResult
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'audit.error.exportLifecycle')
      throw error
    } finally {
      exporting.value = false
    }
  }

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
    fetchManagementEvents,
    fetchLifecycleEvents,
    exportManagementEvents,
    exportLifecycleEvents,
  }
})
