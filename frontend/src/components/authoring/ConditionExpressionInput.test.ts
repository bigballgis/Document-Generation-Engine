import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import ConditionExpressionInput from '@/components/authoring/ConditionExpressionInput.vue'
import en from '@/i18n/locales/en'

vi.mock('vue-i18n', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-i18n')>()
  return {
    ...actual,
  }
})

function mountInput(props?: Partial<{ modelValue: string; variableKeys: string[]; readonly: boolean }>) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(ConditionExpressionInput, {
    props: {
      modelValue: props?.modelValue ?? '',
      variableKeys: props?.variableKeys ?? ['borrowerLegalName', 'showNotice'],
      readonly: props?.readonly ?? false,
    },
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('ConditionExpressionInput', () => {
  it('opens schema suggestions from Insert variable and inserts ${key}', async () => {
    const wrapper = mountInput()
    await wrapper.get('[data-testid="insert-variable-button"]').trigger('click')
    expect(wrapper.find('[data-testid="variable-autocomplete-list"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="variable-suggestion-borrowerLegalName"]').exists()).toBe(true)
    await wrapper.get('[data-testid="variable-suggestion-borrowerLegalName"]').trigger('click')
    expect(wrapper.emitted('update:modelValue')?.at(-1)?.[0]).toBe('${borrowerLegalName}')
  })

  it('opens suggestions when typing ${ prefix', async () => {
    const wrapper = mountInput({ modelValue: '' })
    const input = wrapper.get('[data-testid="condition-expression-input"]')
    await input.setValue('${sho')
    await wrapper.vm.$nextTick()
    // allow onInput nextTick
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(wrapper.find('[data-testid="variable-autocomplete-list"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="variable-suggestion-showNotice"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="variable-suggestion-borrowerLegalName"]').exists()).toBe(false)
  })

  it('hides insert control when readonly', () => {
    const wrapper = mountInput({ readonly: true })
    expect(wrapper.find('[data-testid="insert-variable-button"]').exists()).toBe(false)
  })
})
