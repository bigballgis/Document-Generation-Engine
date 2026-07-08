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
    authorTemplates: ref(true),
    exportTemplates: ref(true),
    decideTests: ref(false),
    decideApprovals: ref(false),
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
})
