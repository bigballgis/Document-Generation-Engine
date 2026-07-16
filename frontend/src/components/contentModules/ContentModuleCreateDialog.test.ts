import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentModuleCreateDialog from '@/components/contentModules/ContentModuleCreateDialog.vue'
import en from '@/i18n/locales/en'
import { useContentModulesStore } from '@/stores/contentModules'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'

const ensureGroupCatalog = vi.fn().mockResolvedValue(undefined)
const resolveDefaultGroupCode = vi.fn((current = '') => current || 'HQ')
const groupOptions = {
  value: [
    { value: 'HQ', label: 'HQ' },
    { value: 'RETAIL', label: 'RETAIL' },
    { value: 'WEALTH', label: 'WEALTH' },
  ],
}

vi.mock('@/composables/useScopedGroupOptions', () => ({
  useScopedGroupOptions: () => ({
    groupOptions,
    isGroupLocked: { value: false },
    lockedGroupCode: { value: '' },
    ensureGroupCatalog,
    resolveDefaultGroupCode,
    selectableGroupCodes: { value: ['HQ', 'RETAIL', 'WEALTH'] },
  }),
}))

vi.mock('@/components/common/ScopedGroupSelect.vue', () => ({
  default: {
    name: 'ScopedGroupSelect',
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<div data-testid="group-select-stub" />',
    methods: {
      prepare: vi.fn().mockResolvedValue(undefined),
    },
  },
}))

const AUTHOR_CAPABILITIES: ManagementCapabilities = {
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: true,
  decideTests: false,
  decideApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: false,
  viewCollaborationWorkItems: false,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: true,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
  manageAssetLibrary: true,
  manageLegalHold: false,
}

function patchSession(roles: string[], capabilities?: ManagementCapabilities) {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000005',
      displayName: 'Actor',
      email: 'actor@example.com',
      authSource: 'LOCAL',
      roles,
      authorizedGroupCodes: ['HQ', 'RETAIL', 'WEALTH'],
      defaultRoute: 'route.dashboard-home',
      visibleRoutes: ['route.content-module-management'],
      capabilities,
      expiresAt: '2099-01-01T00:00:00Z',
    },
  })
}

type CreateDialogExposed = {
  form: {
    groupCode: string
    moduleCode: string
    name: string
    sharedGroupCodes: string[]
  }
  sharedGroupSelectOptions: { value: string }[]
  handleSubmit: () => Promise<void>
}

describe('ContentModuleCreateDialog sharedGroupCodes (CE-U10)', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    ensureGroupCatalog.mockClear()
  })

  it('SGC-001: GROUP_ADMIN create payload includes selected sharedGroupCodes', async () => {
    patchSession(['GROUP_ADMIN'])
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleCreateDialog, {
      props: { modelValue: true },
      global: { plugins: [pinia, i18n, ElementPlus] },
    })

    const store = useContentModulesStore()
    const createModule = vi.spyOn(store, 'createModule').mockResolvedValue({
      moduleId: 'mod-1',
      moduleCode: 'MOD-LOAN',
      groupCode: 'HQ',
      name: 'Loan',
      sharedGroupCodes: ['RETAIL'],
      versions: [],
      reviewHistory: [],
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Share to groups')
    expect(wrapper.find('[data-testid="content-module-shared-groups-select"]').exists()).toBe(true)

    const vm = wrapper.vm as unknown as CreateDialogExposed
    vm.form.groupCode = 'HQ'
    vm.form.moduleCode = 'MOD-LOAN'
    vm.form.name = 'Loan'
    vm.form.sharedGroupCodes = ['RETAIL']

    await vm.handleSubmit()
    await flushPromises()

    expect(createModule).toHaveBeenCalledWith(
      expect.objectContaining({
        groupCode: 'HQ',
        moduleCode: 'MOD-LOAN',
        sharedGroupCodes: ['RETAIL'],
      }),
    )
  })

  it('SGC-002: TEMPLATE_AUTHOR does not see Share to groups and sends empty list', async () => {
    patchSession(['TEMPLATE_AUTHOR'], AUTHOR_CAPABILITIES)
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleCreateDialog, {
      props: { modelValue: true },
      global: { plugins: [pinia, i18n, ElementPlus] },
    })

    const store = useContentModulesStore()
    const createModule = vi.spyOn(store, 'createModule').mockResolvedValue({
      moduleId: 'mod-1',
      moduleCode: 'MOD-LOAN',
      groupCode: 'HQ',
      name: 'Loan',
      sharedGroupCodes: [],
      versions: [],
      reviewHistory: [],
    })

    await flushPromises()

    expect(wrapper.text()).not.toContain('Share to groups')
    expect(wrapper.find('[data-testid="content-module-shared-groups-select"]').exists()).toBe(false)

    const vm = wrapper.vm as unknown as CreateDialogExposed
    vm.form.groupCode = 'HQ'
    vm.form.moduleCode = 'MOD-LOAN'
    vm.form.name = 'Loan'
    vm.form.sharedGroupCodes = ['RETAIL']

    await vm.handleSubmit()
    await flushPromises()

    expect(createModule).toHaveBeenCalledWith(
      expect.objectContaining({
        sharedGroupCodes: [],
      }),
    )
  })

  it('SGC-007: shared options exclude the owning groupCode', async () => {
    patchSession(['GROUP_ADMIN'])
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleCreateDialog, {
      props: { modelValue: true },
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    const vm = wrapper.vm as unknown as CreateDialogExposed
    vm.form.groupCode = 'HQ'
    await flushPromises()

    const optionValues = vm.sharedGroupSelectOptions.map((o) => o.value)
    expect(optionValues).not.toContain('HQ')
    expect(optionValues).toEqual(expect.arrayContaining(['RETAIL', 'WEALTH']))
  })
})
