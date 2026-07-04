import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateExportActions from '@/components/templates/TemplateExportActions.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', () => ({
  exportTemplateJson: vi.fn(),
  exportTemplateZip: vi.fn(),
}))

vi.mock('@/utils/downloadExport', () => ({
  downloadJsonExport: vi.fn(),
  downloadBlobExport: vi.fn(),
}))

describe('TemplateExportActions', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.exportTemplateJson).mockReset()
    vi.mocked(templatesApi.exportTemplateZip).mockReset()
  })

  function mountActions() {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(TemplateExportActions, {
      props: {
        templateId: 'tpl-1',
        externalId: 'TPL-EXPORT',
      },
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
      },
    })
  }

  it('renders export action label', () => {
    const wrapper = mountActions()
    expect(wrapper.text()).toContain('Export bundle')
  })

  it('downloads JSON export when command is triggered', async () => {
    const { downloadJsonExport } = await import('@/utils/downloadExport')
    vi.mocked(templatesApi.exportTemplateJson).mockResolvedValue({
      format: 'template-export-bundle-v1-json',
      bundle: {
        format: 'template-export-bundle-v1-json',
        metadata: {
          templateId: 'tpl-1',
          externalId: 'TPL-EXPORT',
          groupCode: 'RETAIL',
          name: 'Export',
          description: null,
          masterId: 'master-1',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          devVersionId: 'dev-1',
          devVersionNumber: 1,
          exportedAt: '2026-06-26T00:00:00Z',
        },
        variables: [],
        bindings: [],
        rules: [],
        contentModuleReferences: [],
        policySnapshot: undefined,
      },
    })

    const wrapper = mountActions()
    await wrapper.vm.handleExportJson()
    await flushPromises()

    expect(templatesApi.exportTemplateJson).toHaveBeenCalledWith('tpl-1')
    expect(downloadJsonExport).toHaveBeenCalledWith('TPL-EXPORT-export.json', expect.any(Object))
  })
})
