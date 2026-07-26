import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export interface TableColumnFilterOption {
  label: string
  value: string
}
import type { TemplateLifecycleStatus } from '@/types/template'

const LIFECYCLE_STATUSES: TemplateLifecycleStatus[] = [
  'DRAFT',
  'TESTING',
  'APPROVAL',
  'PENDING_RELEASE',
  'PUBLISHED',
  'STOPPED',
  'DEPRECATED',
]

const MASTER_STATUSES = ['DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'] as const

export function useLifecycleStatusFilterOptions() {
  const { t, te } = useI18n()
  return computed<TableColumnFilterOption[]>(() =>
    LIFECYCLE_STATUSES.map((status) => ({
      value: status,
      label: te(`templates.status.${status}`) ? t(`templates.status.${status}`) : status,
    })),
  )
}

export function useMasterStatusFilterOptions() {
  const { t, te } = useI18n()
  return computed<TableColumnFilterOption[]>(() =>
    MASTER_STATUSES.map((status) => ({
      value: status,
      label: te(`masters.status.${status}`) ? t(`masters.status.${status}`) : status,
    })),
  )
}

/** CE-U20 — content-module catalog status filter (badge-aligned). */
const CONTENT_MODULE_CATALOG_STATUSES = [
  'DRAFT',
  'SUBMITTED',
  'APPROVED',
  'STOPPED',
  'DEPRECATED',
] as const

export function useContentModuleStatusFilterOptions() {
  const { t, te } = useI18n()
  return computed<TableColumnFilterOption[]>(() =>
    CONTENT_MODULE_CATALOG_STATUSES.map((status) => {
      const reviewKey = `contentModules.reviewState.${status}`
      const lifecycleKey = `contentModules.lifecycle.${status}`
      if (te(reviewKey)) {
        return { value: status, label: t(reviewKey) }
      }
      if (te(lifecycleKey)) {
        return { value: status, label: t(lifecycleKey) }
      }
      return { value: status, label: status }
    }),
  )
}

export function useYesNoFilterOptions() {
  const { t } = useI18n()
  return computed<TableColumnFilterOption[]>(() => [
    { value: t('common.yes'), label: t('common.yes') },
    { value: t('common.no'), label: t('common.no') },
  ])
}

export function useCredentialStatusFilterOptions() {
  return computed<TableColumnFilterOption[]>(() => [
    { value: 'ACTIVE', label: 'ACTIVE' },
    { value: 'REVOKED', label: 'REVOKED' },
  ])
}
