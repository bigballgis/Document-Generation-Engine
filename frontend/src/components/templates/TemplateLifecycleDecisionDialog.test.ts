import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it } from 'vitest'
import TemplateLifecycleDecisionDialog from '@/components/templates/TemplateLifecycleDecisionDialog.vue'
import en from '@/i18n/locales/en'

describe('TemplateLifecycleDecisionDialog', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDialog(mode: 'test-fail' | 'approval-reject' = 'test-fail') {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(TemplateLifecycleDecisionDialog, {
      props: {
        modelValue: true,
        mode,
      },
      attachTo: document.body,
      global: {
        plugins: [i18n, ElementPlus],
      },
    })
  }

  it('wires el-form validation rules for structured negative decisions', async () => {
    const wrapper = mountDialog('approval-reject')
    await flushPromises()

    const form = wrapper.findComponent({ name: 'ElForm' })
    const rules = form.props('rules') as Record<string, Array<{ required?: boolean }>>

    expect(rules.reasonCategory?.[0]?.required).toBe(true)
    expect(rules.impactSummary?.[0]?.required).toBe(true)
  })

  it('uses mode-specific dialog titles', async () => {
    const failWrapper = mountDialog('test-fail')
    await flushPromises()
    expect(failWrapper.text()).toContain('Record test failure')

    failWrapper.unmount()
    document.body.innerHTML = ''

    const rejectWrapper = mountDialog('approval-reject')
    await flushPromises()
    expect(rejectWrapper.text()).toContain('Reject template')
  })
})
