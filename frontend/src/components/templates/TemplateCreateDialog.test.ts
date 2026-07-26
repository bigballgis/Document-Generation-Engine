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
import * as mastersApi from '@/api/masters'

vi.mock('@/api/templates', () => ({
  createTemplate: vi.fn(),
  listTemplates: vi.fn(),
}))

vi.mock('@/api/masters', () => ({
  listAllMasters: vi.fn(),
  listMasters: vi.fn(),
}))

vi.mock('@/api/riskPromptConfig', () => ({
  getGlobalRiskPromptConfig: vi.fn().mockResolvedValue({
    scopeType: 'GLOBAL',
    groupCode: null,
    reasonCategories: ['BINDING_ISSUE', 'FIDELITY_WARNING'],
    riskPromptCopy: {},
    updatedAt: '2026-07-02T00:00:00Z',
  }),
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

const approvedMaster = {
  id: 'master-1',
  groupCode: 'RETAIL',
  name: 'Retail letterhead',
  status: 'APPROVED' as const,
  originalFilename: 'letterhead.docx',
  anchorCount: 1,
  updatedAt: '2026-06-23T10:00:00Z',
  updatedBy: 'tester',
}

describe('TemplateCreateDialog', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.createTemplate).mockReset()
    vi.mocked(mastersApi.listAllMasters).mockReset()
    vi.mocked(mastersApi.listAllMasters).mockResolvedValue({
      content: [approvedMaster],
      truncated: false,
      totalElements: 1,
    })
  })

  function mountDialog(options: { seedMasters?: typeof approvedMaster[] } = {}) {
    const pinia = createPinia()
    setActivePinia(pinia)
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    useMastersStore().$patch({
      masters: options.seedMasters ?? [approvedMaster],
    })

    return mount(TemplateCreateDialog, {
      props: { modelValue: true },
      attachTo: document.body,
      global: {
        plugins: [i18n, ElementPlus, pinia],
        stubs: {
          ScopedGroupSelect: selectStub,
          AppSearchSelect: selectStub,
          TemplateRiskPromptConfigPanel: true,
        },
      },
    })
  }

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('FOS-W2-1: fetches APPROVED letterheads when the dialog opens on an empty store', async () => {
    vi.mocked(mastersApi.listAllMasters).mockResolvedValue({
      content: [approvedMaster],
      truncated: false,
      totalElements: 1,
    })
    mountDialog({ seedMasters: [] })
    await flushPromises()

    expect(mastersApi.listAllMasters).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'APPROVED' }),
    )
    expect(useMastersStore().masters.map((m) => m.id)).toEqual(['master-1'])
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
    expect(rules.locale?.[0]?.required).toBe(true)
  })

  type CreateDialogExposed = {
    form: {
      groupCode: string
      masterId: string
      externalId: string
      name: string
      locale: string
    }
    handleSubmit: () => Promise<void>
  }

  it('IBL-E1-013: blocks create submit when locale is empty', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const vm = wrapper.vm as unknown as CreateDialogExposed
    vm.form.groupCode = 'RETAIL'
    vm.form.masterId = 'master-1'
    vm.form.externalId = 'TPL-NEW'
    vm.form.name = 'New template'
    vm.form.locale = ''

    await vm.handleSubmit()
    await flushPromises()

    expect(templatesApi.createTemplate).not.toHaveBeenCalled()
  })

  it('IBL-E1-013: submits create payload with required locale', async () => {
    vi.mocked(templatesApi.createTemplate).mockResolvedValue({
      id: 'tpl-1',
      externalId: 'TPL-NEW',
      groupCode: 'RETAIL',
      name: 'New template',
      masterId: 'master-1',
      lifecycleStatus: 'DRAFT',
      releaseVersion: null,
      locale: 'en-US',
      devVersionId: 'dev-1',
      devVersionNumber: 1,
      variables: [],
      bindings: [],
      rules: [],
      createdAt: '2026-07-19T10:00:00Z',
      updatedAt: '2026-07-19T10:00:00Z',
    })

    const wrapper = mountDialog()
    await flushPromises()

    const vm = wrapper.vm as unknown as CreateDialogExposed
    vm.form.groupCode = 'RETAIL'
    vm.form.masterId = 'master-1'
    vm.form.externalId = 'TPL-NEW'
    vm.form.name = 'New template'
    vm.form.locale = 'en-US'

    await vm.handleSubmit()
    await flushPromises()

    expect(templatesApi.createTemplate).toHaveBeenCalledWith(
      expect.objectContaining({
        locale: 'en-US',
        externalId: 'TPL-NEW',
      }),
    )
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

    const vm = wrapper.vm as unknown as CreateDialogExposed
    vm.form.groupCode = 'RETAIL'
    vm.form.masterId = 'master-1'
    vm.form.externalId = 'TPL-DUP'
    vm.form.name = 'Duplicate template'
    vm.form.locale = 'zh-CN'

    await vm.handleSubmit()
    await flushPromises()

    expect(templatesStore.lastErrorMessageKey).toBe('api.error.template.externalIdExists')
    expect(wrapper.text()).toContain('A template with this external identifier already exists.')
  })
})
