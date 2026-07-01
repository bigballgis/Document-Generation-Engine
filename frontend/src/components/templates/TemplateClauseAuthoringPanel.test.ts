import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, describe, expect, it, vi } from 'vitest'
import TemplateClauseAuthoringPanel from '@/components/templates/TemplateClauseAuthoringPanel.vue'
import en from '@/i18n/locales/en'
import * as contentModulesApi from '@/api/contentModules'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', () => ({
  listTemplateContentModuleReferences: vi.fn(),
  upsertTemplateContentModuleReference: vi.fn(),
}))

vi.mock('@/api/contentModules', () => ({
  listContentModules: vi.fn(),
  getContentModule: vi.fn(),
}))

describe('TemplateClauseAuthoringPanel', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockReset()
    vi.mocked(templatesApi.upsertTemplateContentModuleReference).mockReset()
    vi.mocked(contentModulesApi.listContentModules).mockReset()
    vi.mocked(contentModulesApi.getContentModule).mockReset()
  })

  it('renders clause references with resolved module names', async () => {
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockResolvedValue([
      {
        referenceKey: 'LOAN_DISCLOSURE',
        moduleId: 'MOD-LOAN-DISCLOSURE',
        semanticVersion: '1.0.0',
        locked: false,
      },
    ])
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue([
      {
        moduleId: 'MOD-LOAN-DISCLOSURE',
        moduleCode: 'MOD-LOAN-DISCLOSURE',
        groupCode: 'RETAIL',
        name: 'Loan disclosure',
        createdAt: '2026-06-26T10:00:00Z',
        updatedAt: '2026-06-26T10:00:00Z',
      },
    ])

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    setActivePinia(createPinia())
    const wrapper = mount(TemplateClauseAuthoringPanel, {
      props: {
        templateId: 'tpl-1',
        groupCode: 'RETAIL',
        editable: false,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('LOAN_DISCLOSURE')
    expect(wrapper.text()).toContain('Loan disclosure')
    expect(wrapper.text()).toContain('Preview')
  })

  it('shows add reference control when editable', async () => {
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockResolvedValue([])
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue([])

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    setActivePinia(createPinia())
    const wrapper = mount(TemplateClauseAuthoringPanel, {
      props: {
        templateId: 'tpl-1',
        groupCode: 'RETAIL',
        editable: true,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Add reference')
  })
})
