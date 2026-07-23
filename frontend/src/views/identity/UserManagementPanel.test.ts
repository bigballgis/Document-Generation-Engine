import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'
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
  roles: ['DOCUMENT_AUTHOR'],
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

async function mountPanel() {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/entitlement/users', component: { template: '<div />' } },
      { path: '/entitlement/groups', component: { template: '<div />' } },
    ],
  })
  await router.push('/entitlement/users')
  await router.isReady()
  return mount(UserManagementPanel, {
    global: { plugins: [i18n, ElementPlus, router] },
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
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('10000001')
    expect(wrapper.text()).toContain('Retail Operator')
  })

  it('uses shared Edit/More actions and group EntityLink (BDD-SYS-NORM-W1-007/014 / BDD-PQH-N22-011)', async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.find('[data-testid="table-edit-more-actions"]').exists()).toBe(true)
    expect(wrapper.find('.table-edit-more-actions__edit').exists()).toBe(true)
    expect(wrapper.text()).toContain('Edit')
    expect(wrapper.text()).toContain('More')
    expect(wrapper.find('.entity-link-cell__link').exists()).toBe(true)
    expect(wrapper.text()).toContain('RETAIL')
  })

  it('shows delete action for global admins in more menu', { timeout: 20000 }, async () => {
    patchSession(['GLOBAL_ADMIN'], ['RETAIL'])
    const wrapper = await mountPanel()
    await flushPromises()

    const actions = wrapper.find('[data-testid="table-edit-more-actions"]')
    expect(actions.exists()).toBe(true)
    const moreButton = actions.findAll('button').find((button) => button.text().includes('More'))
    expect(moreButton).toBeDefined()
    await moreButton!.trigger('click')
    await flushPromises()

    expect(document.body.querySelector('.delete-action')).toBeTruthy()
  })

  it('hides delete action for group admins', async () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL'])
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.find('.delete-action').exists()).toBe(false)
  }, 15000)

  it('restricts role filter options to operational roles for group admins', async () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL'])
    await mountPanel()
    await flushPromises()

    const optionText = document.body.textContent ?? ''
    expect(optionText).toContain('Document author')
    expect(optionText).not.toContain('Global administrator')
    expect(optionText).not.toContain('Audit administrator')
    expect(optionText).not.toContain('Template author')
    expect(optionText).not.toContain('Letterhead designer')
    expect(optionText).not.toContain('Template approver')
  })

  it('exposes administrative roles to global admins', async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    await mountPanel()
    await flushPromises()

    const optionText = document.body.textContent ?? ''
    expect(optionText).toContain('Global administrator')
  })

  it('creates a user through the store', { timeout: 30_000 }, async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    vi.mocked(identityApi.createUser).mockResolvedValue({
      ...sampleUser,
      id: 'user-2',
      username: '10000002',
    })
    const wrapper = await mountPanel()
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
    vm.form.roles = ['DOCUMENT_AUTHOR']
    vm.form.authorizedGroupCodes = ['RETAIL']
    await flushPromises()
    await vm.submitForm()
    await flushPromises()

    expect(identityApi.createUser).toHaveBeenCalledWith({
      username: '10000002',
      displayName: 'Second Operator',
      email: 'second@example.com',
      initialPassword: 'Sup3rSecret!42',
      roles: ['DOCUMENT_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
    })
  })

  it('surfaces backend error codes on load failure', async () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL'])
    vi.mocked(identityApi.listUsers).mockRejectedValue(
      axiosEnvelopeError(403, 'identity.error.loadUsers', {
        code: 'ACCESS_DENIED',
        category: 'AUTHORIZATION',
        message: 'Not allowed.',
      }),
    )
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('Unable to load users.')
  })

  it('LR-C9-B: empty user list shows create CTA for admins', async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    vi.mocked(identityApi.listUsers).mockResolvedValue(userPage([]))
    const wrapper = await mountPanel()
    await flushPromises()

    const emptyActions = wrapper.find('[data-testid="empty-state-actions"]')
    expect(emptyActions.exists()).toBe(true)
    expect(emptyActions.text()).toContain('Create user')
  })

  it('LR-C9-A: LoadErrorPanel retry reloads users', async () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    vi.mocked(identityApi.listUsers)
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce(userPage([sampleUser]))
    const wrapper = await mountPanel()
    await flushPromises()

    const errorPanel = wrapper.findComponent({ name: 'LoadErrorPanel' })
    expect(errorPanel.exists()).toBe(true)
    await errorPanel.vm.$emit('retry')
    await flushPromises()

    expect(identityApi.listUsers).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('10000001')
  })
})
