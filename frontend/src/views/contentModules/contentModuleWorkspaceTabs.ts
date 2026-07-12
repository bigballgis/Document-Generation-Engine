import type { LocationQuery } from 'vue-router'

const CONTENT_MODULE_WORKSPACE_TABS = ['versions', 'content', 'lifecycle'] as const

export type ContentModuleWorkspaceTab = (typeof CONTENT_MODULE_WORKSPACE_TABS)[number]

const DEFAULT_CONTENT_MODULE_WORKSPACE_TAB: ContentModuleWorkspaceTab = 'versions'

export const CONTENT_MODULE_WORKSPACE_TAB_LABEL_KEYS: Record<ContentModuleWorkspaceTab, string> = {
  versions: 'contentModules.workspace.tabs.versions',
  content: 'contentModules.workspace.tabs.content',
  lifecycle: 'contentModules.workspace.tabs.lifecycle',
}

function resolveContentModuleWorkspaceTab(value: unknown): ContentModuleWorkspaceTab {
  if (
    typeof value === 'string' &&
    (CONTENT_MODULE_WORKSPACE_TABS as readonly string[]).includes(value)
  ) {
    return value as ContentModuleWorkspaceTab
  }
  return DEFAULT_CONTENT_MODULE_WORKSPACE_TAB
}

export function resolveContentModuleWorkspaceTabFromQuery(
  query: LocationQuery,
): ContentModuleWorkspaceTab {
  const workspaceTab = query.workspaceTab
  if (typeof workspaceTab === 'string') {
    return resolveContentModuleWorkspaceTab(workspaceTab)
  }
  return DEFAULT_CONTENT_MODULE_WORKSPACE_TAB
}

export function buildContentModuleWorkspaceQuery(
  query: LocationQuery,
  workspaceTab: ContentModuleWorkspaceTab,
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
