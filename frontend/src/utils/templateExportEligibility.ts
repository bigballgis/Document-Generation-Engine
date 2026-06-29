import type { TemplateLifecycleStatus } from '@/types/template'

export const TEMPLATE_EXPORT_ELIGIBLE_STATUSES: ReadonlySet<TemplateLifecycleStatus> = new Set([
  'PENDING_RELEASE',
  'PUBLISHED',
  'STOPPED',
  'DEPRECATED',
])

export function isTemplateExportEligible(status: TemplateLifecycleStatus): boolean {
  return TEMPLATE_EXPORT_ELIGIBLE_STATUSES.has(status)
}
