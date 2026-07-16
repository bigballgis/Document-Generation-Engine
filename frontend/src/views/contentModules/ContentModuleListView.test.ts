import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentModuleListView from '@/views/contentModules/ContentModuleListView.vue'
import en from '@/i18n/locales/en'
import * as contentModulesApi from '@/api/contentModules'
import { useSessionStore } from '@/stores/session'
import type { ContentModuleSummary } from '@/types/contentModule'

vi.mock('@/api/contentModules', () => ({
  listContentModules: vi.fn(),
  getContentModule: vi.fn(),
  createContentModule: vi.fn(),
}))

vi.mock('@/stores/identity', () => ({
  useIdentityStore: () => ({
    groups: [],
    fetchGroups: vi.fn(),
  }),
}))

const routerPush = vi.fn()

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

function summary(
  overrides: Partial<ContentModuleSummary> & Pick<ContentModuleSummary, 'moduleId' | 'name'>,
): ContentModuleSummary {
  return {
    moduleId: overrides.moduleId,
    moduleCode: overrides.moduleCode ?? overrides.moduleId,
    groupCode: overrides.groupCode ?? 'RETAIL',
    name: overrides.name,
    reviewState: overrides.reviewState ?? 'DRAFT',
    lifecycleState: overrides.lifecycleState,
    createdAt: overrides.createdAt ?? '2026-06-26T10:00:00Z',
    updatedAt: overrides.updatedAt ?? '2026-06-26T10:00:00Z',
  }
}

function patchAuthorSession() {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000003',
      displayName: 'Author',
      email: 'author@example.com',
      authSource: 'LOCAL',
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: 'route.dashboard-home',
      visibleRoutes: ['route.dashboard-home', 'route.content-module-management'],
      expiresAt: '2099-01-01T00:00:00Z',
    },
  })
}

describe('ContentModuleListView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchAuthorSession()
    routerPush.mockReset()
    vi.mocked(contentModulesApi.listContentModules).mockReset()
  })

  it('renders content modules across authorized groups', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue(
      pageView([
        summary({
          moduleId: 'MOD-LOAN-DISCLOSURE',
          name: 'Loan disclosure',
          reviewState: 'DRAFT',
        }),
      ]),
    )

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()
    await flushPromises()

    expect(contentModulesApi.listContentModules).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ sort: 'groupCodeAsc' }),
    )
    expect(wrapper.text()).toContain('Loan disclosure')
    expect(wrapper.text()).toContain('MOD-LOAN-DISCLOSURE')
    expect(wrapper.text()).toContain('RETAIL')
  })

  it('CCS-005: Status column renders ContentModuleStatusBadge labels', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue(
      pageView([
        summary({
          moduleId: 'MOD-DRAFT',
          name: 'Draft clause',
          reviewState: 'DRAFT',
        }),
        summary({
          moduleId: 'MOD-APPROVED',
          name: 'Approved clause',
          reviewState: 'APPROVED',
          lifecycleState: 'ACTIVE',
        }),
      ]),
    )

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('Status')
    expect(wrapper.text()).toContain('Draft')
    expect(wrapper.text()).toContain('Approved')
  })

  it('CCS-006: status filter requests status=DRAFT and resets to page 0', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue(
      pageView(
        [
          summary({
            moduleId: 'MOD-LOAN-DISCLOSURE',
            name: 'Loan disclosure',
            reviewState: 'DRAFT',
          }),
        ],
        25,
      ),
    )

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })

    await flushPromises()
    await flushPromises()

    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue(pageView([]))

    const toolbar = wrapper.findComponent({ name: 'CatalogFilterToolbar' })
    const filterValues = toolbar.props('filterValues') as Record<string, string>
    filterValues.status = 'DRAFT'
    await flushPromises()
    await flushPromises()

    expect(contentModulesApi.listContentModules).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ status: 'DRAFT', sort: 'groupCodeAsc' }),
    )
  })

  it('shows load error with retry when list fails', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockRejectedValue(new Error('network'))

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('Unable to load content modules')
    expect(wrapper.text()).toContain('Retry')
  })

  it('LR-C5: group filter triggers server request with groupCode', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue(
      pageView(
        [
          summary({
            moduleId: 'MOD-LOAN-DISCLOSURE',
            name: 'Loan disclosure',
            reviewState: 'DRAFT',
          }),
        ],
        25,
      ),
    )

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()
    await flushPromises()

    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue(pageView([]))
    const groupFilter = wrapper.find('.catalog-filter-toolbar__control input')
    await groupFilter.setValue('RETAIL')
    await flushPromises()
    await flushPromises()

    expect(contentModulesApi.listContentModules).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ groupCode: 'RETAIL', sort: 'groupCodeAsc' }),
    )
  })

  it('LR-C9-B: empty catalog shows create CTA for template authors', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue(pageView([]))

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()
    await flushPromises()

    const emptyActions = wrapper.find('[data-testid="empty-state-actions"]')
    expect(emptyActions.exists()).toBe(true)
    expect(emptyActions.text()).toContain('New content module')
  })

  it('CCS-009 / LR-C9-B: empty catalog hides create CTA without author capability', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue(pageView([]))
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      session: {
        username: '10000006',
        displayName: 'Tester',
        email: 'tester@example.com',
        authSource: 'LOCAL',
        roles: ['TEMPLATE_APPROVER'],
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: 'route.dashboard-home',
        visibleRoutes: ['route.dashboard-home', 'route.content-module-management'],
        expiresAt: '2099-01-01T00:00:00Z',
      },
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('No standard clauses yet.')
  })

  it('LR-C9-A: retry after load failure reloads modules', async () => {
    vi.mocked(contentModulesApi.listContentModules)
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce(
        pageView([
          summary({
            moduleId: 'MOD-LOAN-DISCLOSURE',
            name: 'Loan disclosure',
            reviewState: 'DRAFT',
          }),
        ]),
      )

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()
    await flushPromises()

    await wrapper.findComponent({ name: 'LoadErrorPanel' }).vm.$emit('retry')
    await flushPromises()
    await flushPromises()

    expect(contentModulesApi.listContentModules).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Loan disclosure')
  })
})
