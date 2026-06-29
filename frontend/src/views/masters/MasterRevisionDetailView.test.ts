import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MasterRevisionDetailView from '@/views/masters/MasterRevisionDetailView.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'

vi.mock('@/api/masters', () => ({
  getMaster: vi.fn(),
  getMasterRevisionLine: vi.fn(),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { masterId: 'master-1', revisionLineId: 'revision-1' } }),
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/auth/roles', () => ({
  canReviewMasters: () => false,
  sessionContext: () => ({}),
}))

describe('MasterRevisionDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(mastersApi.getMaster).mockReset()
    vi.mocked(mastersApi.getMasterRevisionLine).mockReset()
  })

  it('renders revision overview and hides workflow actions for non-current line', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: null,
      status: 'APPROVED',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterRevisionLine).mockResolvedValue({
      id: 'revision-1',
      masterId: 'master-1',
      lineLabel: 'CURRENT',
      status: 'APPROVED',
      originalFilename: 'letterhead-v2.docx',
      changeSummary: 'Updated header',
      current: false,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block' }],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T11:00:00Z',
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterRevisionDetailView, {
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Revision overview')
    expect(wrapper.text()).toContain('Anchor catalog')
    expect(wrapper.text()).not.toContain('Submit for review')
  })
})
