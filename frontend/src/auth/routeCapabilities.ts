import {
  canAccessApiPolicyManagement,
  canAccessAuditConsole,
  canAccessContentModuleManagement,
  canAccessTemplateManagement,
  canAuthorContentModules,
  canAuthorTemplates,
  canDecideApprovals,
  canDecideContentModuleReviews,
  canDecideTests,
  canManageAssetLibrary,
  canManageContentModuleLifecycle,
  canManageLegalHold,
  canPublishTemplates,
  canReviewMasters,
  canUploadMasters,
  MANAGEMENT_ROLES,
  sessionContext,
  type CapabilityContext,
} from '@/auth/roles'
import { isGlobalAdmin } from '@/auth/identityRoles'
import { ROUTE_KEYS, type RouteKey } from '@/routing/routeKeys'
import type { ManagementSession } from '@/types/session'

function strictRouteCapability(
  context: CapabilityContext,
  checks: Array<(ctx: CapabilityContext) => boolean>,
  roleFallback: (roles: string[]) => boolean,
): boolean {
  if (context.capabilities) {
    return checks.some((check) => check(context))
  }
  return roleFallback(context.roles)
}

/**
 * Mirrors backend {@code RouteVisibilityService.resolveVisibleRoutes} for
 * defense-in-depth role checks (OPT-G5). Backend {@code visibleRoutes} remains
 * authoritative; this never widens access beyond what roles would grant.
 */
function rolesAllowRoute(routeKey: string, roles: string[]): boolean {
  const roleSet = new Set(roles)
  if (roleSet.has(MANAGEMENT_ROLES.GLOBAL_ADMIN)) {
    return (
      routeKey === ROUTE_KEYS.dashboardHome ||
      routeKey === ROUTE_KEYS.globalGovernanceHome ||
      routeKey === ROUTE_KEYS.masterManagement ||
      routeKey === ROUTE_KEYS.templateManagement ||
      routeKey === ROUTE_KEYS.contentModuleManagement ||
      routeKey === ROUTE_KEYS.assetLibraryManagement ||
      routeKey === ROUTE_KEYS.apiPolicyManagement ||
      routeKey === ROUTE_KEYS.auditConsole ||
      routeKey === ROUTE_KEYS.identityAdministration ||
      routeKey === ROUTE_KEYS.legalHoldAdministration ||
      routeKey === ROUTE_KEYS.templateAuthoringHome
    )
  }
  // Backend short-circuits AUDIT_ADMIN to audit-only (even when combined with other roles).
  if (roleSet.has(MANAGEMENT_ROLES.AUDIT_ADMIN)) {
    return routeKey === ROUTE_KEYS.auditConsole
  }
  const allowed = new Set<string>()
  if (roleSet.has(MANAGEMENT_ROLES.GROUP_ADMIN)) {
    allowed.add(ROUTE_KEYS.dashboardHome)
    allowed.add(ROUTE_KEYS.groupGovernanceHome)
    allowed.add(ROUTE_KEYS.masterManagement)
    allowed.add(ROUTE_KEYS.templateManagement)
    allowed.add(ROUTE_KEYS.contentModuleManagement)
    allowed.add(ROUTE_KEYS.assetLibraryManagement)
    allowed.add(ROUTE_KEYS.apiPolicyManagement)
    allowed.add(ROUTE_KEYS.auditConsole)
    allowed.add(ROUTE_KEYS.identityAdministration)
    allowed.add(ROUTE_KEYS.templateAuthoringHome)
  }
  if (roleSet.has(MANAGEMENT_ROLES.MASTER_DESIGNER)) {
    allowed.add(ROUTE_KEYS.dashboardHome)
    allowed.add(ROUTE_KEYS.masterManagement)
    allowed.add(ROUTE_KEYS.templateManagement)
    allowed.add(ROUTE_KEYS.contentModuleManagement)
    allowed.add(ROUTE_KEYS.assetLibraryManagement)
  }
  if (roleSet.has(MANAGEMENT_ROLES.TEMPLATE_AUTHOR)) {
    allowed.add(ROUTE_KEYS.dashboardHome)
    allowed.add(ROUTE_KEYS.templateAuthoringHome)
    allowed.add(ROUTE_KEYS.templateManagement)
    allowed.add(ROUTE_KEYS.contentModuleManagement)
    allowed.add(ROUTE_KEYS.assetLibraryManagement)
  }
  if (roleSet.has(MANAGEMENT_ROLES.TEMPLATE_TESTER)) {
    allowed.add(ROUTE_KEYS.dashboardHome)
    allowed.add(ROUTE_KEYS.templateManagement)
    allowed.add(ROUTE_KEYS.assetLibraryManagement)
  }
  if (roleSet.has(MANAGEMENT_ROLES.TEMPLATE_APPROVER)) {
    allowed.add(ROUTE_KEYS.dashboardHome)
    allowed.add(ROUTE_KEYS.templateManagement)
    allowed.add(ROUTE_KEYS.contentModuleManagement)
    allowed.add(ROUTE_KEYS.assetLibraryManagement)
  }
  return allowed.has(routeKey)
}

