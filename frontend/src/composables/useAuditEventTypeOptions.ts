import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TableColumnFilterOption } from '@/composables/useTableFilterOptions'
import { auditEventTypeMessageKey } from '@/utils/auditEventLabels'

/** Known audit event types with i18n labels under `audit.eventTypes.*`. */
export const AUDIT_EVENT_TYPE_CODES = [
  'PUBLISH',
  'RECORD_APPROVAL_DECISION',
  'SUBMIT_FOR_APPROVAL',
  'RECORD_TEST_DECISION',
  'SUBMIT_FOR_TEST',
  'COLLABORATION_TIMEOUT_ESCALATION',
] as const

export type AuditEventTypeCode = (typeof AUDIT_EVENT_TYPE_CODES)[number]

export function useAuditEventTypeOptions() {
  const { t, te } = useI18n()

  return computed<TableColumnFilterOption[]>(() =>
    AUDIT_EVENT_TYPE_CODES.map((eventType) => {
      const key = auditEventTypeMessageKey(eventType)
      return {
        value: eventType,
        label: te(key) ? t(key) : eventType,
      }
    }),
  )
}
