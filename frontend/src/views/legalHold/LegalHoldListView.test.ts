import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TableEditMoreActions from '@/components/common/TableEditMoreActions.vue'
import LegalHoldListView from '@/views/legalHold/LegalHoldListView.vue'
import en from '@/i18n/locales/en'
import * as legalHoldsApi from '@/api/legalHolds'
import { USERS_CATALOG_PATH } from '@/composables/useEntityLinkTargets'
import { useSessionStore } from '@/stores/session'
import type { LegalHoldView } from '@/types/legalHold'
import type { ManagementCapabilities } from '@/types/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

vi.mock('@/api/legalHolds', () => ({
  listLegalHolds: vi.fn(),
  createLegalHold: vi.fn(),
  releaseLegalHold: vi.fn(),
}))

vi.mock('@/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirmAction: vi.fn().mockResolvedValue(true),
  }),
}))

vi.mock('@/composables/useAuditTemplateFilterOptions', () => ({
  useAuditTemplateFilterOptions: () => ({
    templateOptions: { value: [] },
    loadingTemplates: { value: false },
    searchTemplates: vi.fn().mockResolvedValue(undefined),
  }),
}))

const adminCapabilities: ManagementCapabilities = {
  manageMasters: true,
  reviewMasters: true,
  authorTemplates: true,
  decideTests: true,
  decideApprovals: true,
  decideLegalApprovals: false,
  publishTemplates: true,
  stopTemplates: true,
  restoreOrDeprecateTemplates: true,
  deleteTemplates: true,
  exportTemplates: true,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: true,
  authorContentModules: true,
  decideContentModuleReviews: true,
  manageContentModuleLifecycle: true,
  manageApiPolicy: true,
  readAudit: true,
  manageAssetLibrary: true,
  manageLegalHold: true,
}

function pageView<T>(content: T[], totalElements = content.length) {
  return {
    content,
    page: 0,
    size: 20,
    totalElements,
    totalPages: totalElements === 0 ? 0 : Math.ceil(totalElements / 20),
  }
}

function sampleHold(overrides: Partial<LegalHoldView> = {}): LegalHoldView {
  return {
    id: 'hold-1',
    holdExternalId: 'LH-001',
    scopeType: 'TEMPLATE_WINDOW',
    status: 'ACTIVE',
    reason: 'Litigation freeze',
    templateId: 'tpl-1',
    templateExternalId: 'TPL-001',
    effectiveFrom: '2026-01-01T00:00:00Z',
    effectiveTo: null,
    invocationExternalIds: [],
    invocationCount: 0,
    createdAt: '2026-07-16T10:00:00Z',
    createdByUsername: '10000001',
    releasedAt: null,
    releasedByUsername: null,
    ...overrides,
  }
}

