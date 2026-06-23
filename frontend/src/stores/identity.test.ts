import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as identityApi from '@/api/identity'
import { useIdentityStore } from '@/stores/identity'
import type { BusinessGroupView, ManagementUserView } from '@/types/identity'

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

const user: ManagementUserView = {
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

const group: BusinessGroupView = {
  id: 'group-1',
  groupCode: 'RETAIL',
  displayName: 'Retail banking',
  dimension: 'BUSINESS_LINE',
  enabled: true,
  createdAt: '2026-06-23T10:00:00Z',
  updatedAt: '2026-06-23T10:00:00Z',
}

function userPage(content: ManagementUserView[]) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 }
}

function groupPage(content: BusinessGroupView[]) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 }
}

describe('identity store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads users into state', async () => {
    vi.mocked(identityApi.listUsers).mockResolvedValue(userPage([user]))
    const store = useIdentityStore()

    await store.fetchUsers()

    expect(store.users).toHaveLength(1)
    expect(store.users[0]?.username).toBe('10000001')
    expect(store.usersTotal).toBe(1)
  })

  it('records backend error message key on user load failure', async () => {
    vi.mocked(identityApi.listUsers).mockRejectedValue({
      isAxiosError: true,
      response: { data: { error: { messageKey: 'api.error.forbidden.userManagementNotAllowed' } } },
    })
    const store = useIdentityStore()

    await expect(store.fetchUsers()).rejects.toBeTruthy()
    expect(store.lastUserErrorMessageKey).toBe('api.error.forbidden.userManagementNotAllowed')
  })

  it('prepends created user', async () => {
    vi.mocked(identityApi.listUsers).mockResolvedValue(userPage([user]))
    vi.mocked(identityApi.createUser).mockResolvedValue({ ...user, id: 'user-2', username: '10000002' })
    const store = useIdentityStore()
    await store.fetchUsers()

    await store.createUser({
      username: '10000002',
      displayName: 'Another',
      email: 'a@example.com',
      initialPassword: 'Sup3rSecret!42',
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
    })

    expect(store.users[0]?.id).toBe('user-2')
    expect(store.users).toHaveLength(2)
  })

  it('replaces updated user after enable/disable', async () => {
    vi.mocked(identityApi.listUsers).mockResolvedValue(userPage([user]))
    vi.mocked(identityApi.disableUser).mockResolvedValue({ ...user, enabled: false })
    const store = useIdentityStore()
    await store.fetchUsers()

    await store.setUserEnabled('user-1', false)

    expect(store.users[0]?.enabled).toBe(false)
  })

  it('removes deleted user from state', async () => {
    vi.mocked(identityApi.listUsers).mockResolvedValue(userPage([user]))
    vi.mocked(identityApi.deleteUser).mockResolvedValue({ ...user, enabled: false })
    const store = useIdentityStore()
    await store.fetchUsers()

    await store.deleteUser('user-1')

    expect(store.users).toHaveLength(0)
  })

  it('loads groups into state', async () => {
    vi.mocked(identityApi.listGroups).mockResolvedValue(groupPage([group]))
    const store = useIdentityStore()

    await store.fetchGroups()

    expect(store.groups).toHaveLength(1)
    expect(store.groups[0]?.groupCode).toBe('RETAIL')
  })

  it('prepends created group', async () => {
    vi.mocked(identityApi.listGroups).mockResolvedValue(groupPage([group]))
    vi.mocked(identityApi.createGroup).mockResolvedValue({ ...group, id: 'group-2', groupCode: 'CORPORATE' })
    const store = useIdentityStore()
    await store.fetchGroups()

    await store.createGroup({ groupCode: 'CORPORATE', displayName: 'Corporate', dimension: 'DEPARTMENT' })

    expect(store.groups[0]?.groupCode).toBe('CORPORATE')
  })

  it('records group error message key on failure', async () => {
    vi.mocked(identityApi.createGroup).mockRejectedValue({
      isAxiosError: true,
      response: { data: { error: { messageKey: 'api.error.conflict.groupCodeAlreadyExists' } } },
    })
    const store = useIdentityStore()

    await expect(
      store.createGroup({ groupCode: 'RETAIL', displayName: 'Dup', dimension: 'BUSINESS_LINE' }),
    ).rejects.toBeTruthy()
    expect(store.lastGroupErrorMessageKey).toBe('api.error.conflict.groupCodeAlreadyExists')
  })
})
