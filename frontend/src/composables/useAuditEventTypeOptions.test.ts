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
  it('builds labeled options from audit.eventTypes i18n keys', () => {
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

    expect(options).toHaveLength(AUDIT_EVENT_TYPE_CODES.length)
    expect(options.find((option) => option.value === 'PUBLISH')?.label).toBe('Template go-live')
    expect(options.find((option) => option.value === 'COLLABORATION_TIMEOUT_ESCALATION')?.label).toBe(
      'Overdue reminder sent',
    )
  })
})
