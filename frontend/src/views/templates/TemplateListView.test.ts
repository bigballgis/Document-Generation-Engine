import { mount, flushPromises } from '@vue/test-utils'
import { computed, ref } from 'vue'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateListView from '@/views/templates/TemplateListView.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ManagementSession } from '@/types/session'

const routerPush = vi.fn()
const authorTemplates = ref(true)
const exportTemplates = ref(true)
const decideApprovals = ref(false)

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
  importTemplate: vi.fn(),
}))

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    context: computed(() => ({ roles: ['GLOBAL_ADMIN'] })),
    authorTemplates,
    exportTemplates,
    decideTests: ref(false),
    decideApprovals,
    publishTemplates: ref(false),
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

describe('TemplateListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    authorTemplates.value = true
    exportTemplates.value = true
    decideApprovals.value = false
    vi.mocked(templatesApi.listTemplates).mockReset()
  })

  it('renders templates in a flat table with group column', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [
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

    const wrapper = mount(TemplateListView, {
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Retail letter')
    expect(wrapper.text()).toContain('RETAIL')
    expect(wrapper.text()).toContain('Group')
    expect(wrapper.text()).not.toContain('Group: RETAIL')
    expect(wrapper.find('.group-section').exists()).toBe(false)
    expect(wrapper.findAll('.el-table').length).toBe(1)
  })

  it('shows updatedBy display name when API provides it', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [
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
          updatedByDisplayName: 'Alice Author',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Alice Author')
    expect(wrapper.text()).not.toContain('10000001')
  })

  it('renders template name as navigable link when route is visible', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [
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
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    const session: ManagementSession = {
      username: '10000000',
      displayName: 'Admin',
      email: 'admin@example.com',
      authSource: 'LOCAL',
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      defaultRoute: ROUTE_KEYS.templateManagement,
      visibleRoutes: [ROUTE_KEYS.templateManagement],
      expiresAt: new Date().toISOString(),
    }
    sessionStore.$patch({ accessToken: 'token', session })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })

    await flushPromises()

    const link = wrapper.find('.entity-link-cell__link')
    expect(link.exists()).toBe(true)
    expect(link.text()).toBe('Retail letter')
  })

  it('shows import action for export-capable users', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

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
    expect(wrapper.text()).toContain('Import template')
  })

  it('LR-C9-B: empty catalog shows create CTA when authorTemplates is true', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    authorTemplates.value = true

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No template packages yet.')
    const emptyActions = wrapper.find('[data-testid="empty-state-actions"]')
    expect(emptyActions.exists()).toBe(true)
    expect(emptyActions.text()).toContain('New template package')
  })

  it('LR-C9-B: empty catalog hides create CTA when authorTemplates is false', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    authorTemplates.value = false
    exportTemplates.value = false

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No template packages yet.')
    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('New template package')
  })

  it('LR-C9-A: load failure shows LoadErrorPanel and retry reloads list', async () => {
    vi.mocked(templatesApi.listTemplates)
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce({
        content: [
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
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    const errorPanel = wrapper.findComponent({ name: 'LoadErrorPanel' })
    expect(errorPanel.exists()).toBe(true)
    await errorPanel.vm.$emit('retry')
    await flushPromises()

    expect(templatesApi.listTemplates).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Retail letter')
    expect(wrapper.findComponent({ name: 'LoadErrorPanel' }).exists()).toBe(false)
  })

  it('LR-C5: initial load requests page 0 size 20 with groupCodeAsc sort', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    mount(TemplateListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    expect(templatesApi.listTemplates).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ sort: 'groupCodeAsc' }),
    )
  })

  it('LR-C5: workflow chip maps to lifecycleStatus and approvalSubState on server', async () => {
    decideApprovals.value = true
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'TPL-RETAIL-LETTER',
          groupCode: 'RETAIL',
          name: 'Retail letter',
          lifecycleStatus: 'APPROVAL',
          approvalSubState: 'PENDING_DECISION',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: '10000001',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(TemplateListView, {
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    const tags = wrapper.findAllComponents({ name: 'ElCheckTag' })
    const approvalTag = tags.find((tag) => tag.text().includes('Awaiting my approval'))
    expect(approvalTag).toBeTruthy()
    await approvalTag!.vm.$emit('change', true)
    await flushPromises()

    expect(templatesApi.listTemplates).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
        sort: 'groupCodeAsc',
      }),
    )
  })
})
