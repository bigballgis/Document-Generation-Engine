export const ROUTE_KEYS = {
  dashboardHome: 'route.dashboard-home',
  globalGovernanceHome: 'route.global-governance-home',
  groupGovernanceHome: 'route.group-governance-home',
  templateAuthoringHome: 'route.template-authoring-home',
  apiPolicyManagement: 'route.api-policy-management',
  auditConsole: 'route.audit-console',
  masterManagement: 'route.master-management',
  templateManagement: 'route.template-management',
  contentModuleManagement: 'route.content-module-management',
  assetLibraryManagement: 'route.asset-library-management',
  identityAdministration: 'route.identity-administration',
  legalHoldAdministration: 'route.legal-hold-administration',
  documentBrandAdministration: 'route.document-brand-administration',
} as const

export type RouteKey = (typeof ROUTE_KEYS)[keyof typeof ROUTE_KEYS]

/** Legacy route keys from older sessions map to current landing paths. */
const LEGACY_ROUTE_PATH_REDIRECT: Partial<Record<string, string>> = {
  [ROUTE_KEYS.globalGovernanceHome]: '/dashboard',
  [ROUTE_KEYS.groupGovernanceHome]: '/dashboard',
  [ROUTE_KEYS.templateAuthoringHome]: '/dashboard',
  'route.tester-workbench': '/dashboard#tasks-section',
  'route.approver-workbench': '/dashboard#tasks-section',
  'route.escalation-workbench': '/dashboard#tasks-section',
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
  [ROUTE_KEYS.contentModuleManagement]: '/content-modules',
  [ROUTE_KEYS.assetLibraryManagement]: '/library/assets',
  [ROUTE_KEYS.identityAdministration]: '/entitlement/users',
  [ROUTE_KEYS.legalHoldAdministration]: '/governance/legal-holds',
  [ROUTE_KEYS.documentBrandAdministration]: '/governance/document-brands',
}


export const MASTER_DETAIL_PATH_PREFIX = '/masters/'
const TEMPLATE_DETAIL_PATH_PREFIX = '/templates/'
const CONTENT_MODULE_DETAIL_PATH_PREFIX = '/content-modules/'

export function contentModuleDetailPath(moduleId: string): string {
  return `${CONTENT_MODULE_DETAIL_PATH_PREFIX}${moduleId}`
}

export function masterDetailPath(masterId: string): string {
  return `${MASTER_DETAIL_PATH_PREFIX}${masterId}`
}

export function masterRevisionDetailPath(masterId: string, revisionLineId: string): string {
  return `${MASTER_DETAIL_PATH_PREFIX}${masterId}/revisions/${revisionLineId}`
}

export function pathForRouteKey(routeKey: string): string {
  return (
    LEGACY_ROUTE_PATH_REDIRECT[routeKey] ??
    ROUTE_PATH_BY_KEY[routeKey as RouteKey] ??
    '/forbidden'
  )
}


export function templatePackageHubPath(templateId: string, tab?: string): string {
  const base = `${TEMPLATE_DETAIL_PATH_PREFIX}${templateId}`
  if (!tab) {
    return base
  }
  return `${base}?tab=${encodeURIComponent(tab)}`
}

/** @deprecated Use templatePackageHubPath — hub is the default template package surface. */
export function templateDetailPath(templateId: string, tab?: string): string {
  return templatePackageHubPath(templateId, tab)
}

export function templateDevVersionPath(
  templateId: string,
  devVersionId: string,
  tab?: string,
  extraQuery?: Record<string, string>,
): string {
  const base = `${TEMPLATE_DETAIL_PATH_PREFIX}${templateId}/dev/${devVersionId}`
  const params = new URLSearchParams()
  if (tab) {
    params.set('tab', tab)
  }
  if (extraQuery) {
    for (const [key, value] of Object.entries(extraQuery)) {
      params.set(key, value)
    }
  }
  const query = params.toString()
  return query ? `${base}?${query}` : base
}

export function templateReleaseDetailPath(templateId: string, releaseVersion: string): string {
  return `${TEMPLATE_DETAIL_PATH_PREFIX}${templateId}/releases/${encodeURIComponent(releaseVersion)}`
}

export function templateLifecyclePanelPath(
  templateId: string,
  extraQuery?: Record<string, string>,
): string {
  const base = templatePackageHubPath(templateId, 'lifecycle')
  if (!extraQuery) {
    return base
  }
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(extraQuery)) {
    if (value) {
      params.set(key, value)
    }
  }
  const query = params.toString()
  return query ? `${base}&${query}` : base
}

/** Canonical external-access surface — package hub tab (P13 IA convergence). */
function apiPolicyHubPath(templateId: string, domain?: string): string {
  const base = templatePackageHubPath(templateId, 'apiAccess')
  if (!domain) {
    return base
  }
  return `${base}#domain=${encodeURIComponent(domain)}`
}

/**
 * @deprecated Legacy `/api/policies/:id` — resolves to hub path for programmatic navigation.
 * Router redirect handles direct browser hits to the old URL.
 */
export function apiPolicyDetailPath(templateId: string, domain?: string): string {
  return apiPolicyHubPath(templateId, domain)
}
