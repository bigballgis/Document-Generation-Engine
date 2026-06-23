import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import GroupManagementView from '@/views/identity/GroupManagementView.vue'
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

function emptyPage() {
  return { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
}

describe('GroupManagementView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(identityApi.listGroups).mockResolvedValue(emptyPage())

    const sessionStore = useSessionStore()
    const session: ManagementSession = {
      username: '10000000',
      displayName: 'Admin',
      email: 'admin@example.com',
      authSource: 'LOCAL',
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      defaultRoute: ROUTE_KEYS.identityAdministration,
      visibleRoutes: [ROUTE_KEYS.identityAdministration],
      expiresAt: new Date().toISOString(),
    }
    sessionStore.$patch({ accessToken: 'token', session })
  })

  it('renders group management without user tabs', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(GroupManagementView, {
      global: { plugins: [i18n, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Group management')
    expect(wrapper.text()).not.toContain('User management')
  })
})
