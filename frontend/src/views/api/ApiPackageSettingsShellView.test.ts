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
    apiPolicy: {
      templateId: 'tpl-1',
      policyVersion: 1,
      allowedAdGroups: ['RETAIL-CALLERS'],
      defaultRouteReleaseVersion: '1.0.0',
      outputFormats: ['PDF'],
      outputModes: ['SYNC'],
      batchEnabled: false,
      maxBatchSize: 1,
      docxEncryptionEnabled: false,
      pdfEncryptionEnabled: false,
      saveGeneratedDocuments: true,
      invocationRecordRetentionDays: 90,
      documentRetentionDays: 90,
      updatedAt: '2026-07-20T00:00:00Z',
    },
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
const routeQuery = { releaseVersion: '1.0.0', panel: 'not-a-real-panel' as string | undefined }

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { templateId: 'tpl-1' },
    query: routeQuery,
  }),
  useRouter: () => ({ push: routerPush }),
}))

describe('ApiPackageSettingsShellView (BDD-SYS-NORM-W3-008…010)', () => {
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
    routeQuery.releaseVersion = '1.0.0'
    routeQuery.panel = 'not-a-real-panel'
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

  it('renders complete settings home without interim framing', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ApiPackageSettingsShellView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="api-package-settings-interim-banner"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="api-package-settings-release-context"]').text()).toContain(
      '1.0.0',
    )
    expect(wrapper.find('[data-testid="api-package-settings-unknown-panel"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Retail letter')
    expect(wrapper.text()).toContain('Edit package-level external access')
  })
})
