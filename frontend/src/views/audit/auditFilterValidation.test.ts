import { describe, expect, it } from 'vitest'
import { validateGroupAdminAuditFilters } from '@/views/audit/auditFilterValidation'

describe('validateGroupAdminAuditFilters', () => {
  it('requires group scope for group-scoped audit queries', () => {
    expect(
      validateGroupAdminAuditFilters({ groupScope: '', templateId: 'tpl-1' }),
    ).toBe('audit.filters.groupScopeRequired')
  })

  it('requires template id for group-scoped audit queries', () => {
    expect(
      validateGroupAdminAuditFilters({ groupScope: 'RETAIL', templateId: '' }),
    ).toBe('audit.filters.templateIdRequired')
  })

  it('passes when both scope fields are present', () => {
    expect(
      validateGroupAdminAuditFilters({ groupScope: 'RETAIL', templateId: 'tpl-1' }),
    ).toBeNull()
  })
})
