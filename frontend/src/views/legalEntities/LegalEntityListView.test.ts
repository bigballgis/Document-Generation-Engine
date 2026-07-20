import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LegalEntityListView from '@/views/legalEntities/LegalEntityListView.vue'
import en from '@/i18n/locales/en'
import * as legalEntitiesApi from '@/api/legalEntities'
import * as documentBrandsApi from '@/api/documentBrands'
import { useSessionStore } from '@/stores/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

vi.mock('@/api/legalEntities', () => ({
  listLegalEntities: vi.fn(),
  createLegalEntity: vi.fn(),
  updateLegalEntity: vi.fn(),
  getGroupDefaultLegalEntity: vi.fn(),
  putGroupDefaultLegalEntity: vi.fn(),
}))

vi.mock('@/api/documentBrands', () => ({
  listDocumentBrands: vi.fn(),
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

describe('LegalEntityListView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchSession(['GROUP_ADMIN'])
    vi.mocked(legalEntitiesApi.listLegalEntities).mockReset()
    vi.mocked(legalEntitiesApi.getGroupDefaultLegalEntity).mockReset()
    vi.mocked(documentBrandsApi.listDocumentBrands).mockResolvedValue(pageView([]))
    vi.mocked(legalEntitiesApi.listLegalEntities).mockResolvedValue(
      pageView([
        {
          groupCode: 'RETAIL',
          legalEntityCode: 'LE-HK-001',
          displayName: 'HK Entity',
          status: 'ACTIVE',
          documentBrandCode: 'HK-RETAIL-LETTER',
        },
      ]),
    )
    vi.mocked(legalEntitiesApi.getGroupDefaultLegalEntity).mockResolvedValue({
      groupCode: 'RETAIL',
      defaultLegalEntityCode: 'LE-HK-001',
    })
  })

  it('lists legal entities with brand binding and group default panel', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(LegalEntityListView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('LE-HK-001')
    expect(wrapper.text()).toContain('HK-RETAIL-LETTER')
    expect(wrapper.find('[data-testid="legal-entity-default-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="legal-entity-create-open"]').exists()).toBe(true)
  })
})
