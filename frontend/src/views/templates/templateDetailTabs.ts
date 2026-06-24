export const TEMPLATE_DETAIL_TABS = ['overview', 'authoring', 'releaseVersions', 'apiAccess'] as const

export type TemplateDetailTab = (typeof TEMPLATE_DETAIL_TABS)[number]

export function resolveTemplateDetailTab(value: unknown): TemplateDetailTab {
  if (typeof value === 'string' && (TEMPLATE_DETAIL_TABS as readonly string[]).includes(value)) {
    return value as TemplateDetailTab
  }
  return 'releaseVersions'
}
