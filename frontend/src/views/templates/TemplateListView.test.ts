import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateListView from '@/views/templates/TemplateListView.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'

const routerPush = vi.fn()

vi.mock('@/api/templates', () => ({
  listTemplates: vi.fn(),
  getTemplate: vi.fn(),
  submitForTest: vi.fn(),
  recordTestDecision: vi.fn(),
  submitForApproval: vi.fn(),
  recordApprovalDecision: vi.fn(),
  publishTemplate: vi.fn(),
  testGenerate: vi.fn(),
  getPreview: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

describe('TemplateListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    vi.mocked(templatesApi.listTemplates).mockReset()
  })

  it('renders grouped templates after load', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue([
      {
        id: 'tpl-1',
        externalId: 'TPL-RETAIL-LETTER',
        groupCode: 'RETAIL',
        name: 'Retail letter',
        lifecycleStatus: 'DRAFT',
        releaseVersion: null,
        releaseVersionCount: 0,
        masterId: 'master-1',
        updatedBy: '10000001',
        updatedAt: '2026-06-23T10:00:00Z',
      },
    ])

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplateListView, {
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Retail letter')
    expect(wrapper.text()).toContain('RETAIL')
  })
})
