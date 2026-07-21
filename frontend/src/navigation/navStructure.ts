import {
  canAccessContentModuleManagement,
  canMaintainCollaborationTimeoutConfig,
  type CapabilityContext,
} from '@/auth/roles'
import { isGlobalAdmin } from '@/auth/identityRoles'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import {
  BEHAVIOR_NAV_ITEM_SPECS,
  type BehaviorNavItemSpec,
  type NavGroupDefinition,
  type NavItemDefinition,
  type NavItemTarget,
} from '@/navigation/navCatalog'
import { NAV_GROUPS } from '@/navigation/navGroupsCatalog'

export type {
  BehaviorNavItemSpec,
  NavGroupDefinition,
  NavItemDefinition,
  NavItemTarget,
} from '@/navigation/navCatalog'

export { BEHAVIOR_NAV_ITEM_SPECS } from '@/navigation/navCatalog'
export { NAV_GROUPS } from '@/navigation/navGroupsCatalog'

function hasDashboardHomeAccess(visibleRoutes: string[]): boolean {
  return visibleRoutes.includes(ROUTE_KEYS.dashboardHome)
}

function behaviorSpecToNavItem(spec: BehaviorNavItemSpec): NavItemDefinition {
  return {
    id: spec.id,
    routeKey: ROUTE_KEYS.dashboardHome,
    path: '/dashboard',
    labelKey: spec.labelKey,
    query: spec.query,
    hash: spec.hash,
  }
}

export function buildVisibleBehaviorNavItems(
  context: CapabilityContext,
  visibleRoutes: string[],
): NavItemDefinition[] {
  if (!hasDashboardHomeAccess(visibleRoutes)) {
    return []
  }
  return BEHAVIOR_NAV_ITEM_SPECS.filter((spec) => spec.isVisible(context, visibleRoutes)).map(
    behaviorSpecToNavItem,
  )
}

export function resolveNavItemTarget(item: NavItemDefinition): NavItemTarget {
  const target: NavItemTarget = { path: item.path }
  if (item.query && Object.keys(item.query).length > 0) {
    target.query = { ...item.query }
  }
  if (item.hash) {
    target.hash = item.hash
  }
  return target
}

export function buildVisibleNavGroups(
  visibleRouteKeys: string[],
  roles: string[] = [],
  capabilities?: CapabilityContext['capabilities'],
): NavGroupDefinition[] {
  const context: CapabilityContext = { roles, capabilities }
  const allowed = new Set(visibleRouteKeys)
  if (
    canAccessContentModuleManagement(context.roles) &&
    !allowed.has(ROUTE_KEYS.contentModuleManagement)
  ) {
    allowed.add(ROUTE_KEYS.contentModuleManagement)
  }
  // BDD-RT-IA-014 — System settings nav is capability-gated (not backend visibleRoutes).
  if (
    isGlobalAdmin(roles) &&
    canMaintainCollaborationTimeoutConfig(context) &&
    !allowed.has(ROUTE_KEYS.systemSettingsReminderTiming)
  ) {
    allowed.add(ROUTE_KEYS.systemSettingsReminderTiming)
  }
  const resourceGroups = NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => allowed.has(item.routeKey)),
  })).filter((group) => group.items.length > 0)

  // Behavior queue items are now surfaced as tabs inside DashboardView;
  // do not duplicate them in the sidebar.
  return resourceGroups
}
