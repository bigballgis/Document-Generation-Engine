import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import UserManagementPanel from '@/views/identity/UserManagementPanel.vue'
import en from '@/i18n/locales/en'
import * as identityApi from '@/api/identity'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ManagementSession } from '@/types/session'
import type { ManagementUserView } from '@/types/identity'

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

const sampleUser: ManagementUserView = {
  id: 'user-1',
  username: '10000001',
  displayName: 'Retail Operator',
  email: 'operator@example.com',
  authSource: 'LOCAL',
  roles: ['TEMPLATE_AUTHOR'],
  authorizedGroupCodes: ['RETAIL'],
  enabled: true,
  createdAt: '2026-06-23T10:00:00Z',
  updatedAt: '2026-06-23T10:00:00Z',
}

function userPage(content: (typeof sampleUser)[]) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 }
}

function patchSession(roles: string[], authorizedGroupCodes: string[]) {
  const sessionStore = useSessionStore()
  const session: ManagementSession = {
    username: '10000000',
    displayName: 'Admin',
    email: 'admin@example.com',
    authSource: 'LOCAL',
    roles,
    authorizedGroupCodes,
    defaultRoute: ROUTE_KEYS.identityAdministration,
    visibleRoutes: [ROUTE_KEYS.identityAdministration],
    expiresAt: new Date().toISOString(),
  }
  sessionStore.$patch({ accessToken: 'token', session })
}

function mountPanel() {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(UserManagementPanel, {
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('UserManagementPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(identityApi.listUsers).mockResolvedValue(userPage([sampleUser]))
    vi.mocked(identityApi.listGroups).mockResolvedValue({
      content: [
        {
          id: 'group-1',
          groupCode: 'RETAIL',
          displayName: 'Retail',
          dimension: 'BUSINESS_LINE',
          enabled: true,
          createdAt: '2026-06-23T10:00:00Z',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('renders users after load', async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('10000001')
    expect(wrapper.text()).toContain('Retail Operator')
  })

  it('shows the delete action for global admins', async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('.delete-action').exists()).toBe(true)
  })

  it('hides the delete action for group admins', async () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL'])
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('.delete-action').exists()).toBe(false)
  })

  it('restricts role filter options to operational roles for group admins', async () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL'])
    mountPanel()
    await flushPromises()

    const optionText = document.body.textContent ?? ''
    expect(optionText).toContain('Template author')
    expect(optionText).not.toContain('Global administrator')
    expect(optionText).not.toContain('Audit administrator')
  })

  it('exposes administrative roles to global admins', async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    mountPanel()
    await flushPromises()

    const optionText = document.body.textContent ?? ''
    expect(optionText).toContain('Global administrator')
  })

  it('creates a user through the store', async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    vi.mocked(identityApi.createUser).mockResolvedValue({
      ...sampleUser,
      id: 'user-2',
      username: '10000002',
    })
    const wrapper = mountPanel()
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      openCreate: () => void
      form: {
        username: string
        displayName: string
        email: string
        initialPassword: string
        roles: string[]
        authorizedGroupCodes: string[]
      }
      submitForm: () => Promise<void>
    }
    vm.openCreate()
    await flushPromises()
    vm.form.username = '10000002'
    vm.form.displayName = 'Second Operator'
    vm.form.email = 'second@example.com'
    vm.form.initialPassword = 'Sup3rSecret!42'
    vm.form.roles = ['TEMPLATE_AUTHOR']
    vm.form.authorizedGroupCodes = ['RETAIL']
    await flushPromises()
    await vm.submitForm()
    await flushPromises()

    expect(identityApi.createUser).toHaveBeenCalledWith({
      username: '10000002',
      displayName: 'Second Operator',
      email: 'second@example.com',
      initialPassword: 'Sup3rSecret!42',
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
    })
  }, 15000)

  it('surfaces backend error codes on load failure', async () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL'])
    vi.mocked(identityApi.listUsers).mockRejectedValue({
      isAxiosError: true,
      response: {
        data: { error: { messageKey: 'identity.error.loadUsers' } },
      },
    })
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('Unable to load users.')
  })
})
