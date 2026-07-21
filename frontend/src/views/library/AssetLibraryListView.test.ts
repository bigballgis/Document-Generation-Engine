import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AssetLibraryListView from '@/views/library/AssetLibraryListView.vue'
import en from '@/i18n/locales/en'
import * as libraryAssetsApi from '@/api/libraryAssets'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

vi.mock('@/api/libraryAssets', () => ({
  listLibraryAssets: vi.fn(),
  uploadLibraryAsset: vi.fn(),
  disableLibraryAsset: vi.fn(),
}))

vi.mock('@/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirmAction: vi.fn().mockResolvedValue(true),
  }),
}))

vi.mock('@/composables/useScopedGroupOptions', () => ({
  useScopedGroupOptions: () => ({
    resolveDefaultGroupCode: (current = '') => current || 'RETAIL',
    ensureGroupCatalog: vi.fn().mockResolvedValue(undefined),
    groupOptions: [
      { value: 'RETAIL', label: 'RETAIL' },
      { value: 'CORP', label: 'CORP' },
    ],
    isGroupLocked: { value: false },
    lockedGroupCode: { value: '' },
    isGlobalAdminActor: { value: false },
    selectableGroupCodes: { value: ['RETAIL', 'CORP'] },
  }),
}))

const scopedGroupStub = {
  props: ['modelValue', 'placeholder', 'clearable', 'disabled'],
  emits: ['update:modelValue'],
  template:
    '<input data-testid="asset-library-group-select-input" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const authorCapabilities: ManagementCapabilities = {
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: true,
  decideTests: false,
  decideApprovals: false,
  decideLegalApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: true,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: true,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
  manageAssetLibrary: true,
  manageLegalHold: false,
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

function sampleAsset(overrides: Record<string, unknown> = {}) {
  return {
    groupCode: 'RETAIL',
    assetKey: 'IMG-LOGO-BANK',
    assetClass: 'IMAGE' as const,
    status: 'ACTIVE' as const,
    contentType: 'image/png' as const,
    sizeBytes: 2048,
    contentSha256: 'c'.repeat(64),
    originalFileName: 'logo.png',
    uploadedBy: 'author',
    uploadedAt: '2026-07-16T10:00:00Z',
    ...overrides,
  }
}

function patchSession(
  roles: string[],
  capabilities: ManagementCapabilities,
  authorizedGroupCodes: string[] = ['RETAIL'],
) {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000003',
      displayName: 'User',
      email: 'user@example.com',
      authSource: 'LOCAL',
      roles,
      authorizedGroupCodes,
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.assetLibraryManagement],
      capabilities,
      expiresAt: '2099-01-01T00:00:00Z',
    },
  })
}

