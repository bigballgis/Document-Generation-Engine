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

vi.mock('@/composables/useScopedGroupOptions', () => ({
  useScopedGroupOptions: () => ({
    resolveDefaultGroupCode: () => 'RETAIL',
    ensureGroupCatalog: vi.fn().mockResolvedValue(undefined),
    groupOptions: [{ value: 'RETAIL', label: 'RETAIL' }],
    isGroupLocked: { value: false },
    lockedGroupCode: { value: null },
  }),
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

function pageView<T>(content: T[], totalElements = content.length) {
  return {
    content,
    page: 0,
    size: 20,
    totalElements,
    totalPages: totalElements === 0 ? 0 : Math.ceil(totalElements / 20),
  }
}

describe('MasterListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    manageMasters.value = true
    vi.mocked(mastersApi.listMasters).mockReset()
    vi.mocked(mastersApi.createMaster).mockReset()
  })

  it('renders masters in a flat table with group column', { timeout: 20000 }, async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue(
      pageView([
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
      ]),
    )

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
    vi.mocked(mastersApi.listMasters).mockResolvedValue(
      pageView([
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
      ]),
    )

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
      .mockResolvedValueOnce(
        pageView([
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
        ]),
      )

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
    vi.mocked(mastersApi.listMasters).mockResolvedValue(pageView([]))
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
    vi.mocked(mastersApi.listMasters).mockResolvedValue(pageView([]))
    manageMasters.value = false

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(MasterListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('No letterhead packages yet.')
  })

  it('LR-C10: upload failure keeps list visible and hides LoadErrorPanel while dialog open', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue(
      pageView([
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
      ]),
    )
    vi.mocked(mastersApi.createMaster).mockRejectedValue(new Error('upload failed'))

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(MasterListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Retail letterhead')

    const openUpload = wrapper.findAll('button').find((button) =>
      button.text().includes('New letterhead package'),
    )
    expect(openUpload).toBeTruthy()
    await openUpload!.trigger('click')
    await flushPromises()

    const dialog = wrapper.findComponent({ name: 'MasterUploadDialog' })
    expect(dialog.exists()).toBe(true)
    await dialog.vm.$emit('submit', {
      groupCode: 'RETAIL',
      name: 'New master',
      description: '',
      file: new File([new Uint8Array(8)], 'new.docx'),
    })
    await flushPromises()

    expect(wrapper.findComponent({ name: 'LoadErrorPanel' }).exists()).toBe(false)
    expect(wrapper.text()).toContain('Retail letterhead')
    expect(dialog.props('serverErrorKey')).toBeTruthy()
  })

  it('LR-C5: requests server page with default size and group-first sort', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue(pageView([]))

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    mount(MasterListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    expect(mastersApi.listMasters).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ sort: 'groupCodeAsc' }),
    )
  })

  it('LR-C5: group filter triggers server request with groupCode and resets to page 0', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue(
      pageView(
        [
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
        ],
        25,
      ),
    )

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(MasterListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    vi.mocked(mastersApi.listMasters).mockResolvedValue(pageView([]))
    const groupFilter = wrapper.find('.catalog-filter-toolbar__control input')
    await groupFilter.setValue('RETAIL')
    await flushPromises()

    expect(mastersApi.listMasters).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ groupCode: 'RETAIL', sort: 'groupCodeAsc' }),
    )
  })
})
