import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ApiPolicyHomeView from '@/views/api/ApiPolicyHomeView.vue'
import * as apiPolicyApi from '@/api/apiPolicy'
import * as templatesApi from '@/api/templates'
import en from '@/i18n/locales/en'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ManagementSession } from '@/types/session'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/api/apiPolicy', () => ({
  fetchAlerts: vi.fn(),
  fetchReadinessSummary: vi.fn(),
  listInvocations: vi.fn(),
}))

vi.mock('@/api/templates', () => ({
  listAllTemplates: vi.fn(),
  listTemplates: vi.fn(),
}))

const sampleAlerts = [
  {
    alertKind: 'MISSING_AD_GROUP' as const,
    templateId: 'tpl-1',
    templateName: 'Retail account open',
    templateExternalId: 'RETAIL-ACCOUNT-OPEN',
    groupCode: 'RETAIL',
  },
  {
    alertKind: 'EXPIRING_CREDENTIAL' as const,
    templateId: 'tpl-2',
    templateName: 'Mortgage approval',
    templateExternalId: 'MORTGAGE-APPROVAL',
    groupCode: 'MORTGAGE',
    credentialExternalId: 'EXT-001',
    credentialExpiresAt: '2026-07-20T00:00:00Z',
  },
]

const sampleSummary = {
  publishedInScopeCount: 12,
  attentionCount: 2,
  pendingReleaseNeedingSetupCount: 1,
}

function patchSession(visibleRoutes: string[]) {
  const sessionStore = useSessionStore()
  const session: ManagementSession = {
    username: '10000000',
    displayName: 'Admin',
    email: 'admin@example.com',
    authSource: 'LOCAL',
    roles: ['GLOBAL_ADMIN'],
    authorizedGroupCodes: ['*'],
    defaultRoute: ROUTE_KEYS.apiPolicyManagement,
    visibleRoutes,
    expiresAt: new Date().toISOString(),
  }
  sessionStore.$patch({ accessToken: 'token', session })
}

function mountHome(visibleRoutes: string[] = [
  ROUTE_KEYS.apiPolicyManagement,
  ROUTE_KEYS.templateManagement,
]) {
  const pinia = createPinia()
  setActivePinia(pinia)
  patchSession(visibleRoutes)
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(ApiPolicyHomeView, {
    global: {
      plugins: [pinia, i18n, ElementPlus],
    },
  })
}

describe('ApiPolicyHomeView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    vi.mocked(apiPolicyApi.fetchAlerts).mockReset()
    vi.mocked(apiPolicyApi.fetchReadinessSummary).mockReset()
    vi.mocked(apiPolicyApi.listInvocations).mockReset()
    vi.mocked(templatesApi.listAllTemplates).mockReset()
    vi.mocked(apiPolicyApi.fetchReadinessSummary).mockResolvedValue(sampleSummary)
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'RETAIL-ACCOUNT-OPEN',
          groupCode: 'RETAIL',
          name: 'Retail account open',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm1',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
      ],
      totalElements: 1,
      truncated: false,
    })
    vi.mocked(apiPolicyApi.listInvocations).mockResolvedValue({
      content: [
        {
          invocationId: 'inv-1',
          invocationKind: 'SINGLE',
          status: 'SUCCEEDED',
          requestId: 'req-1',
          resolvedReleaseVersion: '1.0.0',
          routeType: 'DEFAULT',
          createdAt: '2026-07-20T12:00:00Z',
          accessAccountSummary: 'acct',
        },
        {
          invocationId: 'inv-2',
          invocationKind: 'SINGLE',
          status: 'FAILED',
          requestId: 'req-2',
          resolvedReleaseVersion: '1.0.0',
          routeType: 'DEFAULT',
          createdAt: '2026-07-20T13:00:00Z',
          accessAccountSummary: 'acct',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })
  })

  it('SCEN-AOD-06: renders readiness summary cards above alerts', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue(sampleAlerts)
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.find('[data-testid="api-readiness-summary"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="summary-card-publishedInScope"]').text()).toContain('12')
    expect(wrapper.find('[data-testid="summary-card-attention"]').text()).toContain('2')
    expect(wrapper.find('[data-testid="summary-card-pendingReleaseNeedingSetup"]').text()).toContain(
      '1',
    )
    expect(wrapper.text()).toContain('Published in scope')
    expect(wrapper.text()).toContain('Need attention')
    expect(wrapper.text()).toContain('Pending release needing setup')
  })

  it('BDD-SYS-NORM-W3-001: renders performance / failure / artifacts ops summaries', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue(sampleAlerts)
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.find('[data-testid="api-ops-summary"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="ops-card-performance"]').text()).toContain('2')
    expect(wrapper.find('[data-testid="ops-card-failureRate"]').text()).toContain('50%')
    expect(wrapper.find('[data-testid="ops-card-artifacts"]').text()).toContain('1')
    expect(wrapper.text()).not.toContain('p95')
    expect(wrapper.text()).not.toContain('error budget')
  })

  it('SCEN-AOD-07: does not render a paginated template catalog', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue(sampleAlerts)
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.findComponent({ name: 'ElPagination' }).exists()).toBe(false)
    expect(wrapper.findAllComponents({ name: 'AppDataTable' })).toHaveLength(1)
    expect(wrapper.find('[data-testid="api-readiness-summary"]').exists()).toBe(true)
  })

  it('renders alerts table instead of coming soon placeholder', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue(sampleAlerts)
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Aggregated alerts coming soon')
    expect(wrapper.text()).toContain('Missing authorized AD group')
    expect(wrapper.text()).toContain('Access key expiring soon')
    expect(wrapper.text()).toContain('RETAIL-ACCOUNT-OPEN')
  })

  it('navigates to package API settings shell on alert row click', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue(sampleAlerts)
    const wrapper = mountHome()
    await flushPromises()

    const table = wrapper.findComponent({ name: 'AppDataTable' })
    await table.vm.$emit('row-click', sampleAlerts[0])

    expect(routerPush).toHaveBeenCalledWith('/api/packages/tpl-1/settings')
  })

  it('shows LoadErrorPanel instead of empty alerts table when fetch fails', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockRejectedValue({
      response: { status: 500, data: { error: { code: 'INTERNAL_ERROR' } } },
    })
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).not.toContain('No attention items')
    expect(wrapper.findComponent({ name: 'LoadErrorPanel' }).exists()).toBe(true)
  })

  it('offers browse templates when alerts are empty', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue([])
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).toContain('Browse templates')
    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'ElCollapse' }).exists()).toBe(false)
  })

  it('LR-C9-B: empty alerts hide browse CTA without template route access', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue([])
    const wrapper = mountHome([ROUTE_KEYS.apiPolicyManagement])
    await flushPromises()

    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('No attention items')
  })

  it('navigates to template catalog from header action', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue([])
    const wrapper = mountHome()
    await flushPromises()

    const browseButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Browse templates'))
    expect(browseButton).toBeDefined()
    await browseButton!.trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/templates')
  })

  it('LR-C9-A: retryable load failure wires retryable hint on LoadErrorPanel', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockRejectedValue(
      axiosEnvelopeError(500, 'apiPolicy.home.alerts.loadFailed', {
        code: 'INTERNAL_ERROR',
        category: 'SYSTEM',
        retryable: true,
        message: 'Unable to load external access alerts.',
      }),
    )
    const wrapper = mountHome()
    await flushPromises()

    const errorPanel = wrapper.findComponent({ name: 'LoadErrorPanel' })
    expect(errorPanel.exists()).toBe(true)
    expect(errorPanel.props('retryable')).toBe(true)
  })
})
