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

  it('renders masters in a flat table with group column', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue([
      {
        id: 'master-1',
        groupCode: 'RETAIL',
        name: 'Retail letterhead',
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
        updatedBy: '10000001',
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
    expect(wrapper.text()).toContain('Group')
    expect(wrapper.text()).not.toContain('Group: RETAIL')
    expect(wrapper.find('.group-section').exists()).toBe(false)
    expect(wrapper.findAll('.el-table').length).toBe(1)
  })

  it('shows updatedBy display name when API provides it', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue([
      {
        id: 'master-1',
        groupCode: 'RETAIL',
        name: 'Retail letterhead',
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
        updatedBy: '10000001',
        updatedByDisplayName: 'Bob Builder',
        updatedAt: '2026-06-23T10:00:00Z',
      },
    ])

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(MasterListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Bob Builder')
    expect(wrapper.text()).not.toContain('10000001')
  })

  it('shows load error with retry when list fails', async () => {
    vi.mocked(mastersApi.listMasters).mockRejectedValue(new Error('network'))

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

    expect(wrapper.text()).toContain('Unable to load letterheads')
    expect(wrapper.text()).toContain('Retry')
  })
})
