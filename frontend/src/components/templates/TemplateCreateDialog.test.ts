import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateCreateDialog from '@/components/templates/TemplateCreateDialog.vue'
import en from '@/i18n/locales/en'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', () => ({
  createTemplate: vi.fn(),
  listTemplates: vi.fn(),
}))

vi.mock('@/composables/useScopedGroupOptions', () => ({
  useScopedGroupOptions: () => ({
    resolveDefaultGroupCode: () => 'RETAIL',
    ensureGroupCatalog: vi.fn().mockResolvedValue(undefined),
    groupOptions: [{ value: 'RETAIL', label: 'RETAIL' }],
    isGroupLocked: { value: false },
    lockedGroupCode: { value: null },
  }),
}))

const selectStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template:
    '<input class="select-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

describe('TemplateCreateDialog', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.createTemplate).mockReset()
  })

  function mountDialog() {
    const pinia = createPinia()
    setActivePinia(pinia)
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    useMastersStore().$patch({
      masters: [
        {
          id: 'master-1',
          groupCode: 'RETAIL',
          name: 'Retail letterhead',
          status: 'APPROVED',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
    })

    return mount(TemplateCreateDialog, {
      props: { modelValue: true },
      attachTo: document.body,
      global: {
        plugins: [i18n, ElementPlus, pinia],
        stubs: {
          ScopedGroupSelect: selectStub,
          AppSearchSelect: selectStub,
        },
      },
    })
  }

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('wires el-form validation rules for required create fields', async () => {
    const wrapper = mountDialog()
    await flushPromises()
    const form = wrapper.findComponent({ name: 'ElForm' })
    const rules = form.props('rules') as Record<string, Array<{ required?: boolean }>>

    expect(rules.groupCode?.[0]?.required).toBe(true)
    expect(rules.masterId?.[0]?.required).toBe(true)
    expect(rules.externalId?.[0]?.required).toBe(true)
    expect(rules.name?.[0]?.required).toBe(true)
  })

  it('surfaces API error message keys in the dialog', async () => {
    vi.mocked(templatesApi.createTemplate).mockRejectedValue(
      axiosEnvelopeError(
        422,
        'api.error.template.externalIdExists',
        {
          code: 'TEMPLATE_VALIDATION_FAILED',
          category: 'TEMPLATE',
          message: 'External ID already exists.',
        },
      ),
    )

    const wrapper = mountDialog()
    const templatesStore = useTemplatesStore()
    await flushPromises()

    const selects = wrapper.findAll('.select-stub')
    await selects[0]?.setValue('RETAIL')
    await selects[1]?.setValue('master-1')
    await wrapper.find('input[placeholder="e.g. TPL-RETAIL-LETTER"]').setValue('TPL-DUP')
    const nameInput = wrapper.findAll('.el-input__inner')[1]
    await nameInput.setValue('Duplicate template')

    await wrapper.find('.el-button--primary').trigger('click')
    await flushPromises()

    expect(templatesStore.lastErrorMessageKey).toBe('api.error.template.externalIdExists')
    expect(wrapper.text()).toContain('Unable to create template.')
  })
})
