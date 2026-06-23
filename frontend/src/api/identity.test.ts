import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as identityApi from '@/api/identity'
import type { CreateUserRequest, ManagementUserView, UpdateUserRequest } from '@/types/identity'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
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

const sampleGroup = {
  id: 'group-1',
  groupCode: 'RETAIL',
  displayName: 'Retail banking',
  dimension: 'BUSINESS_LINE',
  enabled: true,
  createdAt: '2026-06-23T10:00:00Z',
  updatedAt: '2026-06-23T10:00:00Z',
}

function pageEnvelope<T>(content: T[]) {
  return {
    data: {
      metadata: {},
      result: { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 },
    },
  }
}

describe('identity API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
    vi.mocked(http.put).mockReset()
    vi.mocked(http.delete).mockReset()
  })

  it('lists users with group, role and pagination params', async () => {
    vi.mocked(http.get).mockResolvedValue(pageEnvelope([sampleUser]))

    const page = await identityApi.listUsers({ group: 'RETAIL', role: 'TEMPLATE_AUTHOR', page: 0, size: 20 })

    expect(http.get).toHaveBeenCalledWith('/users', {
      params: { group: 'RETAIL', role: 'TEMPLATE_AUTHOR', page: 0, size: 20 },
    })
    expect(page.content[0]?.username).toBe('10000001')
    expect(page.totalElements).toBe(1)
  })

  it('omits empty user filters', async () => {
    vi.mocked(http.get).mockResolvedValue(pageEnvelope([sampleUser]))

    await identityApi.listUsers({ page: 1, size: 20 })

    expect(http.get).toHaveBeenCalledWith('/users', { params: { page: 1, size: 20 } })
  })

  it('gets a single user', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: { metadata: {}, result: sampleUser } })

    const user = await identityApi.getUser('user-1')

    expect(http.get).toHaveBeenCalledWith('/users/user-1')
    expect(user.id).toBe('user-1')
  })

  it('creates a user', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { metadata: {}, result: sampleUser } })
    const body: CreateUserRequest = {
      username: '10000001',
      displayName: 'Retail Operator',
      email: 'operator@example.com',
      initialPassword: 'Sup3rSecret!42',
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
    }

    const created = await identityApi.createUser(body)

    expect(http.post).toHaveBeenCalledWith('/users', body)
    expect(created.username).toBe('10000001')
  })

  it('updates a user', async () => {
    vi.mocked(http.put).mockResolvedValue({ data: { metadata: {}, result: sampleUser } })
    const body: UpdateUserRequest = {
      displayName: 'Retail Operator',
      email: 'operator@example.com',
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
    }

    await identityApi.updateUser('user-1', body)

    expect(http.put).toHaveBeenCalledWith('/users/user-1', body)
  })

  it('disables and enables a user', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { metadata: {}, result: sampleUser } })

    await identityApi.disableUser('user-1')
    expect(http.post).toHaveBeenCalledWith('/users/user-1/disable')

    await identityApi.enableUser('user-1')
    expect(http.post).toHaveBeenCalledWith('/users/user-1/enable')
  })

  it('resets a user password', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { metadata: {}, result: sampleUser } })

    await identityApi.resetUserPassword('user-1', 'BrandNewSecret9')

    expect(http.post).toHaveBeenCalledWith('/users/user-1/reset-password', {
      newPassword: 'BrandNewSecret9',
    })
  })

  it('deletes a user', async () => {
    vi.mocked(http.delete).mockResolvedValue({ data: { metadata: {}, result: sampleUser } })

    await identityApi.deleteUser('user-1')

    expect(http.delete).toHaveBeenCalledWith('/users/user-1')
  })

  it('surfaces backend error codes from envelope', async () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          metadata: {},
          error: {
            code: 'USERNAME_ALREADY_EXISTS',
            message: 'Username already exists.',
            messageKey: 'api.error.conflict.usernameAlreadyExists',
          },
        },
      },
    }
    vi.mocked(http.post).mockRejectedValue(error)

    await expect(
      identityApi.createUser({
        username: '10000001',
        displayName: 'x',
        email: 'x@example.com',
        initialPassword: 'Sup3rSecret!42',
        roles: ['TEMPLATE_AUTHOR'],
        authorizedGroupCodes: ['RETAIL'],
      }),
    ).rejects.toMatchObject({
      response: { data: { error: { code: 'USERNAME_ALREADY_EXISTS' } } },
    })
  })

  it('lists groups with pagination params', async () => {
    vi.mocked(http.get).mockResolvedValue(pageEnvelope([sampleGroup]))

    const page = await identityApi.listGroups({ page: 0, size: 20 })

    expect(http.get).toHaveBeenCalledWith('/groups', { params: { page: 0, size: 20 } })
    expect(page.content[0]?.groupCode).toBe('RETAIL')
  })

  it('gets a single group', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: { metadata: {}, result: sampleGroup } })

    const group = await identityApi.getGroup('group-1')

    expect(http.get).toHaveBeenCalledWith('/groups/group-1')
    expect(group.groupCode).toBe('RETAIL')
  })

  it('creates a group', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { metadata: {}, result: sampleGroup } })
    const body = { groupCode: 'RETAIL', displayName: 'Retail banking', dimension: 'BUSINESS_LINE' as const }

    await identityApi.createGroup(body)

    expect(http.post).toHaveBeenCalledWith('/groups', body)
  })

  it('updates group display name only', async () => {
    vi.mocked(http.put).mockResolvedValue({ data: { metadata: {}, result: sampleGroup } })

    await identityApi.updateGroup('group-1', 'Retail banking renamed')

    expect(http.put).toHaveBeenCalledWith('/groups/group-1', { displayName: 'Retail banking renamed' })
  })

  it('disables and enables a group', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { metadata: {}, result: sampleGroup } })

    await identityApi.disableGroup('group-1')
    expect(http.post).toHaveBeenCalledWith('/groups/group-1/disable')

    await identityApi.enableGroup('group-1')
    expect(http.post).toHaveBeenCalledWith('/groups/group-1/enable')
  })
})
