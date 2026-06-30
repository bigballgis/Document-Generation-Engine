import type { LocationQuery } from 'vue-router'

export const TEMPLATE_DETAIL_TABS = ['overview', 'lifecycle', 'authoring', 'releaseVersions', 'apiAccess'] as const

export type TemplateDetailTab = (typeof TEMPLATE_DETAIL_TABS)[number]

export const DEFAULT_TEMPLATE_DETAIL_TAB: TemplateDetailTab = TEMPLATE_DETAIL_TABS[0]

export const TEMPLATE_DETAIL_TAB_LABEL_KEYS: Record<TemplateDetailTab, string> = {
  overview: 'templates.detail.tabs.overview',
  lifecycle: 'templates.detail.tabs.lifecycle',
  authoring: 'templates.detail.tabs.authoring',
  releaseVersions: 'templates.detail.tabs.releaseVersions',
  apiAccess: 'templates.detail.tabs.apiAccess',
}

export type TemplateDetailRouteQuery = LocationQuery

export function templateDetailTabLabelKey(tab: TemplateDetailTab): string {
  return TEMPLATE_DETAIL_TAB_LABEL_KEYS[tab]
}

export function resolveTemplateDetailTab(value: unknown): TemplateDetailTab {
  if (typeof value === 'string' && (TEMPLATE_DETAIL_TABS as readonly string[]).includes(value)) {
    return value as TemplateDetailTab
  }
  return DEFAULT_TEMPLATE_DETAIL_TAB
}

export function resolveTemplateDetailTabFromQuery(query: TemplateDetailRouteQuery): TemplateDetailTab {
  if (query.focus === 'lifecycle') {
    return 'lifecycle'
  }
  return resolveTemplateDetailTab(query.tab)
}

export function normalizeTemplateDetailQuery(
  query: TemplateDetailRouteQuery,
): { query: Record<string, string | string[]>; tab: TemplateDetailTab } | null {
  if (query.focus !== 'lifecycle') {
    return null
  }

  const normalizedQuery: Record<string, string | string[]> = {}
  for (const [key, value] of Object.entries(query)) {
    if (key === 'focus' || value === null || value === undefined) {
      continue
    }
    if (Array.isArray(value)) {
      normalizedQuery[key] = value.filter((entry): entry is string => entry !== null)
      continue
    }
    normalizedQuery[key] = value
  }
  normalizedQuery.tab = 'lifecycle'
  return { query: normalizedQuery, tab: 'lifecycle' }
}
