import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import en from '@/i18n/locales/en'
import {
  AUDIT_EVENT_TYPE_CODES,
  useAuditEventTypeOptions,
} from '@/composables/useAuditEventTypeOptions'

describe('useAuditEventTypeOptions', () => {
  function mountOptions() {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    let options: ReturnType<typeof useAuditEventTypeOptions>['value'] = []

    const Harness = defineComponent({
      setup() {
        options = useAuditEventTypeOptions().value
        return () => null
      },
    })

    mount(Harness, {
      global: {
        plugins: [i18n],
      },
    })
    return options
  }

  it('builds labeled options from audit.eventTypes i18n keys', () => {
    const options = mountOptions()
    expect(options).toHaveLength(AUDIT_EVENT_TYPE_CODES.length)
    expect(options.find((option) => option.value === 'PUBLISH')?.label).toBe('Template go-live')
    expect(options.find((option) => option.value === 'COLLABORATION_TIMEOUT_ESCALATION')?.label).toBe(
      'Overdue reminder sent',
    )
  })

  it('includes ManagementAuditEventTypes catalogue codes with human labels (FOS-W1-3)', () => {
    const options = mountOptions()
    expect(AUDIT_EVENT_TYPE_CODES.length).toBeGreaterThanOrEqual(45)
    for (const code of ['USER_DELETED', 'LEGAL_HOLD_CREATED', 'API_POLICY_UPDATED'] as const) {
      expect(AUDIT_EVENT_TYPE_CODES).toContain(code)
      const opt = options.find((o) => o.value === code)
      expect(opt?.label).toBeTruthy()
      expect(opt?.label).not.toBe(code)
    }
  })
})
