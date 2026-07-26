import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import en from '@/i18n/locales/en'
import { useMasterStatusFilterOptions } from '@/composables/useTableFilterOptions'

describe('FOS-W1-4 master status filters', () => {
  it('excludes phantom ARCHIVED and matches backend MasterDocumentStatus', () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    let options: { value: string; label: string }[] = []
    const Comp = defineComponent({
      setup() {
        options = useMasterStatusFilterOptions().value
        return () => h('div')
      },
    })
    mount(Comp, { global: { plugins: [i18n] } })
    const values = options.map((o) => o.value)
    expect(values).toEqual(['DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'])
    expect(values).not.toContain('ARCHIVED')
  })
})
