import type { Schema } from '@/types/openapi'

/** Assignable management roles after ADR-0070 six-role compression. */
export const MANAGEMENT_ROLE_VALUES = [
  'GLOBAL_ADMIN',
  'GROUP_ADMIN',
  'DOCUMENT_AUTHOR',
  'TEMPLATE_TESTER',
  'LEGAL_REVIEWER',
  'AUDIT_ADMIN',
] as const

export type ManagementRole = (typeof MANAGEMENT_ROLE_VALUES)[number]

/** Retired assignable codes — must never appear in role pickers. */
export const RETIRED_MANAGEMENT_ROLE_VALUES = [
  'MASTER_DESIGNER',
  'TEMPLATE_AUTHOR',
  'TEMPLATE_APPROVER',
] as const

export type GroupDimension = 'BUSINESS_LINE' | 'DEPARTMENT'

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */
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

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */
export interface BusinessGroupView {
  id: string
  groupCode: string
  displayName: string
  dimension: GroupDimension
  enabled: boolean
  createdAt: string
  updatedAt: string
}

type OpenApiPageView = Schema<'PageView'>

export interface PageView<T> extends Omit<OpenApiPageView, 'content'> {
  content: T[]
}

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */
export interface CreateUserRequest {
  username: string
  displayName: string
  email: string
  initialPassword: string
  roles: ManagementRole[]
  authorizedGroupCodes: string[]
}

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */
export interface UpdateUserRequest {
  displayName: string
  email: string
  roles: ManagementRole[]
  authorizedGroupCodes: string[]
}

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */
export interface CreateGroupRequest {
  groupCode: string
  displayName: string
  dimension: GroupDimension
}

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */
export interface UserQuery {
  group?: string
  role?: string
  page?: number
  size?: number
}

/** Not yet modeled in `openapi-v1.yaml` (management identity admin). */
export interface GroupQuery {
  page?: number
  size?: number
}
