import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createRouter, createMemoryHistory } from 'vue-router'
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

async function mountPanel(query: Record<string, string> = {}) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/entitlement/groups', component: { template: '<div />' } }],
  })
  await router.push({ path: '/entitlement/groups', query })
  await router.isReady()
  return mount(GroupManagementPanel, {
    global: { plugins: [i18n, ElementPlus, router] },
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
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('RETAIL')
    expect(wrapper.text()).toContain('Retail banking')
  })

  it('filters groups by search query on group code and display name', { timeout: 20000 }, async () => {
    patchSession(['GLOBAL_ADMIN'])
    vi.mocked(identityApi.listGroups).mockResolvedValue({
      content: [
        sampleGroup,
        {
          ...sampleGroup,
          id: 'group-2',
          groupCode: 'CORPORATE',
          displayName: 'Corporate banking',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })
    const wrapper = await mountPanel()
    await flushPromises()

    const vm = wrapper.vm as unknown as { searchQuery: string }
    vm.searchQuery = 'corporate'
    await flushPromises()

    expect(wrapper.text()).toContain('Corporate banking')
    expect(wrapper.text()).not.toContain('Retail banking')
  })

  it('uses shared Edit/More actions primitive (BDD-SYS-NORM-W1-007)', async () => {
    patchSession(['GLOBAL_ADMIN'])
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.find('[data-testid="table-edit-more-actions"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Edit')
    expect(wrapper.text()).toContain('More')
  })

  it('shows write controls for global admins', async () => {
    patchSession(['GLOBAL_ADMIN'])
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('Create group')
    expect(wrapper.text()).not.toContain('You can view groups within your authorized scope')
  })

  it('hides write controls and shows read-only hint for group admins', async () => {
    patchSession(['GROUP_ADMIN'])
    const wrapper = await mountPanel()
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
    const wrapper = await mountPanel()
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

  it('P1-2-A: list load failure shows only LoadErrorPanel without el-alert dual track', async () => {
    patchSession(['GLOBAL_ADMIN'])
    vi.mocked(identityApi.listGroups).mockRejectedValue(new Error('load failed'))
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.findComponent({ name: 'LoadErrorPanel' }).exists()).toBe(true)
    expect(wrapper.find('.el-alert').exists()).toBe(false)
    expect(wrapper.find('.panel-alert').exists()).toBe(false)
  })

  it('P1-2-B: LoadErrorPanel retry reloads groups', async () => {
    patchSession(['GLOBAL_ADMIN'])
    vi.mocked(identityApi.listGroups)
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce({
        content: [sampleGroup],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
    const wrapper = await mountPanel()
    await flushPromises()

    const errorPanel = wrapper.findComponent({ name: 'LoadErrorPanel' })
    expect(errorPanel.exists()).toBe(true)
    await errorPanel.vm.$emit('retry')
    await flushPromises()

    expect(identityApi.listGroups).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('RETAIL')
    expect(wrapper.findComponent({ name: 'LoadErrorPanel' }).exists()).toBe(false)
  })

  it('LR-C9-B: empty catalog shows create CTA for global admins', async () => {
    patchSession(['GLOBAL_ADMIN'])
    vi.mocked(identityApi.listGroups).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    const wrapper = await mountPanel()
    await flushPromises()

    const emptyActions = wrapper.find('[data-testid="empty-state-actions"]')
    expect(emptyActions.exists()).toBe(true)
    expect(emptyActions.text()).toContain('Create group')
  })

  it('LR-C9-B: empty catalog hides create CTA for group admins', async () => {
    patchSession(['GROUP_ADMIN'])
    vi.mocked(identityApi.listGroups).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    const wrapper = await mountPanel()
    await flushPromises()

    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('No groups available.')
  })
})

