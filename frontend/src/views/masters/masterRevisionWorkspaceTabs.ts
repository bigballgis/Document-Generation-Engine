import type { LocationQuery } from 'vue-router'

const MASTER_REVISION_WORKSPACE_TABS = ['design', 'approval'] as const

export type MasterRevisionWorkspaceTab = (typeof MASTER_REVISION_WORKSPACE_TABS)[number]

const DEFAULT_MASTER_REVISION_WORKSPACE_TAB: MasterRevisionWorkspaceTab = 'design'

export const MASTER_REVISION_WORKSPACE_TAB_LABEL_KEYS: Record<MasterRevisionWorkspaceTab, string> = {
  design: 'masters.revisionWorkspace.tabs.design',
  approval: 'masters.revisionWorkspace.tabs.approval',
}

export function resolveMasterRevisionWorkspaceTab(value: unknown): MasterRevisionWorkspaceTab {
  if (
    typeof value === 'string' &&
    (MASTER_REVISION_WORKSPACE_TABS as readonly string[]).includes(value)
  ) {
    return value as MasterRevisionWorkspaceTab
  }
  return DEFAULT_MASTER_REVISION_WORKSPACE_TAB
}

export function resolveMasterRevisionWorkspaceTabFromQuery(
  query: LocationQuery,
): MasterRevisionWorkspaceTab {
  const workspaceTab = query.workspaceTab
  if (typeof workspaceTab === 'string') {
    return resolveMasterRevisionWorkspaceTab(workspaceTab)
  }
  return DEFAULT_MASTER_REVISION_WORKSPACE_TAB
}

export function buildMasterRevisionWorkspaceQuery(
  query: LocationQuery,
  workspaceTab: MasterRevisionWorkspaceTab,
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
