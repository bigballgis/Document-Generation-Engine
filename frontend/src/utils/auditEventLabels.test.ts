import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en'
import { auditEventTypeMessageKey, formatAuditEventType } from '@/utils/auditEventLabels'

describe('auditEventTypeMessageKey', () => {
  it('builds stable keys under audit.eventTypes', () => {
    expect(auditEventTypeMessageKey('PUBLISH')).toBe('audit.eventTypes.PUBLISH')
  })
})

describe('formatAuditEventType', () => {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })

  it('returns human-readable labels for mapped lifecycle and management codes', () => {
    const translator = {
      translate: i18n.global.t,
      hasKey: i18n.global.te,
    }
    expect(formatAuditEventType('PUBLISH', translator)).toBe('Template go-live')
    expect(formatAuditEventType('RECORD_APPROVAL_DECISION', translator)).toBe('Approval decision recorded')
    expect(formatAuditEventType('SUBMIT_FOR_APPROVAL', translator)).toBe('Submitted for approval')
    expect(formatAuditEventType('RECORD_TEST_DECISION', translator)).toBe('Test decision recorded')
    expect(formatAuditEventType('SUBMIT_FOR_TEST', translator)).toBe('Submitted for testing')
    expect(formatAuditEventType('COLLABORATION_TIMEOUT_ESCALATION', translator)).toBe(
      'Overdue reminder sent',
    )
  })

  it('falls back to the raw code when no label exists', () => {
    const translator = {
      translate: i18n.global.t,
      hasKey: i18n.global.te,
    }
    expect(formatAuditEventType('TOTALLY_UNKNOWN_EVENT_XYZ', translator)).toBe(
      'TOTALLY_UNKNOWN_EVENT_XYZ',
    )
  })
})
