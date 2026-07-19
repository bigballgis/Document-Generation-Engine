import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import en from '@/i18n/locales/en'
import TemplateApprovalMatrixModeField from '@/components/templates/TemplateApprovalMatrixModeField.vue'

describe('TemplateApprovalMatrixModeField (BDD-IBL-E3-002/015)', () => {
  it('emits mode changes when writable', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateApprovalMatrixModeField, {
      props: { modelValue: 'SINGLE_TRACK' },
      global: { plugins: [i18n, ElementPlus] },
    })
    expect(wrapper.find('[data-testid="approval-matrix-mode-field"]').exists()).toBe(true)
    await wrapper.vm.$emit('update:modelValue', 'LEGAL_THEN_COMPLIANCE')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['LEGAL_THEN_COMPLIANCE'])
  })
})
