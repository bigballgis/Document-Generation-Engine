import {
  canViewEscalationQueue,
  canAuthorTemplates,
  canDecideApprovals,
  canDecideLegalApprovals,
  canDecideTests,
  canPublishTemplates,
  canReviewMasters,
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
    id: 'behavior-legal',
    labelKey: 'nav.behaviorItems.legal',
    query: { queue: 'LEGAL' },
    hash: '#tasks-section',
    isVisible: (context) => canDecideLegalApprovals(context),
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
