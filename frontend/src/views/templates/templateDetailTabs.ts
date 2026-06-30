export const TEMPLATE_DETAIL_TABS = ['overview', 'lifecycle', 'authoring', 'releaseVersions', 'apiAccess'] as const

export type TemplateDetailTab = (typeof TEMPLATE_DETAIL_TABS)[number]

export const TEMPLATE_DETAIL_TAB_LABEL_KEYS: Record<TemplateDetailTab, string> = {
  overview: 'templates.detail.tabs.overview',
  lifecycle: 'templates.detail.tabs.lifecycle',
  authoring: 'templates.detail.tabs.authoring',
  releaseVersions: 'templates.detail.tabs.releaseVersions',
  apiAccess: 'templates.detail.tabs.apiAccess',
}

export function templateDetailTabLabelKey(tab: TemplateDetailTab): string {
  return TEMPLATE_DETAIL_TAB_LABEL_KEYS[tab]
}

export function resolveTemplateDetailTab(value: unknown): TemplateDetailTab {
  if (typeof value === 'string' && (TEMPLATE_DETAIL_TABS as readonly string[]).includes(value)) {
    return value as TemplateDetailTab
  }
  return 'releaseVersions'
}
