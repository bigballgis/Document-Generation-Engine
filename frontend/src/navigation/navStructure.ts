import {
  canViewEscalationQueue,
  canAuthorTemplates,
  canDecideApprovals,
  canDecideTests,
  canPublishTemplates,
  canReviewMasters,
  canAccessContentModuleManagement,
  MANAGEMENT_ROLES,
  type CapabilityContext,
} from '@/auth/roles'
import { ROUTE_KEYS, type RouteKey } from '@/routing/routeKeys'

export interface NavItemDefinition {
  id: string
  routeKey: RouteKey
  path: string
  labelKey: string
  query?: Record<string, string>
  hash?: string
}

export interface NavGroupDefinition {
  id: string
  labelKey: string
  items: NavItemDefinition[]
}

export interface NavItemTarget {
  path: string
  query?: Record<string, string>
  hash?: string
}

export interface BehaviorNavItemSpec {
  id: string
  labelKey: string
  query?: Record<string, string>
  hash: string
  isVisible: (context: CapabilityContext, visibleRoutes: string[]) => boolean
}

function canSeeBehaviorMasterReview(
  context: CapabilityContext,
  visibleRoutes: string[],
): boolean {
  if (canReviewMasters(context)) {
    return true
  }
  return (
    context.roles.includes(MANAGEMENT_ROLES.MASTER_DESIGNER) &&
    visibleRoutes.includes(ROUTE_KEYS.masterManagement)
  )
}

function hasDashboardHomeAccess(visibleRoutes: string[]): boolean {
  return visibleRoutes.includes(ROUTE_KEYS.dashboardHome)
}

function canSeeBehaviorRemediation(context: CapabilityContext): boolean {
  const hasEligibleRole = context.roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
      ] as string[]
    ).includes(role),
  )
  if (!hasEligibleRole) {
    return false
  }
  return canAuthorTemplates(context)
}

/** Behavior-typed nav catalog — visibility driven by capabilities + roles (P21 §12.2). */
export const BEHAVIOR_NAV_ITEM_SPECS: BehaviorNavItemSpec[] = [
  {
    id: 'behavior-testing',
    labelKey: 'nav.behaviorItems.testing',
    query: { queue: 'TEST' },
    hash: '#tasks-section',
    isVisible: (context) => canDecideTests(context),
  },
  {
    id: 'behavior-approval',
    labelKey: 'nav.behaviorItems.approval',
    query: { queue: 'APPROVAL' },
    hash: '#tasks-section',
    isVisible: (context) => canDecideApprovals(context),
  },
  {
    id: 'behavior-remediation',
    labelKey: 'nav.behaviorItems.remediation',
    query: { queue: 'REMEDIATION' },
    hash: '#tasks-section',
    isVisible: (context) => canSeeBehaviorRemediation(context),
  },
  {
    id: 'behavior-pending-release',
    labelKey: 'nav.behaviorItems.pendingRelease',
    query: { queue: 'PENDING_RELEASE' },
    hash: '#tasks-section',
    isVisible: (context) => canPublishTemplates(context),
  },
  {
    id: 'behavior-escalation',
    labelKey: 'nav.behaviorItems.escalation',
    query: { queue: 'ESCALATION' },
    hash: '#tasks-section',
    isVisible: (context) => canViewEscalationQueue(context),
  },
  {
    id: 'behavior-master-review',
    labelKey: 'nav.behaviorItems.masterReview',
    query: { filter: 'master-review' },
    hash: '#tasks-section',
    isVisible: (context, visibleRoutes) => canSeeBehaviorMasterReview(context, visibleRoutes),
  },
]

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
  const resourceGroups = NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => allowed.has(item.routeKey)),
  })).filter((group) => group.items.length > 0)

  const behaviorItems = buildVisibleBehaviorNavItems(context, visibleRouteKeys)
  if (behaviorItems.length === 0) {
    return resourceGroups
  }

  const myTodosGroup: NavGroupDefinition = {
    id: 'myTodos',
    labelKey: 'nav.groups.myTodos',
    items: behaviorItems,
  }

  const overviewIndex = resourceGroups.findIndex((group) => group.id === 'overview')
  if (overviewIndex === -1) {
    return [myTodosGroup, ...resourceGroups]
  }

  return [
    ...resourceGroups.slice(0, overviewIndex + 1),
    myTodosGroup,
    ...resourceGroups.slice(overviewIndex + 1),
  ]
}
