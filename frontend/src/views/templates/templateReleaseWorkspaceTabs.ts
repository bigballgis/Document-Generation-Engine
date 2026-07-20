import type { LocationQuery } from 'vue-router'

const TEMPLATE_RELEASE_WORKSPACE_TABS = [
  'basics',
  'dependencies',
  'testing',
  'approval',
  'variables',
  'bindings',
  'rules',
] as const

export type TemplateReleaseWorkspaceTab = (typeof TEMPLATE_RELEASE_WORKSPACE_TABS)[number]

const DEFAULT_TEMPLATE_RELEASE_WORKSPACE_TAB: TemplateReleaseWorkspaceTab = 'basics'

export const TEMPLATE_RELEASE_WORKSPACE_TAB_LABEL_KEYS: Record<
  TemplateReleaseWorkspaceTab,
  string
> = {
  basics: 'templates.releaseDetail.tabs.basics',
  dependencies: 'templates.releaseDetail.tabs.dependencies',
  testing: 'templates.releaseDetail.tabs.testing',
  approval: 'templates.releaseDetail.tabs.approval',
  variables: 'templates.releaseDetail.tabs.variables',
  bindings: 'templates.releaseDetail.tabs.bindings',
  rules: 'templates.releaseDetail.tabs.rules',
}

export function resolveTemplateReleaseWorkspaceTab(value: unknown): TemplateReleaseWorkspaceTab {
  if (
    typeof value === 'string' &&
    (TEMPLATE_RELEASE_WORKSPACE_TABS as readonly string[]).includes(value)
  ) {
    return value as TemplateReleaseWorkspaceTab
  }
  return DEFAULT_TEMPLATE_RELEASE_WORKSPACE_TAB
}

export function resolveTemplateReleaseWorkspaceTabFromQuery(
  query: LocationQuery,
): TemplateReleaseWorkspaceTab {
  const workspaceTab = query.workspaceTab
  if (typeof workspaceTab === 'string') {
    return resolveTemplateReleaseWorkspaceTab(workspaceTab)
  }
  return DEFAULT_TEMPLATE_RELEASE_WORKSPACE_TAB
}

export function buildTemplateReleaseWorkspaceQuery(
  query: LocationQuery,
  workspaceTab: TemplateReleaseWorkspaceTab,
): Record<string, string | string[]> {
  const normalized: Record<string, string | string[]> = {}
  for (const [key, value] of Object.entries(query)) {
    if (key === 'workspaceTab' || value === null || value === undefined) {
      continue
    }
    if (Array.isArray(value)) {
      normalized[key] = value.filter((entry): entry is string => entry !== null)
      continue
    }
    normalized[key] = value
  }
  normalized.workspaceTab = workspaceTab
  return normalized
}