describe('AssetLibraryListView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchSession(['DOCUMENT_AUTHOR'], authorCapabilities)
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockReset()
    vi.mocked(libraryAssetsApi.uploadLibraryAsset).mockReset()
    vi.mocked(libraryAssetsApi.disableLibraryAsset).mockReset()
  })

  function mountView() {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(AssetLibraryListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          ScopedGroupSelect: scopedGroupStub,
        },
      },
    })
  }

  it('renders catalog rows and upload action for authors', async () => {
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(pageView([sampleAsset()]))

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    expect(libraryAssetsApi.listLibraryAssets).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ groupCode: 'RETAIL' }),
    )
    expect(wrapper.text()).toContain('IMG-LOGO-BANK')
    expect(wrapper.text()).toContain('RETAIL')
    expect(wrapper.text()).toContain('logo.png')
    expect(wrapper.find('[data-testid="asset-library-upload-open"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="asset-library-disable"]').exists()).toBe(false)
  })

  it('BDD-ALGI-015 — shows ScopedGroupSelect group filter on the list', async () => {
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(pageView([sampleAsset()]))

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-testid="asset-library-group-filter"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="asset-library-group-select-input"]').exists()).toBe(true)
  })

  it('BDD-ALGI-016 — GLOBAL_ADMIN can clear group filter and reload without groupCode', async () => {
    patchSession(['GLOBAL_ADMIN'], {
      ...authorCapabilities,
      manageMasters: true,
      decideApprovals: true,
      publishTemplates: true,
      manageContentModuleLifecycle: true,
      manageApiPolicy: true,
      readAudit: true,
    }, ['*'])

    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(
      pageView([
        sampleAsset({ groupCode: 'RETAIL' }),
        sampleAsset({
          groupCode: 'CORP',
          assetKey: 'IMG-CORP',
          originalFileName: 'corp.png',
        }),
      ]),
    )

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    vi.mocked(libraryAssetsApi.listLibraryAssets).mockClear()
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(
      pageView([
        sampleAsset({ groupCode: 'RETAIL' }),
        sampleAsset({ groupCode: 'CORP', assetKey: 'IMG-CORP' }),
      ]),
    )

    const groupInput = wrapper.find('[data-testid="asset-library-group-select-input"]')
    await groupInput.setValue('')
    await flushPromises()
    await flushPromises()

    expect(libraryAssetsApi.listLibraryAssets).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ groupCode: undefined }),
    )
  })

  it('passes group filter to list when a group is selected', async () => {
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(pageView([sampleAsset()]))

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    vi.mocked(libraryAssetsApi.listLibraryAssets).mockClear()
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(pageView([sampleAsset()]))

    const groupInput = wrapper.find('[data-testid="asset-library-group-select-input"]')
    await groupInput.setValue('CORP')
    await flushPromises()
    await flushPromises()

    expect(libraryAssetsApi.listLibraryAssets).toHaveBeenCalledWith(
      0,
      20,
      expect.objectContaining({ groupCode: 'CORP' }),
    )
  })

  it('disables with groupCode for admins on ACTIVE rows', async () => {
    patchSession(['GLOBAL_ADMIN'], {
      ...authorCapabilities,
      manageMasters: true,
      decideApprovals: true,
      decideLegalApprovals: false,
      publishTemplates: true,
      manageContentModuleLifecycle: true,
      manageApiPolicy: true,
      readAudit: true,
    })
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(
      pageView([sampleAsset({ assetKey: 'IMG-ADMIN', originalFileName: 'a.png', uploadedBy: 'admin' })]),
    )
    vi.mocked(libraryAssetsApi.disableLibraryAsset).mockResolvedValue(
      sampleAsset({ assetKey: 'IMG-ADMIN', status: 'DISABLED' }),
    )

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    const disable = wrapper.find('[data-testid="asset-library-disable"]')
    expect(disable.exists()).toBe(true)
    await disable.trigger('click')
    await flushPromises()

    expect(libraryAssetsApi.disableLibraryAsset).toHaveBeenCalledWith('IMG-ADMIN', 'RETAIL')
  })

  it('hides upload for testers and shows ACTIVE assets only', async () => {
    patchSession(['TEMPLATE_TESTER'], {
      ...authorCapabilities,
      authorTemplates: false,
      decideTests: true,
      exportTemplates: false,
      authorContentModules: false,
    })
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(
      pageView([
        sampleAsset({
          assetKey: 'IMG-TEST',
          sizeBytes: 100,
          contentSha256: 'd'.repeat(64),
          originalFileName: 't.png',
          uploadedBy: 'admin',
          uploadedAt: '2026-07-16T11:00:00Z',
        }),
      ]),
    )

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-testid="asset-library-upload-open"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('IMG-TEST')
  })

  it('BDD-SYS-NORM-W8-001 — shows honest empty with Upload CTA when permitted', async () => {
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(pageView([]))

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain(en.assetLibrary.list.empty)
    expect(wrapper.text()).toContain(en.assetLibrary.list.emptyDescription)
    expect(wrapper.find('[data-testid="asset-library-table"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="asset-library-upload-open-empty"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="asset-library-group-filter"]').exists()).toBe(true)
  })

  it('BDD-SYS-NORM-W8-001 — honest empty without Upload CTA when not permitted', async () => {
    patchSession(['TEMPLATE_TESTER'], {
      ...authorCapabilities,
      authorTemplates: false,
      decideTests: true,
      exportTemplates: false,
      authorContentModules: false,
      manageAssetLibrary: false,
    })
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(pageView([]))

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain(en.assetLibrary.list.empty)
    expect(wrapper.text()).toContain(en.assetLibrary.list.emptyDescriptionReadOnly)
    expect(wrapper.find('[data-testid="asset-library-upload-open"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="asset-library-upload-open-empty"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
  })

  it('shows disable action for admins on ACTIVE rows', async () => {
    patchSession(['GLOBAL_ADMIN'], {
      ...authorCapabilities,
      manageMasters: true,
      decideApprovals: true,
      decideLegalApprovals: false,
      publishTemplates: true,
      manageContentModuleLifecycle: true,
      manageApiPolicy: true,
      readAudit: true,
    })
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue(
      pageView([
        sampleAsset({
          assetKey: 'IMG-ADMIN',
          sizeBytes: 100,
          contentSha256: 'e'.repeat(64),
          originalFileName: 'a.png',
          uploadedBy: 'admin',
          uploadedAt: '2026-07-16T12:00:00Z',
        }),
      ]),
    )

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-testid="asset-library-disable"]').exists()).toBe(true)
  })
})
