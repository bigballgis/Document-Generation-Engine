import type { Ref } from 'vue'
import * as auditApi from '@/api/audit'
import { resolveApiErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import type {
  AuditQueryFilters,
  LifecycleAuditEvent,
  LifecycleAuditExportResult,
  ManagementAuditEvent,
  ManagementAuditExportResult,
} from '@/types/audit'

export function createAuditQueryActions(deps: {
  managementEvents: Ref<ManagementAuditEvent[]>
  lifecycleEvents: Ref<LifecycleAuditEvent[]>
  managementTotalElements: Ref<number>
  lifecycleTotalElements: Ref<number>
  managementPage: Ref<number>
  lifecyclePage: Ref<number>
  pageSize: Ref<number>
  exportResult: Ref<ManagementAuditExportResult | LifecycleAuditExportResult | null>
  loadingManagement: Ref<boolean>
  loadingLifecycle: Ref<boolean>
  exporting: Ref<boolean>
  lastErrorMessageKey: Ref<string | null>
  lastListErrorRetryable: Ref<boolean>
  buildQueryFilters: (page: number, size: number) => AuditQueryFilters
}) {
  const {
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
  } = deps

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
      exportResult.value = await auditApi.exportManagementEvents(
        buildQueryFilters(0, pageSize.value),
      )
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
      exportResult.value = await auditApi.exportLifecycleEvents(
        buildQueryFilters(0, pageSize.value),
      )
      return exportResult.value as LifecycleAuditExportResult
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'audit.error.exportLifecycle')
      throw error
    } finally {
      exporting.value = false
    }
  }

  return {
    fetchManagementEvents,
    fetchLifecycleEvents,
    exportManagementEvents,
    exportLifecycleEvents,
  }
}
