import { mount, flushPromises } from '@vue/test-utils'
import { computed, ref } from 'vue'
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
const manageMasters = ref(true)

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    context: computed(() => ({ roles: ['MASTER_DESIGNER'] })),
    manageMasters,
    reviewMasters: ref(false),
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

describe('MasterListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    manageMasters.value = true
    vi.mocked(mastersApi.listMasters).mockReset()
  })

  it('renders masters in a flat table with group column', { timeout: 20000 }, async () => {
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

  it('shows updatedBy display name when API provides it', { timeout: 20000 }, async () => {
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

  it('LR-C9-A: retry after load failure reloads masters without remount', async () => {
    vi.mocked(mastersApi.listMasters)
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce([
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

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(MasterListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    await wrapper.findComponent({ name: 'LoadErrorPanel' }).vm.$emit('retry')
    await flushPromises()

    expect(mastersApi.listMasters).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Retail letterhead')
  })

  it('LR-C9-B: empty catalog shows upload CTA when manageMasters is true', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue([])
    manageMasters.value = true

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(MasterListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    const emptyActions = wrapper.find('[data-testid="empty-state-actions"]')
    expect(emptyActions.exists()).toBe(true)
    expect(emptyActions.text()).toContain('New letterhead package')
  })

  it('LR-C9-B: empty catalog hides upload CTA when manageMasters is false', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue([])
    manageMasters.value = false

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(MasterListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('No letterhead packages yet.')
  })
})
