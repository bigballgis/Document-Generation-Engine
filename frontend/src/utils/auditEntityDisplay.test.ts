import { describe, expect, it } from 'vitest'
import {
  resolveAuditActorDisplay,
  resolveAuditTemplateDisplay,
  truncateEntityId,
} from '@/utils/auditEntityDisplay'

describe('auditEntityDisplay', () => {
  it('truncates long entity ids', () => {
    expect(truncateEntityId('abcdefgh-1234-5678-90ab-cdef12345678')).toBe('abcdefgh…')
    expect(truncateEntityId('short')).toBe('short')
  })

  it('resolves template label with graceful fallbacks', () => {
    expect(
      resolveAuditTemplateDisplay({
        templateDisplayName: 'Loan agreement',
        templateExternalId: 'TPL-001',
        templateId: 'tpl-uuid',
      }),
    ).toEqual({ label: 'Loan agreement', subtitle: 'TPL-001' })

    expect(
      resolveAuditTemplateDisplay({
        templateExternalId: 'TPL-001',
        templateId: 'tpl-uuid',
      }),
    ).toEqual({ label: 'TPL-001', subtitle: undefined })

    expect(
      resolveAuditTemplateDisplay({
        templateId: 'abcdefgh-1234-5678-90ab-cdef12345678',
      }),
    ).toEqual({ label: 'abcdefgh…', subtitle: undefined })
  })

  it('resolves actor display without exposing raw actor id', () => {
    expect(
      resolveAuditActorDisplay({
        actorDisplayName: 'Jane Doe',
        actorSummary: 'jane@example.com',
        actorId: 'user-uuid',
      }),
    ).toBe('Jane Doe')

    expect(
      resolveAuditActorDisplay({
        actorSummary: 'jane@example.com',
        actorId: 'user-uuid',
      }),
    ).toBe('jane@example.com')

    expect(
      resolveAuditActorDisplay({
        actorId: 'user-uuid',
      }),
    ).toBe('—')
  })
})
