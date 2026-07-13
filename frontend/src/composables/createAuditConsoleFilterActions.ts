import type { Ref } from 'vue'
import { validateGroupAdminAuditFilters } from '@/views/audit/auditFilterValidation'
import type { useAuditStore } from '@/stores/audit'

type AuditStore = ReturnType<typeof useAuditStore>

export function createAuditConsoleFilterActions(deps: {
  auditStore: AuditStore
  activeTab: Ref<'management' | 'lifecycle'>
  filterValidationKey: Ref<string | null>
  loadFailed: Ref<boolean>
  showGroupFilters: () => boolean
  reloadActiveTab: () => Promise<void>
  auditAbortSignal: Ref<AbortSignal>
}) {
  const {
    auditStore,
    activeTab,
    filterValidationKey,
    loadFailed,
    showGroupFilters,
    reloadActiveTab,
    auditAbortSignal,
  } = deps

  function validateFilters(): boolean {
    if (showGroupFilters()) {
      filterValidationKey.value = validateGroupAdminAuditFilters(auditStore.filters)
      return !filterValidationKey.value
    }
    filterValidationKey.value = null
    return true
  }

  async function refreshActiveTab() {
    if (!validateFilters()) {
      return
    }
    loadFailed.value = false
    try {
      await reloadActiveTab()
    } catch {
      loadFailed.value = true
    }
  }

  async function applyFilters() {
    if (!validateFilters()) {
      return
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

  async function handleTabChange(tab: string | number | boolean) {
    activeTab.value = tab as 'management' | 'lifecycle'
  }

  return {
    refreshActiveTab,
    applyFilters,
    resetFilters,
    handleTabChange,
  }
}
