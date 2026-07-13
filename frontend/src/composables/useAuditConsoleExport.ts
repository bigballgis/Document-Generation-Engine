import type { Composer } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadJsonExport } from '@/utils/downloadExport'
import type { useAuditStore } from '@/stores/audit'
import type { Ref } from 'vue'

type AuditStore = ReturnType<typeof useAuditStore>

export function useAuditConsoleExport(options: {
  t: Composer['t']
  auditStore: AuditStore
  activeTab: Ref<'management' | 'lifecycle'>
  errorMessage: Ref<string>
}) {
  function exportScopeSummary(): string {
    const parts: string[] = []
    const { t, auditStore } = options
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

  async function handleExport() {
    const { t, auditStore, activeTab, errorMessage } = options
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

  return { handleExport }
}
