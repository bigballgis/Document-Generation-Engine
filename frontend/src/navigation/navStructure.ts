import { ROUTE_KEYS, type RouteKey } from '@/routing/routeKeys'

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
    id: 'versionCatalog',
    labelKey: 'nav.groups.versionCatalog',
    items: [
      {
        id: 'master-versions',
        routeKey: ROUTE_KEYS.masterManagement,
        path: '/masters',
        labelKey: 'nav.items.masterVersions',
      },
      {
        id: 'template-versions',
        routeKey: ROUTE_KEYS.templateManagement,
        path: '/templates',
        labelKey: 'nav.items.templateVersions',
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

export function buildVisibleNavGroups(visibleRouteKeys: string[]): NavGroupDefinition[] {
  const allowed = new Set(visibleRouteKeys)
  return NAV_GROUPS
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => allowed.has(item.routeKey)),
    }))
    .filter((group) => group.items.length > 0)
}
