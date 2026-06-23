export const ROUTE_KEYS = {
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

export const ROUTE_PATH_BY_KEY: Record<RouteKey, string> = {
  [ROUTE_KEYS.globalGovernanceHome]: '/home/global-governance',
  [ROUTE_KEYS.groupGovernanceHome]: '/home/group-governance',
  [ROUTE_KEYS.templateAuthoringHome]: '/home/template-authoring',
  [ROUTE_KEYS.apiPolicyManagement]: '/home/api-policy',
  [ROUTE_KEYS.auditConsole]: '/home/audit',
  [ROUTE_KEYS.masterManagement]: '/masters',
  [ROUTE_KEYS.templateManagement]: '/templates',
  [ROUTE_KEYS.testerWorkbench]: '/home/tester-workbench',
  [ROUTE_KEYS.approverWorkbench]: '/home/approver-workbench',
  [ROUTE_KEYS.identityAdministration]: '/home/identity',
}

export const ROUTE_NAV_LABEL_KEY: Record<RouteKey, string> = {
  [ROUTE_KEYS.globalGovernanceHome]: 'nav.routes.globalGovernance',
  [ROUTE_KEYS.groupGovernanceHome]: 'nav.routes.groupGovernance',
  [ROUTE_KEYS.templateAuthoringHome]: 'nav.routes.templateAuthoring',
  [ROUTE_KEYS.apiPolicyManagement]: 'nav.routes.apiPolicy',
  [ROUTE_KEYS.auditConsole]: 'nav.routes.audit',
  [ROUTE_KEYS.masterManagement]: 'nav.routes.masters',
  [ROUTE_KEYS.templateManagement]: 'nav.routes.templates',
  [ROUTE_KEYS.testerWorkbench]: 'nav.routes.testerWorkbench',
  [ROUTE_KEYS.approverWorkbench]: 'nav.routes.approverWorkbench',
  [ROUTE_KEYS.identityAdministration]: 'nav.routes.identityAdministration',
}

export const MASTER_DETAIL_PATH_PREFIX = '/masters/'
export const TEMPLATE_DETAIL_PATH_PREFIX = '/templates/'

export function pathForRouteKey(routeKey: string): string {
  return ROUTE_PATH_BY_KEY[routeKey as RouteKey] ?? '/forbidden'
}

export function routeKeyForPath(path: string): RouteKey | undefined {
  const entry = Object.entries(ROUTE_PATH_BY_KEY).find(([, routePath]) => routePath === path)
  return entry?.[0] as RouteKey | undefined
}

export function templateDetailPath(templateId: string): string {
  return `${TEMPLATE_DETAIL_PATH_PREFIX}${templateId}`
}
