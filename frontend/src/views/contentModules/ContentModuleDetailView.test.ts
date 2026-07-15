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
  listContentModuleWorkflowTasks: vi.fn(),
  getContentModule: vi.fn(),
  createContentModule: vi.fn(),
  updateContentModuleSharedGroupCodes: vi.fn(),
  createContentModuleVersion: vi.fn(),
  updateContentModuleDraftVersion: vi.fn(),
  transitionContentModuleReview: vi.fn(),
  previewContentModuleLifecycleImpact: vi.fn(),
  applyContentModuleLifecycleOperation: vi.fn(),
}))

const routerPush = vi.fn()
const routerReplace = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { moduleId: 'MOD-LOAN-DISCLOSURE' }, query: {} }),
  useRouter: () => ({ push: routerPush, replace: routerReplace }),
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
      reviewHistory: [],
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
    await wrapper.find('.workspace-tab-shell').findAll('.el-tabs__item')[2].trigger('click')
    expect(wrapper.find('.workspace-tab-shell__actions').text()).toContain('Approve')
    expect(wrapper.find('.workspace-tab-shell__actions').text()).toContain('Reject')
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
      reviewHistory: [],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    patchSession(['GROUP_ADMIN'])
    await flushPromises()

    await wrapper.find('.workspace-tab-shell').findAll('.el-tabs__item')[2].trigger('click')
    expect(wrapper.find('.workspace-tab-shell__actions').text()).toContain('Stop module')
  })

  it('shows rejection reason column and review timeline on detail workspace', async () => {
    vi.mocked(contentModulesApi.getContentModule).mockResolvedValue({
      moduleId: 'MOD-LOAN-DISCLOSURE',
      moduleCode: 'MOD-LOAN-DISCLOSURE',
      groupCode: 'RETAIL',
      name: 'Loan disclosure',
      versions: [
        {
          versionId: 'v1',
          semanticVersion: '1.0.0',
          reviewState: 'DRAFT',
          lifecycleState: 'ACTIVE',
          changeDescription: 'Rework wording',
          rejectionReason: 'Wording not acceptable',
          createdAt: '2026-06-26T10:00:00Z',
          updatedAt: '2026-06-26T12:00:00Z',
        },
      ],
      reviewHistory: [
        {
          action: 'SUBMITTED',
          changeSummary: 'Ready for review',
          actorUsername: '10000003',
          createdAt: '2026-06-26T10:00:00Z',
          semanticVersion: '1.0.0',
        },
        {
          action: 'REJECTED',
          commentSummary: 'Wording not acceptable',
          actorUsername: '10000005',
          createdAt: '2026-06-26T11:00:00Z',
          semanticVersion: '1.0.0',
        },
      ],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    patchSession(['TEMPLATE_AUTHOR'])
    await flushPromises()

    expect(wrapper.text()).toContain('Wording not acceptable')
    await wrapper.find('.workspace-tab-shell').findAll('.el-tabs__item')[2].trigger('click')
    await flushPromises()
    expect(wrapper.find('.el-timeline').exists()).toBe(true)
    expect(wrapper.text()).toContain('Submitted for review')
    expect(wrapper.text()).toContain('Rejected')
    expect(wrapper.text()).toContain('By 10000005')
  })

  it('shows empty review history state when no transitions exist', async () => {
    vi.mocked(contentModulesApi.getContentModule).mockResolvedValue({
      moduleId: 'MOD-LOAN-DISCLOSURE',
      moduleCode: 'MOD-LOAN-DISCLOSURE',
      groupCode: 'RETAIL',
      name: 'Loan disclosure',
      versions: [
        {
          versionId: 'v1',
          semanticVersion: '1.0.0',
          reviewState: 'DRAFT',
          createdAt: '2026-06-26T10:00:00Z',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ],
      reviewHistory: [],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    patchSession(['TEMPLATE_AUTHOR'])
    await flushPromises()
    await wrapper.find('.workspace-tab-shell').findAll('.el-tabs__item')[2].trigger('click')
    await flushPromises()
    expect(wrapper.find('.el-timeline').exists()).toBe(false)
    expect(wrapper.text()).toContain('No review activity yet.')
  })

  it('SGC-003: detail summary shows owner and shared groups', async () => {
    vi.mocked(contentModulesApi.getContentModule).mockResolvedValue({
      moduleId: 'MOD-LOAN-DISCLOSURE',
      moduleCode: 'MOD-LOAN',
      groupCode: 'HQ',
      name: 'Loan disclosure',
      sharedGroupCodes: ['WEALTH', 'RETAIL'],
      versions: [],
      reviewHistory: [],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    patchSession(['TEMPLATE_AUTHOR'])
    await flushPromises()

    expect(wrapper.text()).toContain('Owner: HQ')
    expect(wrapper.text()).toContain('Shared with: RETAIL, WEALTH')
  })

  it('SGC-003: empty shared groups show not-shared copy', async () => {
    vi.mocked(contentModulesApi.getContentModule).mockResolvedValue({
      moduleId: 'MOD-LOAN-DISCLOSURE',
      moduleCode: 'MOD-LOAN',
      groupCode: 'HQ',
      name: 'Loan disclosure',
      sharedGroupCodes: [],
      versions: [],
      reviewHistory: [],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    patchSession(['TEMPLATE_AUTHOR'])
    await flushPromises()

    expect(wrapper.text()).toContain('Not shared outside owner group')
  })

  it('SGC-006: settings entry hidden without configure capability', async () => {
    vi.mocked(contentModulesApi.getContentModule).mockResolvedValue({
      moduleId: 'MOD-LOAN-DISCLOSURE',
      moduleCode: 'MOD-LOAN',
      groupCode: 'HQ',
      name: 'Loan disclosure',
      sharedGroupCodes: ['RETAIL'],
      versions: [],
      reviewHistory: [],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    patchSession(['TEMPLATE_AUTHOR'])
    await flushPromises()

    expect(wrapper.find('[data-testid="content-module-settings-open"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Shared with: RETAIL')
  })

  it('SGC-006: GROUP_ADMIN sees settings entry', async () => {
    vi.mocked(contentModulesApi.getContentModule).mockResolvedValue({
      moduleId: 'MOD-LOAN-DISCLOSURE',
      moduleCode: 'MOD-LOAN',
      groupCode: 'HQ',
      name: 'Loan disclosure',
      sharedGroupCodes: ['RETAIL'],
      versions: [],
      reviewHistory: [],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleDetailView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    patchSession(['GROUP_ADMIN'])
    await flushPromises()

    expect(wrapper.find('[data-testid="content-module-settings-open"]').exists()).toBe(true)
  })
})
