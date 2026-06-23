import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MasterListView from '@/views/masters/MasterListView.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'

vi.mock('@/api/masters', () => ({
  listMasters: vi.fn(),
  getMaster: vi.fn(),
  createMaster: vi.fn(),
  submitMasterReview: vi.fn(),
  decideMasterReview: vi.fn(),
  getMasterImpactAnalysis: vi.fn(),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

describe('MasterListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    vi.mocked(mastersApi.listMasters).mockReset()
  })

  it('renders grouped masters after load', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue([
      {
        id: 'master-1',
        groupCode: 'RETAIL',
        name: 'Retail letterhead',
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
        updatedAt: '2026-06-23T10:00:00Z',
      },
    ])

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterListView, {
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Retail letterhead')
    expect(wrapper.text()).toContain('RETAIL')
  })
})
