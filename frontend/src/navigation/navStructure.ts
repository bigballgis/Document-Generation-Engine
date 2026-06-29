import { ROUTE_KEYS, type RouteKey } from '@/routing/routeKeys'
import {
  canAccessCollaborationEscalationWorkbench,
  canAccessApproverWorkbench,
  canAccessContentModuleManagement,
  canAccessTesterWorkbench,
  type CapabilityContext,
} from '@/auth/roles'

export interface NavItemDefinition {
  id: string
  routeKey: RouteKey
  path: string
  labelKey: string
}

export interface NavGroupDefinition {
  id: string
  labelKey: string
  items: NavItemDefinition[]
}

/** User-facing navigation catalog. Order and grouping are fixed in the UI. */
export const NAV_GROUPS: NavGroupDefinition[] = [
  {
    id: 'overview',
    labelKey: 'nav.groups.overview',
    items: [
      {
        id: 'dashboard',
        routeKey: ROUTE_KEYS.dashboardHome,
        path: '/dashboard',
        labelKey: 'nav.items.dashboard',
      },
    ],
  },
  {
    id: 'entitlement',
    labelKey: 'nav.groups.entitlement',
    items: [
      {
        id: 'users',
        routeKey: ROUTE_KEYS.identityAdministration,
        path: '/entitlement/users',
        labelKey: 'nav.items.users',
      },
      {
        id: 'groups',
        routeKey: ROUTE_KEYS.identityAdministration,
        path: '/entitlement/groups',
        labelKey: 'nav.items.groups',
      },
    ],
  },
  {
    id: 'documentContent',
    labelKey: 'nav.groups.content',
    items: [
      {
        id: 'masters',
        routeKey: ROUTE_KEYS.masterManagement,
        path: '/masters',
        labelKey: 'nav.items.masters',
      },
      {
        id: 'templates',
        routeKey: ROUTE_KEYS.templateManagement,
        path: '/templates',
        labelKey: 'nav.items.templates',
      },
      {
        id: 'content-modules',
        routeKey: ROUTE_KEYS.contentModuleManagement,
        path: '/content-modules',
        labelKey: 'nav.items.contentModules',
      },
    ],
  },
  {
    id: 'api',
    labelKey: 'nav.groups.apiAccess',
    items: [
      {
        id: 'api-policies',
        routeKey: ROUTE_KEYS.apiPolicyManagement,
        path: '/api/policies',
        labelKey: 'nav.items.apiPolicies',
      },
    ],
  },
  {
    id: 'security',
    labelKey: 'nav.groups.security',
    items: [
      {
        id: 'audit',
        routeKey: ROUTE_KEYS.auditConsole,
        path: '/audit',
        labelKey: 'nav.items.audit',
      },
    ],
  },
]

const WORKBENCH_NAV_ITEMS: Array<NavItemDefinition & { canAccess: (context: CapabilityContext) => boolean }> = [
  {
    id: 'tester-workbench',
    routeKey: ROUTE_KEYS.testerWorkbench,
    path: '/workbench/tester',
    labelKey: 'nav.items.testerWorkbench',
    canAccess: canAccessTesterWorkbench,
  },
  {
    id: 'approver-workbench',
    routeKey: ROUTE_KEYS.approverWorkbench,
    path: '/workbench/approver',
    labelKey: 'nav.items.approverWorkbench',
    canAccess: canAccessApproverWorkbench,
  },
  {
    id: 'escalation-workbench',
    routeKey: ROUTE_KEYS.escalationWorkbench,
    path: '/workbench/escalation',
    labelKey: 'nav.items.escalationWorkbench',
    canAccess: canAccessCollaborationEscalationWorkbench,
  },
]

export function buildVisibleNavGroups(
  visibleRouteKeys: string[],
  roles: string[] = [],
  capabilities?: CapabilityContext['capabilities'],
): NavGroupDefinition[] {
  const context: CapabilityContext = { roles, capabilities }
  const allowed = new Set(visibleRouteKeys)
  if (
    canAccessContentModuleManagement(roles) &&
    !allowed.has(ROUTE_KEYS.contentModuleManagement)
  ) {
    allowed.add(ROUTE_KEYS.contentModuleManagement)
  }
  const groups = NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => allowed.has(item.routeKey)),
  })).filter((group) => group.items.length > 0)

  const workbenchItems: NavItemDefinition[] = WORKBENCH_NAV_ITEMS.filter((item) =>
    item.canAccess(context),
  ).map(({ id, routeKey, path, labelKey }) => ({ id, routeKey, path, labelKey }))
  if (workbenchItems.length > 0) {
    groups.push({
      id: 'workbench',
      labelKey: 'nav.groups.workbench',
      items: workbenchItems,
    })
  }

  return groups
}
