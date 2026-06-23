export const MANAGEMENT_ROLE_VALUES = [
  'GLOBAL_ADMIN',
  'GROUP_ADMIN',
  'MASTER_DESIGNER',
  'TEMPLATE_AUTHOR',
  'TEMPLATE_TESTER',
  'TEMPLATE_APPROVER',
  'AUDIT_ADMIN',
] as const

export type ManagementRole = (typeof MANAGEMENT_ROLE_VALUES)[number]

export type GroupDimension = 'BUSINESS_LINE' | 'DEPARTMENT'

export interface ManagementUserView {
  id: string
  username: string
  displayName: string
  email: string
  authSource: string
  roles: ManagementRole[]
  authorizedGroupCodes: string[]
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface BusinessGroupView {
  id: string
  groupCode: string
  displayName: string
  dimension: GroupDimension
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface PageView<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateUserRequest {
  username: string
  displayName: string
  email: string
  initialPassword: string
  roles: ManagementRole[]
  authorizedGroupCodes: string[]
}

export interface UpdateUserRequest {
  displayName: string
  email: string
  roles: ManagementRole[]
  authorizedGroupCodes: string[]
}

export interface ResetPasswordRequest {
  newPassword: string
}

export interface CreateGroupRequest {
  groupCode: string
  displayName: string
  dimension: GroupDimension
}

export interface UpdateGroupRequest {
  displayName: string
}

export interface UserQuery {
  group?: string
  role?: string
  page?: number
  size?: number
}

export interface GroupQuery {
  page?: number
  size?: number
}
