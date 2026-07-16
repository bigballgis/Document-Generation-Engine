import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LegalHoldListView from '@/views/legalHold/LegalHoldListView.vue'
import en from '@/i18n/locales/en'
import * as legalHoldsApi from '@/api/legalHolds'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

vi.mock('@/api/legalHolds', () => ({
  listLegalHolds: vi.fn(),
  createLegalHold: vi.fn(),
  releaseLegalHold: vi.fn(),
  getLegalHold: vi.fn(),
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
      capabilities,
      expiresAt: '2099-01-01T00:00:00Z',
    },
  })
}

describe('LegalHoldListView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchSession(['GLOBAL_ADMIN'], adminCapabilities)
    vi.mocked(legalHoldsApi.listLegalHolds).mockReset()
    vi.mocked(legalHoldsApi.createLegalHold).mockReset()
    vi.mocked(legalHoldsApi.releaseLegalHold).mockReset()
  })

  it('renders holds, create action, and releases after confirm for GLOBAL_ADMIN', async () => {
    vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
      pageView([
        {
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
        },
      ]),
    )
    vi.mocked(legalHoldsApi.releaseLegalHold).mockResolvedValue({
      id: 'hold-1',
      holdExternalId: 'LH-001',
      scopeType: 'TEMPLATE_WINDOW',
      status: 'RELEASED',
      reason: 'Litigation freeze',
      templateId: 'tpl-1',
      templateExternalId: 'TPL-001',
      effectiveFrom: '2026-01-01T00:00:00Z',
      effectiveTo: null,
      invocationExternalIds: [],
      invocationCount: 0,
      createdAt: '2026-07-16T10:00:00Z',
      createdByUsername: '10000001',
      releasedAt: '2026-07-16T12:00:00Z',
      releasedByUsername: '10000001',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(LegalHoldListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          LegalHoldCreateDialog: true,
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    })

    await flushPromises()
    await flushPromises()

    expect(legalHoldsApi.listLegalHolds).toHaveBeenCalled()
    expect(wrapper.text()).toContain('LH-001')
    expect(wrapper.text()).toContain('TPL-001')
    expect(wrapper.find('[data-testid="legal-hold-create-open"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="legal-hold-release"]').exists()).toBe(true)

    await wrapper.find('[data-testid="legal-hold-release"]').trigger('click')
    await flushPromises()

    expect(legalHoldsApi.releaseLegalHold).toHaveBeenCalledWith('hold-1')
    expect(vi.mocked(legalHoldsApi.listLegalHolds).mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('hides create and release when manageLegalHold is false', async () => {
    patchSession(['GROUP_ADMIN'], { ...adminCapabilities, manageLegalHold: false })
    vi.mocked(legalHoldsApi.listLegalHolds).mockResolvedValue(
      pageView([
        {
          id: 'hold-2',
          holdExternalId: 'LH-002',
          scopeType: 'INVOCATION_SET',
          status: 'ACTIVE',
          reason: null,
          templateId: null,
          templateExternalId: null,
          effectiveFrom: null,
          effectiveTo: null,
          invocationExternalIds: ['INV-1'],
          invocationCount: 1,
          createdAt: '2026-07-16T11:00:00Z',
          createdByUsername: '10000001',
          releasedAt: null,
          releasedByUsername: null,
        },
      ]),
    )

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(LegalHoldListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          LegalHoldCreateDialog: true,
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-testid="legal-hold-create-open"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="legal-hold-release"]').exists()).toBe(false)
  })
})
