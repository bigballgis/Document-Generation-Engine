export const TEMPLATE_AUTHORING_SUB_TABS = [
  'variables',
  'bindings',
  'rules',
  'contentModules',
  'testPreview',
] as const

export type TemplateAuthoringSubTab = (typeof TEMPLATE_AUTHORING_SUB_TABS)[number]

export const DEFAULT_TEMPLATE_AUTHORING_SUB_TAB: TemplateAuthoringSubTab = 'variables'

export const TEMPLATE_AUTHORING_SUB_TAB_LABEL_KEYS: Record<TemplateAuthoringSubTab, string> = {
  variables: 'templates.authoring.subTabs.variables',
  bindings: 'templates.authoring.subTabs.bindings',
  rules: 'templates.authoring.subTabs.rules',
  contentModules: 'templates.authoring.subTabs.contentModules',
  testPreview: 'templates.authoring.subTabs.testPreview',
}

export function resolveTemplateAuthoringSubTab(value: unknown): TemplateAuthoringSubTab {
  if (typeof value === 'string' && (TEMPLATE_AUTHORING_SUB_TABS as readonly string[]).includes(value)) {
    return value as TemplateAuthoringSubTab
  }
  return DEFAULT_TEMPLATE_AUTHORING_SUB_TAB
}

export function templateAuthoringSubTabLabelKey(tab: TemplateAuthoringSubTab): string {
  return TEMPLATE_AUTHORING_SUB_TAB_LABEL_KEYS[tab]
}
