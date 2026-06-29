import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it, vi } from 'vitest'
import TemplateContentModuleReferencesPanel from '@/components/templates/TemplateContentModuleReferencesPanel.vue'
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

describe('TemplateContentModuleReferencesPanel', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockReset()
    vi.mocked(templatesApi.upsertTemplateContentModuleReference).mockReset()
    vi.mocked(contentModulesApi.listContentModules).mockReset()
    vi.mocked(contentModulesApi.getContentModule).mockReset()
  })

  it('renders content module references from the API', async () => {
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockResolvedValue([
      {
        referenceKey: 'LOAN_DISCLOSURE',
        moduleId: 'MOD-LOAN-DISCLOSURE',
        semanticVersion: '1.0.0',
        locked: false,
      },
    ])

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateContentModuleReferencesPanel, {
      props: {
        templateId: 'tpl-1',
        groupCode: 'RETAIL',
        editable: false,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(templatesApi.listTemplateContentModuleReferences).toHaveBeenCalledWith('tpl-1')
    expect(wrapper.text()).toContain('LOAN_DISCLOSURE')
    expect(wrapper.text()).toContain('MOD-LOAN-DISCLOSURE')
    expect(wrapper.text()).toContain('1.0.0')
  })

  it('shows add reference control when editable', async () => {
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockResolvedValue([])
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
    const wrapper = mount(TemplateContentModuleReferencesPanel, {
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

  it('requests modules for the template group when opening the picker', async () => {
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockResolvedValue([])
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue([
      {
        moduleId: 'MOD-RETAIL-DISCLOSURE',
        moduleCode: 'MOD-RETAIL-DISCLOSURE',
        groupCode: 'RETAIL',
        name: 'Owned module',
        createdAt: '2026-06-26T10:00:00Z',
        updatedAt: '2026-06-26T10:00:00Z',
      },
      {
        moduleId: 'MOD-CORP-TERMS',
        moduleCode: 'MOD-CORP-TERMS',
        groupCode: 'CORP',
        name: 'Shared corp terms',
        sharedGroupCodes: ['RETAIL'],
        createdAt: '2026-06-26T10:00:00Z',
        updatedAt: '2026-06-26T10:00:00Z',
      },
    ])

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateContentModuleReferencesPanel, {
      props: {
        templateId: 'tpl-1',
        groupCode: 'RETAIL',
        editable: true,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    await wrapper.get('.panel-header .el-button--primary').trigger('click')
    await flushPromises()

    expect(contentModulesApi.listContentModules).toHaveBeenCalledWith('RETAIL')
    expect(vi.mocked(contentModulesApi.listContentModules).mock.calls).toHaveLength(1)
  })
})
