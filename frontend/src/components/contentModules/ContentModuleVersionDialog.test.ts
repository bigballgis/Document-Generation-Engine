import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentModuleVersionDialog from '@/components/contentModules/ContentModuleVersionDialog.vue'
import en from '@/i18n/locales/en'
import * as contentModulesApi from '@/api/contentModules'

vi.mock('@/api/contentModules', () => ({
  listContentModules: vi.fn(),
  getContentModule: vi.fn(),
  createContentModule: vi.fn(),
  updateContentModuleSharedGroupCodes: vi.fn(),
  createContentModuleVersion: vi.fn(),
  updateContentModuleDraftVersion: vi.fn(),
  transitionContentModuleReview: vi.fn(),
  previewContentModuleLifecycleImpact: vi.fn(),
  applyContentModuleLifecycleOperation: vi.fn(),
}))

describe('ContentModuleVersionDialog', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(contentModulesApi.updateContentModuleDraftVersion).mockReset()
  })

  it('loads legacy block content into the structured editor on edit', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleVersionDialog, {
      props: {
        modelValue: true,
        moduleId: 'MOD-LOAN-DISCLOSURE',
        mode: 'edit',
        version: {
          versionId: 'v1',
          semanticVersion: '1.0.0',
          reviewState: 'DRAFT',
          contentStructureJson: '{"blocks":[{"type":"paragraph","text":"Existing clause"}]}',
          createdAt: '2026-06-26T10:00:00Z',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="controlled-structured-content-editor"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Existing clause')
  })

  it('uses the default empty v1 structure for create mode', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleVersionDialog, {
      props: {
        modelValue: true,
        moduleId: 'MOD-LOAN-DISCLOSURE',
        mode: 'create',
        version: null,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="controlled-structured-content-editor"]').exists()).toBe(true)
  })
})
