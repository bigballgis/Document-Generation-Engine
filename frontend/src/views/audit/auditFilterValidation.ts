export type AuditFilterValidationKey =
  | 'audit.filters.groupScopeRequired'
  | 'audit.filters.templateIdRequired'

export function validateGroupAdminAuditFilters(filters: {
  groupScope?: string
  templateId?: string
}): AuditFilterValidationKey | null {
  if (!filters.groupScope?.trim()) {
    return 'audit.filters.groupScopeRequired'
  }
  if (!filters.templateId?.trim()) {
    return 'audit.filters.templateIdRequired'
  }
  return null
}
