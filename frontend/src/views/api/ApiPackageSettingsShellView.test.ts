import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ApiPackageSettingsShellView from '@/views/api/ApiPackageSettingsShellView.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import { useSessionStore } from '@/stores/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

vi.mock('@/api/templates', () => ({
  getTemplate: vi.fn(),
}))

vi.mock('@/stores/apiPolicy', () => ({
  useApiPolicyStore: () => ({
    apiPolicy: null,
    credentials: [],
    loadingPolicy: false,
    submitting: false,
    lastErrorMessageKey: null,
    lastCreatedCredential: null,
    lastRotatedCredential: null,
    setActiveTemplate: vi.fn(),
    fetchPolicy: vi.fn().mockResolvedValue(null),
    fetchCredentials: vi.fn().mockResolvedValue([]),
    createCredential: vi.fn(),
    rotateCredential: vi.fn(),
    revokeCredential: vi.fn(),
  }),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { templateId: 'tpl-1' },
    query: { releaseVersion: '1.0.0' },
  }),
  useRouter: () => ({ push: routerPush }),
}))

describe('ApiPackageSettingsShellView (BDD-SYS-NORM-W2-007)', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      visibleRoutes: [ROUTE_KEYS.apiPolicyManagement, ROUTE_KEYS.templateManagement],
      capabilities: { manageApiPolicy: true },
    } as never
    routerPush.mockReset()
    vi.mocked(templatesApi.getTemplate).mockResolvedValue({
      id: 'tpl-1',
      externalId: 'TPL-1',
      groupCode: 'RETAIL',
      name: 'Retail letter',
      description: null,
      masterId: 'master-1',
      lifecycleStatus: 'PUBLISHED',
      releaseVersion: '1.0.0',
      devVersionId: 'dev-1',
      devVersionNumber: 1,
      bindings: [],
      variables: [],
      rules: [],
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
  })

  it('renders honest interim package settings shell with version context', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ApiPackageSettingsShellView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="api-package-settings-interim-banner"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="api-package-settings-release-context"]').text()).toContain(
      '1.0.0',
    )
    expect(wrapper.text()).toContain('Retail letter')
  })
})