describe('LegalHoldListView', () => {
  let pinia: ReturnType<typeof createPinia>
  let activeWrapper: VueWrapper | null = null

  function patchSession(roles: string[], capabilities: ManagementCapabilities, routes?: string[]) {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: '10000001',
        displayName: 'Global Admin',
        email: 'admin@example.com',
        authSource: 'LOCAL',
        roles,
        authorizedGroupCodes: ['*'],
        defaultRoute: ROUTE_KEYS.dashboardHome,
        visibleRoutes: routes ?? [
          ROUTE_KEYS.dashboardHome,
          ROUTE_KEYS.legalHoldAdministration,
          ROUTE_KEYS.templateManagement,
        ],
        // Clone so Pinia reactive merge cannot mutate the shared fixture object.
        capabilities: { ...capabilities },
        expiresAt: '2099-01-01T00:00:00Z',
      },
    })
  }

  async function mountListView(options?: { withRouter?: boolean }) {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    let router: ReturnType<typeof createRouter> | undefined
    if (options?.withRouter) {
      router = createRouter({
        history: createWebHistory(),
        routes: [
          { path: '/', component: { template: '<div />' } },
          {
            path: USERS_CATALOG_PATH,
            component: { template: '<div data-testid="users-catalog" />' },
          },
        ],
      })
      await router.push('/')
      await router.isReady()
    }
    activeWrapper = mount(LegalHoldListView, {
      global: {
        plugins: router ? [pinia, i18n, ElementPlus, router] : [pinia, i18n, ElementPlus],
        stubs: options?.withRouter
          ? { LegalHoldCreateDialog: true }
          : {
              LegalHoldCreateDialog: true,
              RouterLink: {
                props: ['to'],
                template:
                  '<a class="router-link-stub" :data-to="JSON.stringify(to)"><slot /></a>',
              },
            },
      },
    })
    await flushPromises()
    await flushPromises()
    return { wrapper: activeWrapper, router }
  }

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchSession(['GLOBAL_ADMIN'], adminCapabilities)
    vi.mocked(legalHoldsApi.listLegalHolds).mockReset()
    vi.mocked(legalHoldsApi.createLegalHold).mockReset()
    vi.mocked(legalHoldsApi.releaseLegalHold).mockReset()
  })

  afterEach(() => {
    activeWrapper?.unmount()
    activeWrapper = null
    document.body.querySelectorAll('.el-popper, .el-overlay').forEach((node) => node.remove())
  })

  async function emitReleaseCommand(wrapper: VueWrapper) {
    const actions = wrapper.findComponent(TableEditMoreActions)
    expect(actions.exists()).toBe(true)
    const dropdown = actions.findComponent({ name: 'ElDropdown' })
    expect(dropdown.exists()).toBe(true)
    await dropdown.vm.$emit('command', 'release')
    await flushPromises()
  }

  async function openMoreMenu(wrapper: VueWrapper) {
    const actions = wrapper.find('[data-testid="table-edit-more-actions"]')
    expect(actions.exists()).toBe(true)
    const moreButton = actions.findAll('button').find((button) => button.text().includes('More'))
    expect(moreButton).toBeDefined()
    await moreButton!.trigger('click')
    await flushPromises()
  }

  describe('PQH N22 catalog row actions (BDD-PQH-N22-006…008)', () => {
    it('BDD-PQH-N22-006/012 — Actions uses TableEditMoreActions with shared testid', async () => {
      patchSession(['GLOBAL_ADMIN'], adminCapabilities)
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(pageView([sampleHold()]))

      const { wrapper } = await mountListView()

      expect(wrapper.text()).toContain('LH-001')
      expect(wrapper.find('[data-testid="table-edit-more-actions"]').exists()).toBe(true)
      expect(wrapper.findComponent(TableEditMoreActions).exists()).toBe(true)
    })

    it('BDD-PQH-N22-007 — hides Edit; Release under More', async () => {
      patchSession(['GLOBAL_ADMIN'], adminCapabilities)
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(pageView([sampleHold()]))
      vi.mocked(legalHoldsApi.releaseLegalHold).mockResolvedValue(
        sampleHold({ status: 'RELEASED', releasedAt: '2026-07-16T12:00:00Z' }),
      )

      const { wrapper } = await mountListView()
      const actions = wrapper.find('[data-testid="table-edit-more-actions"]')

      expect(actions.find('.table-edit-more-actions__edit').exists()).toBe(false)
      expect(actions.text()).toContain('More')
      expect(actions.text()).not.toContain('Edit')

      await openMoreMenu(wrapper)
      expect(document.body.querySelector('[data-testid="legal-hold-release"]')).toBeTruthy()
      await emitReleaseCommand(wrapper)
      expect(legalHoldsApi.releaseLegalHold).toHaveBeenCalledWith('hold-1')
    })

    it('BDD-PQH-N22-008 — Actions column hidden without manage entitlement', async () => {
      patchSession(['GROUP_ADMIN'], { ...adminCapabilities, manageLegalHold: false })
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(pageView([sampleHold()]))

      const { wrapper } = await mountListView()

      expect(wrapper.find('[data-testid="table-edit-more-actions"]').exists()).toBe(false)
    })
  })

  it('renders holds, create action, and releases after confirm for GLOBAL_ADMIN', async () => {
    vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(pageView([sampleHold()]))
    vi.mocked(legalHoldsApi.releaseLegalHold).mockResolvedValue(
      sampleHold({
        status: 'RELEASED',
        releasedAt: '2026-07-16T12:00:00Z',
        releasedByUsername: '10000001',
      }),
    )

    const { wrapper } = await mountListView()

    expect(legalHoldsApi.listLegalHolds).toHaveBeenCalled()
    expect(wrapper.text()).toContain('LH-001')
    expect(wrapper.text()).toContain('TPL-001')
    expect(wrapper.find('[data-testid="legal-hold-create-open"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="table-edit-more-actions"]').exists()).toBe(true)

    await openMoreMenu(wrapper)
    expect(document.body.querySelector('[data-testid="legal-hold-release"]')).toBeTruthy()
    await emitReleaseCommand(wrapper)

    expect(legalHoldsApi.releaseLegalHold).toHaveBeenCalledWith('hold-1')
    expect(vi.mocked(legalHoldsApi.listLegalHolds).mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('BDD-SYS-NORM-W8-005 — honest empty with Create CTA when manageLegalHold', async () => {
    vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(pageView([]))

    const { wrapper } = await mountListView()

    expect(wrapper.text()).toContain(en.legalHold.list.empty)
    expect(wrapper.text()).toContain(en.legalHold.list.emptyDescription)
    expect(wrapper.find('[data-testid="legal-hold-table"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="legal-hold-create-open-empty"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(true)
  })

  it('BDD-SYS-NORM-W8-006 — honest empty without Create CTA when no manage', async () => {
    patchSession(['GROUP_ADMIN'], { ...adminCapabilities, manageLegalHold: false })
    vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(pageView([]))

    const { wrapper } = await mountListView()

    expect(wrapper.text()).toContain(en.legalHold.list.empty)
    expect(wrapper.text()).toContain(en.legalHold.list.emptyDescriptionReadOnly)
    expect(wrapper.find('[data-testid="legal-hold-table"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="legal-hold-create-open"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="legal-hold-create-open-empty"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
  })

  it('hides create and release when manageLegalHold is false', async () => {
    patchSession(['GROUP_ADMIN'], { ...adminCapabilities, manageLegalHold: false })
    vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
      pageView([
        sampleHold({
          id: 'hold-2',
          holdExternalId: 'LH-002',
          scopeType: 'INVOCATION_SET',
          reason: null,
          templateId: null,
          templateExternalId: null,
          effectiveFrom: null,
          invocationExternalIds: ['INV-1'],
          invocationCount: 1,
          createdAt: '2026-07-16T11:00:00Z',
        }),
      ]),
    )

    const { wrapper } = await mountListView()

    expect(wrapper.find('[data-testid="legal-hold-create-open"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="table-edit-more-actions"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="legal-hold-release"]').exists()).toBe(false)
  })

  describe('N18 Created by EntityLink (BDD-N18-L1-001…007)', () => {
    it('BDD-N18-L1-001 — Created by uses EntityLinkCell', async () => {
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(pageView([sampleHold()]))

      const { wrapper } = await mountListView()
      const cell = wrapper.find('[data-testid="legal-hold-created-by"]')

      expect(cell.exists()).toBe(true)
      expect(cell.classes()).toContain('entity-link-cell')
    })

    it('BDD-N18-L1-002 — display name preferred as label', async () => {
      patchSession(
        ['GLOBAL_ADMIN'],
        adminCapabilities,
        [
          ROUTE_KEYS.dashboardHome,
          ROUTE_KEYS.legalHoldAdministration,
          ROUTE_KEYS.identityAdministration,
        ],
      )
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
        pageView([
          sampleHold({
            createdByUsername: '10000001',
            createdByDisplayName: 'Alice Author',
          }),
        ]),
      )

      const { wrapper } = await mountListView()
      const cell = wrapper.find('[data-testid="legal-hold-created-by"]')

      expect(cell.text()).toContain('Alice Author')
      expect(cell.text()).not.toContain('10000001')
    })

    it('BDD-N18-L1-003 — username fallback when display name missing', async () => {
      patchSession(
        ['GLOBAL_ADMIN'],
        adminCapabilities,
        [
          ROUTE_KEYS.dashboardHome,
          ROUTE_KEYS.legalHoldAdministration,
          ROUTE_KEYS.identityAdministration,
        ],
      )
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
        pageView([sampleHold({ createdByUsername: '10000001', createdByDisplayName: null })]),
      )

      const { wrapper } = await mountListView()
      const cell = wrapper.find('[data-testid="legal-hold-created-by"]')

      expect(cell.text()).toContain('10000001')
    })

    it('BDD-N18-L1-004 — link when identity administration permitted', async () => {
      patchSession(
        ['GLOBAL_ADMIN'],
        adminCapabilities,
        [
          ROUTE_KEYS.dashboardHome,
          ROUTE_KEYS.legalHoldAdministration,
          ROUTE_KEYS.identityAdministration,
        ],
      )
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
        pageView([sampleHold({ createdByUsername: '10000001' })]),
      )

      const { wrapper } = await mountListView()
      const cell = wrapper.find('[data-testid="legal-hold-created-by"]')
      const link = cell.find('.entity-link-cell__link, .router-link-stub')

      expect(link.exists()).toBe(true)
      const toAttr = link.attributes('data-to')
      expect(toAttr).toBeTruthy()
      expect(JSON.parse(toAttr!)).toEqual({
        path: USERS_CATALOG_PATH,
        query: { q: '10000001' },
      })
    })

    it('BDD-N18-L1-005 — plain text when identity administration denied', async () => {
      patchSession(
        ['GROUP_ADMIN'],
        { ...adminCapabilities, manageLegalHold: false },
        [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.legalHoldAdministration],
      )
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
        pageView([sampleHold({ createdByUsername: '10000001' })]),
      )

      const { wrapper } = await mountListView()
      const cell = wrapper.find('[data-testid="legal-hold-created-by"]')

      expect(cell.text()).toContain('10000001')
      expect(cell.find('.entity-link-cell__link').exists()).toBe(false)
      expect(cell.find('.router-link-stub').exists()).toBe(false)
      expect(cell.find('.entity-link-cell__text').exists()).toBe(true)
    })

    it('BDD-N18-L1-006 — empty actor is em dash and not a link', async () => {
      patchSession(
        ['GLOBAL_ADMIN'],
        adminCapabilities,
        [
          ROUTE_KEYS.dashboardHome,
          ROUTE_KEYS.legalHoldAdministration,
          ROUTE_KEYS.identityAdministration,
        ],
      )
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
        pageView([
          sampleHold({
            createdByUsername: '   ',
            createdByDisplayName: null,
          }),
        ]),
      )

      const { wrapper } = await mountListView()
      const cell = wrapper.find('[data-testid="legal-hold-created-by"]')

      expect(cell.text()).toBe('—')
      expect(cell.find('.entity-link-cell__link').exists()).toBe(false)
      expect(cell.find('.router-link-stub').exists()).toBe(false)
    })

    it('BDD-N18-L1-007 — activating link navigates to users catalog with q prefill', async () => {
      patchSession(
        ['GLOBAL_ADMIN'],
        adminCapabilities,
        [
          ROUTE_KEYS.dashboardHome,
          ROUTE_KEYS.legalHoldAdministration,
          ROUTE_KEYS.identityAdministration,
        ],
      )
      vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
        pageView([sampleHold({ createdByUsername: '10000001' })]),
      )

      const { wrapper, router } = await mountListView({ withRouter: true })
      const link = wrapper.find('[data-testid="legal-hold-created-by"] .entity-link-cell__link')

      expect(link.exists()).toBe(true)
      await link.trigger('click')
      await flushPromises()

      expect(router?.currentRoute.value.path).toBe(USERS_CATALOG_PATH)
      expect(router?.currentRoute.value.query.q).toBe('10000001')
    })
  })
})
