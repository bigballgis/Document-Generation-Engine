import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import GroupManagementPanel from '@/views/identity/GroupManagementPanel.vue'
import en from '@/i18n/locales/en'
import * as identityApi from '@/api/identity'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ManagementSession } from '@/types/session'

vi.mock('@/api/identity', () => ({
  listUsers: vi.fn(),
  getUser: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  disableUser: vi.fn(),
  enableUser: vi.fn(),
  resetUserPassword: vi.fn(),
  deleteUser: vi.fn(),
  listGroups: vi.fn(),
  getGroup: vi.fn(),
  createGroup: vi.fn(),
  updateGroup: vi.fn(),
  disableGroup: vi.fn(),
  enableGroup: vi.fn(),
}))

const sampleGroup = {
  id: 'group-1',
  groupCode: 'RETAIL',
  displayName: 'Retail banking',
  dimension: 'BUSINESS_LINE' as const,
  enabled: true,
  createdAt: '2026-06-23T10:00:00Z',
  updatedAt: '2026-06-23T10:00:00Z',
}

function patchSession(roles: string[]) {
  const sessionStore = useSessionStore()
  const session: ManagementSession = {
    username: '10000000',
    displayName: 'Admin',
    email: 'admin@example.com',
    authSource: 'LOCAL',
    roles,
    authorizedGroupCodes: roles.includes('GLOBAL_ADMIN') ? ['*'] : ['RETAIL'],
    defaultRoute: ROUTE_KEYS.identityAdministration,
    visibleRoutes: [ROUTE_KEYS.identityAdministration],
    expiresAt: new Date().toISOString(),
  }
  sessionStore.$patch({ accessToken: 'token', session })
}

function mountPanel() {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(GroupManagementPanel, {
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('GroupManagementPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(identityApi.listGroups).mockResolvedValue({
      content: [sampleGroup],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
  })

  it('renders groups after load', async () => {
    patchSession(['GLOBAL_ADMIN'])
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('RETAIL')
    expect(wrapper.text()).toContain('Retail banking')
  })

  it('shows write controls for global admins', async () => {
    patchSession(['GLOBAL_ADMIN'])
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('Create group')
    expect(wrapper.text()).not.toContain('You can view groups within your authorized scope')
  })

  it('hides write controls and shows read-only hint for group admins', async () => {
    patchSession(['GROUP_ADMIN'])
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Create group')
    expect(wrapper.text()).toContain('You can view groups within your authorized scope')
  })

  it('creates a group through the store', { timeout: 20000 }, async () => {
    patchSession(['GLOBAL_ADMIN'])
    vi.mocked(identityApi.createGroup).mockResolvedValue({
      ...sampleGroup,
      id: 'group-2',
      groupCode: 'CORPORATE',
    })
    const wrapper = mountPanel()
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      openCreate: () => void
      form: { groupCode: string; displayName: string; dimension: string }
      submitForm: () => Promise<void>
    }
    vm.openCreate()
    await flushPromises()
    vm.form.groupCode = 'CORPORATE'
    vm.form.displayName = 'Corporate banking'
    vm.form.dimension = 'DEPARTMENT'
    await flushPromises()
    await vm.submitForm()
    await flushPromises()

    expect(identityApi.createGroup).toHaveBeenCalledWith({
      groupCode: 'CORPORATE',
      displayName: 'Corporate banking',
      dimension: 'DEPARTMENT',
    })
  })
})