/**
 * Client-side capability + role guard per logical route (SOR-K05 / OPT-G5).
 * Backend {@code visibleRoutes} remains authoritative; client checks are
 * defense-in-depth and fail closed on unknown routes.
 */
const ROUTE_CAPABILITY_GUARD: Record<RouteKey, (context: CapabilityContext) => boolean> = {
  [ROUTE_KEYS.dashboardHome]: (context) => rolesAllowRoute(ROUTE_KEYS.dashboardHome, context.roles),
  [ROUTE_KEYS.globalGovernanceHome]: (context) =>
    rolesAllowRoute(ROUTE_KEYS.globalGovernanceHome, context.roles),
  [ROUTE_KEYS.groupGovernanceHome]: (context) =>
    rolesAllowRoute(ROUTE_KEYS.groupGovernanceHome, context.roles),
  [ROUTE_KEYS.templateAuthoringHome]: (context) =>
    rolesAllowRoute(ROUTE_KEYS.templateAuthoringHome, context.roles),
  [ROUTE_KEYS.masterManagement]: (context) =>
    strictRouteCapability(
      context,
      [canUploadMasters, canReviewMasters],
      (roles) => canUploadMasters({ roles }) || canReviewMasters({ roles }),
    ),
  // Testers/approvers/publishers must reach template hub + /dev decision UI even when
  // authorTemplates is false (backend still lists route.template-management for them).
  [ROUTE_KEYS.templateManagement]: (context) =>
    strictRouteCapability(
      context,
      [canAuthorTemplates, canDecideTests, canDecideApprovals, canPublishTemplates],
      (roles) =>
        canAccessTemplateManagement(roles) ||
        roles.some((role) =>
          (
            [
              MANAGEMENT_ROLES.TEMPLATE_TESTER,
              MANAGEMENT_ROLES.TEMPLATE_APPROVER,
              MANAGEMENT_ROLES.MASTER_DESIGNER,
            ] as string[]
          ).includes(role),
        ),
    ),
  [ROUTE_KEYS.contentModuleManagement]: (context) =>
    strictRouteCapability(
      context,
      [canAuthorContentModules, canDecideContentModuleReviews, canManageContentModuleLifecycle],
      canAccessContentModuleManagement,
    ),
  [ROUTE_KEYS.assetLibraryManagement]: (context) =>
    strictRouteCapability(context, [canManageAssetLibrary], (roles) =>
      canManageAssetLibrary({ roles }),
    ),
  [ROUTE_KEYS.apiPolicyManagement]: canAccessApiPolicyManagement,
  [ROUTE_KEYS.auditConsole]: canAccessAuditConsole,
  [ROUTE_KEYS.identityAdministration]: (context) =>
    isGlobalAdmin(context.roles) || context.roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN),
  [ROUTE_KEYS.legalHoldAdministration]: canManageLegalHold,
}

export function canAccessRouteWithCapability(
  routeKey: string,
  session: ManagementSession | null,
): boolean {
  if (!session?.visibleRoutes.includes(routeKey)) {
    return false
  }
  const guard = ROUTE_CAPABILITY_GUARD[routeKey as RouteKey]
  if (!guard) {
    // Fail-closed: unknown logical routes are denied (OPT-G5).
    return false
  }
  return guard(sessionContext(session))
}
