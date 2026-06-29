import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentModuleDetailView from '@/views/contentModules/ContentModuleDetailView.vue'
import en from '@/i18n/locales/en'
import * as contentModulesApi from '@/api/contentModules'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/contentModules', () => ({
  listContentModules: vi.fn(),
  getContentModule: vi.fn(),
  createContentModule: vi.fn(),
  createContentModuleVersion: vi.fn(),
  updateContentModuleDraftVersion: vi.fn(),
  transitionContentModuleReview: vi.fn(),
  previewContentModuleLifecycleImpact: vi.fn(),
  applyContentModuleLifecycleOperation: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { moduleId: 'MOD-LOAN-DISCLOSURE' } }),
  useRouter: () => ({ push: vi.fn() }),
}))

function patchSession(roles: string[]) {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000005',
      displayName: 'Actor',
      email: 'actor@example.com',
      authSource: 'LOCAL',
      roles,
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: 'route.dashboard-home',
      visibleRoutes: ['route.dashboard-home', 'route.content-module-management'],
      expiresAt: '2099-01-01T00:00:00Z',
    },
  })
}

describe('ContentModuleDetailView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.mocked(contentModulesApi.getContentModule).mockReset()
  })

  it('renders module versions and review actions for approver', async () => {
    vi.mocked(contentModulesApi.getContentModule).mockResolvedValue({
      moduleId: 'MOD-LOAN-DISCLOSURE',
      moduleCode: 'MOD-LOAN-DISCLOSURE',
      groupCode: 'RETAIL',
      name: 'Loan disclosure',
      description: 'Standard clauses',
      versions: [
        {
          versionId: 'v1',
          semanticVersion: '1.0.0',
          reviewState: 'SUBMITTED',
          lifecycleState: 'ACTIVE',
          changeDescription: 'Ready for review',
          createdAt: '2026-06-26T10:00:00Z',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    patchSession(['TEMPLATE_APPROVER'])
    await flushPromises()

    expect(wrapper.text()).toContain('Loan disclosure')
    expect(wrapper.text()).toContain('1.0.0')
    expect(wrapper.text()).toContain('Approve')
    expect(wrapper.text()).toContain('Reject')
  })

  it('shows lifecycle actions for group admin when approved active version exists', async () => {
    vi.mocked(contentModulesApi.getContentModule).mockResolvedValue({
      moduleId: 'MOD-LOAN-DISCLOSURE',
      moduleCode: 'MOD-LOAN-DISCLOSURE',
      groupCode: 'RETAIL',
      name: 'Loan disclosure',
      versions: [
        {
          versionId: 'v1',
          semanticVersion: '1.0.0',
          reviewState: 'APPROVED',
          lifecycleState: 'ACTIVE',
          createdAt: '2026-06-26T10:00:00Z',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    patchSession(['GROUP_ADMIN'])
    await flushPromises()

    expect(wrapper.text()).toContain('Stop module')
  })
})
