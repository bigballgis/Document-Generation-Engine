import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import type { LifecycleAuditEvent, ManagementAuditEvent } from '@/types/audit'
import type { TemplateLifecycleStatus } from '@/types/template'
import type { ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'

export function useManagementAuditColumnFilters(
  source: ComputedRef<ManagementAuditEvent[]>,
) {
  const { formatDateTime } = useLocaleFormatters()

  return useDataTableFilters(source, [
    { key: 'eventType', getValue: (row) => row.eventType ?? '' },
    { key: 'actorSummary', getValue: (row) => row.actorSummary ?? '' },
    { key: 'eventAt', getValue: (row) => formatDateTime(row.eventAt) },
  ])
}

export function useLifecycleAuditColumnFilters(
  source: ComputedRef<LifecycleAuditEvent[]>,
) {
  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()

  function formatLifecycleState(state?: string) {
    if (!state) {
      return '—'
    }
    const key = `templates.status.${state as TemplateLifecycleStatus}`
    return te(key) ? t(key) : state
  }

  return useDataTableFilters(source, [
    { key: 'eventType', getValue: (row) => row.eventType ?? '' },
    { key: 'templateId', getValue: (row) => row.templateId ?? '' },
    { key: 'eventAt', getValue: (row) => formatDateTime(row.eventAt) },
    { key: 'fromState', getValue: (row) => formatLifecycleState(row.fromState) },
    { key: 'toState', getValue: (row) => formatLifecycleState(row.toState) },
  ])
}
