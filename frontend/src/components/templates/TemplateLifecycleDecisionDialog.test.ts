import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import TemplateLifecycleDecisionDialog from '@/components/templates/TemplateLifecycleDecisionDialog.vue'
import en from '@/i18n/locales/en'

describe('TemplateLifecycleDecisionDialog', () => {
  let pinia: Pinia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDialog(mode: 'test-fail' | 'test-pass' | 'approval-reject' | 'approval-approve' = 'test-fail') {
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
        plugins: [pinia, i18n, ElementPlus],
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

  it('requires pass confirmations before enabling submit for test pass', async () => {
    const wrapper = mountDialog('test-pass')
    await flushPromises()

    const submitButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Submit decision'))
    expect(submitButton?.attributes('disabled')).toBeDefined()

    const checkboxes = wrapper.findAllComponents({ name: 'ElCheckbox' })
    for (const checkbox of checkboxes.slice(0, 3)) {
      await checkbox.setValue(true)
    }
    await flushPromises()

    expect(
      wrapper
        .findAll('button')
        .find((button) => button.text().includes('Submit decision'))
        ?.attributes('disabled'),
    ).toBeUndefined()
  })

  it('shows remediation fields for approval reject mode', async () => {
    const wrapper = mountDialog('approval-reject')
    await flushPromises()

    expect(wrapper.text()).toContain('Remediation test record ID')
    expect(wrapper.text()).toContain('Remediation change diff reference')
  })

  it('shows remediation fields for test-fail mode (AUD-B05)', async () => {
    const wrapper = mountDialog('test-fail')
    await flushPromises()

    expect(wrapper.text()).toContain('Remediation test record ID')
    expect(wrapper.text()).toContain('Remediation change diff reference')
  })

  it('keeps test-fail submit disabled until remediation link is provided (AUD-B05)', async () => {
    const wrapper = mountDialog('test-fail')
    await flushPromises()

    const submitButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Submit decision'))
    expect(submitButton?.attributes('disabled')).toBeDefined()

    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    await selects[0]?.setValue('BINDING_ISSUE')
    const textareas = wrapper.findAll('textarea')
    await textareas[0]?.setValue('Preview output mismatch.')
    await wrapper.find('input[maxlength="64"]').setValue('PREVIEW_DIFF')
    await flushPromises()

    expect(
      wrapper
        .findAll('button')
        .find((button) => button.text().includes('Submit decision'))
        ?.attributes('disabled'),
    ).toBeUndefined()
  })

  it('requires approval rationale and key evidence before enabling submit', async () => {
    const wrapper = mountDialog('approval-approve')
    await flushPromises()

    const submitButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Submit decision'))
    expect(submitButton?.attributes('disabled')).toBeDefined()

    await wrapper.find('textarea').setValue('Approved after evidence review.')
    const checkbox = wrapper.findComponent({ name: 'ElCheckbox' })
    await checkbox.setValue(true)
    await flushPromises()

    expect(
      wrapper
        .findAll('button')
        .find((button) => button.text().includes('Submit decision'))
        ?.attributes('disabled'),
    ).toBeUndefined()
  })

  it('keeps reject submit disabled until remediation link is provided', async () => {
    const wrapper = mountDialog('approval-reject')
    await flushPromises()

    const submitButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Submit decision'))
    expect(submitButton?.attributes('disabled')).toBeDefined()

    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    await selects[0]?.setValue('BINDING_ISSUE')
    const textareas = wrapper.findAll('textarea')
    await textareas[0]?.setValue('Header binding invalid.')
    await wrapper.find('input[maxlength="64"]').setValue('ANCHOR_INTEGRITY')
    await flushPromises()

    expect(
      wrapper
        .findAll('button')
        .find((button) => button.text().includes('Submit decision'))
        ?.attributes('disabled'),
    ).toBeUndefined()
  })
})
