export const ROUTE_KEYS = {
  dashboardHome: 'route.dashboard-home',
  globalGovernanceHome: 'route.global-governance-home',
  groupGovernanceHome: 'route.group-governance-home',
  templateAuthoringHome: 'route.template-authoring-home',
  apiPolicyManagement: 'route.api-policy-management',
  auditConsole: 'route.audit-console',
  masterManagement: 'route.master-management',
  templateManagement: 'route.template-management',
  testerWorkbench: 'route.tester-workbench',
  approverWorkbench: 'route.approver-workbench',
  identityAdministration: 'route.identity-administration',
} as const

export type RouteKey = (typeof ROUTE_KEYS)[keyof typeof ROUTE_KEYS]

/** Legacy route keys from older sessions map to current landing paths. */
export const LEGACY_ROUTE_PATH_REDIRECT: Partial<Record<string, string>> = {
  [ROUTE_KEYS.globalGovernanceHome]: '/dashboard',
  [ROUTE_KEYS.groupGovernanceHome]: '/dashboard',
  [ROUTE_KEYS.templateAuthoringHome]: '/dashboard',
  [ROUTE_KEYS.testerWorkbench]: '/dashboard',
  [ROUTE_KEYS.approverWorkbench]: '/dashboard',
}

export const ROUTE_PATH_BY_KEY: Record<RouteKey, string> = {
  [ROUTE_KEYS.dashboardHome]: '/dashboard',
  [ROUTE_KEYS.globalGovernanceHome]: '/dashboard',
  [ROUTE_KEYS.groupGovernanceHome]: '/dashboard',
  [ROUTE_KEYS.templateAuthoringHome]: '/dashboard',
  [ROUTE_KEYS.apiPolicyManagement]: '/api/policies',
  [ROUTE_KEYS.auditConsole]: '/audit',
  [ROUTE_KEYS.masterManagement]: '/masters',
  [ROUTE_KEYS.templateManagement]: '/templates',
  [ROUTE_KEYS.testerWorkbench]: '/dashboard',
  [ROUTE_KEYS.approverWorkbench]: '/dashboard',
  [ROUTE_KEYS.identityAdministration]: '/entitlement/users',
}

export const ROUTE_NAV_LABEL_KEY: Record<RouteKey, string> = {
  [ROUTE_KEYS.dashboardHome]: 'nav.items.dashboard',
  [ROUTE_KEYS.globalGovernanceHome]: 'nav.items.dashboard',
  [ROUTE_KEYS.groupGovernanceHome]: 'nav.items.dashboard',
  [ROUTE_KEYS.templateAuthoringHome]: 'nav.items.dashboard',
  [ROUTE_KEYS.apiPolicyManagement]: 'nav.items.apiPolicies',
  [ROUTE_KEYS.auditConsole]: 'nav.items.audit',
  [ROUTE_KEYS.masterManagement]: 'nav.items.masterVersions',
  [ROUTE_KEYS.templateManagement]: 'nav.items.templateVersions',
  [ROUTE_KEYS.testerWorkbench]: 'nav.items.dashboard',
  [ROUTE_KEYS.approverWorkbench]: 'nav.items.dashboard',
  [ROUTE_KEYS.identityAdministration]: 'nav.items.users',
}

export const MASTER_DETAIL_PATH_PREFIX = '/masters/'
export const TEMPLATE_DETAIL_PATH_PREFIX = '/templates/'

export function pathForRouteKey(routeKey: string): string {
  return (
    LEGACY_ROUTE_PATH_REDIRECT[routeKey] ??
    ROUTE_PATH_BY_KEY[routeKey as RouteKey] ??
    '/forbidden'
  )
}

export function routeKeyForPath(path: string): RouteKey | undefined {
  const entry = Object.entries(ROUTE_PATH_BY_KEY).find(([, routePath]) => routePath === path)
  return entry?.[0] as RouteKey | undefined
}

export function templateDetailPath(templateId: string): string {
  return `${TEMPLATE_DETAIL_PATH_PREFIX}${templateId}`
}
