import {
  canAccessApiPolicyManagement,
  canAccessAuditConsole,
  canAccessContentModuleManagement,
  canAccessTemplateManagement,
  canAuthorContentModules,
  canAuthorTemplates,
  canDecideContentModuleReviews,
  canManageContentModuleLifecycle,
  canReviewMasters,
  canUploadMasters,
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
 * Client-side capability guard per logical route (SOR-K05).
 * Backend {@code visibleRoutes} remains authoritative; this fails closed when
 * session capabilities are present and contradict route access.
 */
export const ROUTE_CAPABILITY_GUARD: Record<RouteKey, (context: CapabilityContext) => boolean> = {
  [ROUTE_KEYS.dashboardHome]: () => true,
  [ROUTE_KEYS.globalGovernanceHome]: () => true,
  [ROUTE_KEYS.groupGovernanceHome]: () => true,
  [ROUTE_KEYS.templateAuthoringHome]: () => true,
  [ROUTE_KEYS.masterManagement]: (context) =>
    strictRouteCapability(
      context,
      [canUploadMasters, canReviewMasters],
      (roles) => canUploadMasters({ roles }) || canReviewMasters({ roles }),
    ),
  [ROUTE_KEYS.templateManagement]: (context) =>
    strictRouteCapability(context, [canAuthorTemplates], canAccessTemplateManagement),
  [ROUTE_KEYS.contentModuleManagement]: (context) =>
    strictRouteCapability(
      context,
      [canAuthorContentModules, canDecideContentModuleReviews, canManageContentModuleLifecycle],
      canAccessContentModuleManagement,
    ),
  [ROUTE_KEYS.apiPolicyManagement]: canAccessApiPolicyManagement,
  [ROUTE_KEYS.auditConsole]: canAccessAuditConsole,
  [ROUTE_KEYS.identityAdministration]: (context) => isGlobalAdmin(context.roles),
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
    return true
  }
  return guard(sessionContext(session))
}
