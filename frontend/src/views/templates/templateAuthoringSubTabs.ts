export const TEMPLATE_AUTHORING_SUB_TABS = [
  'variables',
  'contentModules',
  'bindings',
] as const

export type TemplateAuthoringSubTab = (typeof TEMPLATE_AUTHORING_SUB_TABS)[number]

/** CE-U16 — design lands on Bindings when designTab / legacy authoringTab is absent or invalid. */
export const DEFAULT_TEMPLATE_AUTHORING_SUB_TAB: TemplateAuthoringSubTab = 'bindings'

const TEMPLATE_AUTHORING_SUB_TAB_LABEL_KEYS: Record<TemplateAuthoringSubTab, string> = {
  variables: 'templates.authoring.subTabs.variables',
  contentModules: 'templates.authoring.subTabs.contentModules',
  bindings: 'templates.authoring.subTabs.bindings',
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
