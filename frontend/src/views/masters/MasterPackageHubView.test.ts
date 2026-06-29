import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MasterPackageHubView from '@/views/masters/MasterPackageHubView.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'

vi.mock('@/api/masters', () => ({
  listMasters: vi.fn(),
  getMaster: vi.fn(),
  getMasterImpactAnalysis: vi.fn(),
  listMasterRevisionLines: vi.fn(),
  downloadMasterFile: vi.fn(),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { masterId: 'master-1' } }),
  useRouter: () => ({ push: routerPush }),
}))

describe('MasterPackageHubView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    vi.mocked(mastersApi.getMaster).mockReset()
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockReset()
    vi.mocked(mastersApi.listMasterRevisionLines).mockReset()
  })

  it('renders package header and revision lines panel', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: 'Header master',
      status: 'DRAFT',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockResolvedValue({
      masterId: 'master-1',
      referencedTemplateIds: [],
      retestRequired: false,
    })
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'revision-1',
          lineLabel: 'CURRENT',
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000001',
          current: true,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterPackageHubView, {
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Retail letterhead')
    expect(wrapper.text()).toContain('Revision lines')
    expect(wrapper.text()).toContain('Current revision')
    expect(wrapper.text()).toContain('Impact analysis')
  })
})
