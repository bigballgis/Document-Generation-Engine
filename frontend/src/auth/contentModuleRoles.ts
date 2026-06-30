import type { ManagementSession } from '@/types/session'
import type { ContentModuleGovernanceActorRole } from '@/types/contentModule'
import { MANAGEMENT_ROLES } from '@/auth/roles'

export function resolveContentModuleAuthorActorRole(roles: string[]): ContentModuleGovernanceActorRole | null {
  if (roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN)) {
    return 'GLOBAL_ADMIN'
  }
  if (roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN)) {
    return 'GROUP_ADMIN'
  }
  if (roles.includes(MANAGEMENT_ROLES.MASTER_DESIGNER)) {
    return 'MASTER_DESIGNER'
  }
  if (roles.includes(MANAGEMENT_ROLES.TEMPLATE_AUTHOR)) {
    return 'TEMPLATE_AUTHOR'
  }
  return null
}

export function resolveContentModuleApproverActorRole(roles: string[]): ContentModuleGovernanceActorRole | null {
  if (roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN)) {
    return 'GLOBAL_ADMIN'
  }
  if (roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN)) {
    return 'GROUP_ADMIN'
  }
  if (roles.includes(MANAGEMENT_ROLES.TEMPLATE_APPROVER)) {
    return 'APPROVER'
  }
  return null
}

export function resolveContentModuleLifecycleActorRole(roles: string[]): ContentModuleGovernanceActorRole | null {
  if (roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN)) {
    return 'GLOBAL_ADMIN'
  }
  if (roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN)) {
    return 'GROUP_ADMIN'
  }
  return null
}

export function resolveContentModuleActorId(session: ManagementSession | null): string {
  return session?.username ?? 'unknown'
}

export function latestDraftVersion(versions: Array<{ reviewState: string }>) {
  return versions.find((version) => version.reviewState === 'DRAFT')
}

export function latestSubmittedVersion(versions: Array<{ reviewState: string }>) {
  return versions.find((version) => version.reviewState === 'SUBMITTED')
}

export function hasApprovedActiveVersion(
  versions: Array<{ reviewState: string; lifecycleState?: string }>,
): boolean {
  return versions.some(
    (version) => version.reviewState === 'APPROVED' && (version.lifecycleState ?? 'ACTIVE') === 'ACTIVE',
  )
}

export function hasApprovedStoppedVersion(
  versions: Array<{ reviewState: string; lifecycleState?: string }>,
): boolean {
  return versions.some(
    (version) => version.reviewState === 'APPROVED' && version.lifecycleState === 'STOPPED',
  )
}
