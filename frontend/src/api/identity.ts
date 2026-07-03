import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  BusinessGroupView,
  CreateGroupRequest,
  CreateUserRequest,
  GroupQuery,
  ManagementUserView,
  PageView,
  UpdateUserRequest,
  UserQuery,
} from '@/types/identity'

function buildUserParams(query: UserQuery): Record<string, string | number> {
  const params: Record<string, string | number> = {}
  if (query.group) {
    params.group = query.group
  }
  if (query.role) {
    params.role = query.role
  }
  if (query.page !== undefined) {
    params.page = query.page
  }
  if (query.size !== undefined) {
    params.size = query.size
  }
  return params
}

export async function listUsers(query: UserQuery = {}): Promise<PageView<ManagementUserView>> {
  const response = await http.get<ApiEnvelope<PageView<ManagementUserView>>>('/users', {
    params: buildUserParams(query),
  })
  return unwrapEnvelope(response.data)
}

export async function getUser(id: string): Promise<ManagementUserView> {
  const response = await http.get<ApiEnvelope<ManagementUserView>>(`/users/${id}`)
  return unwrapEnvelope(response.data)
}

export async function createUser(body: CreateUserRequest): Promise<ManagementUserView> {
  const response = await http.post<ApiEnvelope<ManagementUserView>>('/users', body)
  return unwrapEnvelope(response.data)
}

export async function updateUser(id: string, body: UpdateUserRequest): Promise<ManagementUserView> {
  const response = await http.put<ApiEnvelope<ManagementUserView>>(`/users/${id}`, body)
  return unwrapEnvelope(response.data)
}

export async function disableUser(id: string): Promise<ManagementUserView> {
  const response = await http.post<ApiEnvelope<ManagementUserView>>(`/users/${id}/disable`)
  return unwrapEnvelope(response.data)
}

export async function enableUser(id: string): Promise<ManagementUserView> {
  const response = await http.post<ApiEnvelope<ManagementUserView>>(`/users/${id}/enable`)
  return unwrapEnvelope(response.data)
}

export async function resetUserPassword(
  id: string,
  newPassword: string,
): Promise<ManagementUserView> {
  const response = await http.post<ApiEnvelope<ManagementUserView>>(`/users/${id}/reset-password`, {
    newPassword,
  })
  return unwrapEnvelope(response.data)
}

export async function deleteUser(id: string): Promise<ManagementUserView> {
  const response = await http.delete<ApiEnvelope<ManagementUserView>>(`/users/${id}`)
  return unwrapEnvelope(response.data)
}

function buildGroupParams(query: GroupQuery): Record<string, string | number> {
  const params: Record<string, string | number> = {}
  if (query.page !== undefined) {
    params.page = query.page
  }
  if (query.size !== undefined) {
    params.size = query.size
  }
  return params
}

export async function listGroups(query: GroupQuery = {}): Promise<PageView<BusinessGroupView>> {
  const response = await http.get<ApiEnvelope<PageView<BusinessGroupView>>>('/groups', {
    params: buildGroupParams(query),
  })
  return unwrapEnvelope(response.data)
}

export async function getGroup(id: string): Promise<BusinessGroupView> {
  const response = await http.get<ApiEnvelope<BusinessGroupView>>(`/groups/${id}`)
  return unwrapEnvelope(response.data)
}

export async function createGroup(body: CreateGroupRequest): Promise<BusinessGroupView> {
  const response = await http.post<ApiEnvelope<BusinessGroupView>>('/groups', body)
  return unwrapEnvelope(response.data)
}

export async function updateGroup(id: string, displayName: string): Promise<BusinessGroupView> {
  const response = await http.put<ApiEnvelope<BusinessGroupView>>(`/groups/${id}`, { displayName })
  return unwrapEnvelope(response.data)
}

export async function disableGroup(id: string): Promise<BusinessGroupView> {
  const response = await http.post<ApiEnvelope<BusinessGroupView>>(`/groups/${id}/disable`)
  return unwrapEnvelope(response.data)
}

export async function enableGroup(id: string): Promise<BusinessGroupView> {
  const response = await http.post<ApiEnvelope<BusinessGroupView>>(`/groups/${id}/enable`)
  return unwrapEnvelope(response.data)
}
