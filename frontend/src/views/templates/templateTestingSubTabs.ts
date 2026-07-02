export const TEMPLATE_TESTING_SUB_TABS = [
  'dataSets',
  'previewRuns',
  'coverage',
  'changeDiff',
] as const

export type TemplateTestingSubTab = (typeof TEMPLATE_TESTING_SUB_TABS)[number]

export const DEFAULT_TEMPLATE_TESTING_SUB_TAB: TemplateTestingSubTab = 'dataSets'

export const TEMPLATE_TESTING_SUB_TAB_LABEL_KEYS: Record<TemplateTestingSubTab, string> = {
  dataSets: 'templates.devWorkspace.testing.subTabs.dataSets',
  previewRuns: 'templates.devWorkspace.testing.subTabs.previewRuns',
  coverage: 'templates.devWorkspace.testing.subTabs.coverage',
  changeDiff: 'templates.devWorkspace.testing.subTabs.changeDiff',
}

export function resolveTemplateTestingSubTab(value: unknown): TemplateTestingSubTab {
  if (typeof value === 'string' && (TEMPLATE_TESTING_SUB_TABS as readonly string[]).includes(value)) {
    return value as TemplateTestingSubTab
  }
  return DEFAULT_TEMPLATE_TESTING_SUB_TAB
}

export function templateTestingSubTabLabelKey(tab: TemplateTestingSubTab): string {
  return TEMPLATE_TESTING_SUB_TAB_LABEL_KEYS[tab]
}
