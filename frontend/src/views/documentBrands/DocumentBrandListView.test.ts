import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DocumentBrandListView from '@/views/documentBrands/DocumentBrandListView.vue'
import en from '@/i18n/locales/en'
import * as documentBrandsApi from '@/api/documentBrands'
import { useSessionStore } from '@/stores/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

vi.mock('@/api/documentBrands', () => ({
  listDocumentBrands: vi.fn(),
  createDocumentBrand: vi.fn(),
  updateDocumentBrand: vi.fn(),
}))

vi.mock('@/composables/useScopedGroupOptions', () => ({
  useScopedGroupOptions: () => ({
    groupOptions: { value: [{ value: 'RETAIL', label: 'RETAIL' }] },
    isGroupLocked: { value: true },
    lockedGroupCode: { value: 'RETAIL' },
    ensureGroupCatalog: vi.fn().mockResolvedValue(undefined),
    resolveDefaultGroupCode: () => 'RETAIL',
  }),
}))

function pageView<T>(content: T[]) {
  return {
    content,
    page: 0,
    size: Math.max(content.length, 1),
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
  }
}

function patchSession(roles: string[]) {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000001',
      displayName: 'Admin',
      email: 'admin@example.com',
      authSource: 'LOCAL',
      roles,
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [
        ROUTE_KEYS.dashboardHome,
        ROUTE_KEYS.documentBrandAdministration,
      ],
      capabilities: undefined,
      expiresAt: '2099-01-01T00:00:00Z',
    },
  })
}

describe('DocumentBrandListView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchSession(['GROUP_ADMIN'])
    vi.mocked(documentBrandsApi.listDocumentBrands).mockReset()
    vi.mocked(documentBrandsApi.listDocumentBrands).mockResolvedValue(
      pageView([
        {
          groupCode: 'RETAIL',
          documentBrandCode: 'PLATFORM_DEFAULT',
          displayName: 'Platform default',
          status: 'ACTIVE',
          logoObjectRef: 'platform/document-brands/PLATFORM_DEFAULT/logo',
        },
      ]),
    )
  })

  it('lists document brands and shows create for admins', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(DocumentBrandListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    expect(documentBrandsApi.listDocumentBrands).toHaveBeenCalled()
    expect(wrapper.text()).toContain('PLATFORM_DEFAULT')
    expect(wrapper.find('[data-testid="document-brand-create-open"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Document brands')
  })

  it('hides create action for non-admin sessions', async () => {
    patchSession(['TEMPLATE_AUTHOR'])
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(DocumentBrandListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="document-brand-create-open"]').exists()).toBe(false)
  })
})
