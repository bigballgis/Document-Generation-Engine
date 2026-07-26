import type { LocationQuery } from 'vue-router'
import {
  DEFAULT_TEMPLATE_AUTHORING_SUB_TAB,
  resolveTemplateAuthoringSubTab,
  type TemplateAuthoringSubTab,
} from '@/views/templates/templateAuthoringSubTabs'
import {
  DEFAULT_TEMPLATE_APPROVAL_SUB_TAB,
  resolveTemplateApprovalSubTab,
  type TemplateApprovalSubTab,
} from '@/views/templates/templateApprovalSubTabs'
import {
  DEFAULT_TEMPLATE_TESTING_SUB_TAB,
  resolveTemplateTestingSubTab,
  type TemplateTestingSubTab,
} from '@/views/templates/templateTestingSubTabs'

export type TemplateDevWorkspaceSubTab =
  | TemplateAuthoringSubTab
  | TemplateTestingSubTab
  | TemplateApprovalSubTab

export const TEMPLATE_DEV_WORKSPACE_TABS = ['design', 'dependencies', 'testing', 'approval'] as const

export type TemplateDevWorkspaceTab = (typeof TEMPLATE_DEV_WORKSPACE_TABS)[number]

const DEFAULT_TEMPLATE_DEV_WORKSPACE_TAB: TemplateDevWorkspaceTab = 'design'

const TEMPLATE_DEV_WORKSPACE_TAB_LABEL_KEYS: Record<TemplateDevWorkspaceTab, string> = {
  design: 'templates.devWorkspace.tabs.design',
  dependencies: 'templates.devWorkspace.tabs.dependencies',
  testing: 'templates.devWorkspace.tabs.testing',
  approval: 'templates.devWorkspace.tabs.approval',
}

export type TemplateDevWorkspaceRouteQuery = LocationQuery

export function templateDevWorkspaceTabLabelKey(tab: TemplateDevWorkspaceTab): string {
  return TEMPLATE_DEV_WORKSPACE_TAB_LABEL_KEYS[tab]
}

export function resolveTemplateDevWorkspaceTab(value: unknown): TemplateDevWorkspaceTab {
  if (typeof value === 'string' && (TEMPLATE_DEV_WORKSPACE_TABS as readonly string[]).includes(value)) {
    return value as TemplateDevWorkspaceTab
  }
  return DEFAULT_TEMPLATE_DEV_WORKSPACE_TAB
}

/**
 * Maps legacy dev-editor query (`tab=authoring`, `authoringTab=testPreview`) to workspace tabs.
 */
export function resolveTemplateDevWorkspaceTabFromQuery(
  query: TemplateDevWorkspaceRouteQuery,
): TemplateDevWorkspaceTab {
  const workspaceTab = query.workspaceTab
  if (typeof workspaceTab === 'string') {
    return resolveTemplateDevWorkspaceTab(workspaceTab)
  }

  const focus = query.focus
  if (focus === 'workflow' || focus === 'lifecycle') {
    return 'approval'
  }

  const legacyTab = query.tab
  if (legacyTab === 'authoring') {
    // CRCH-W1-5: testPreview removed from authoring union; preserve legacy deep-link remap.
    if (query.authoringTab === 'testPreview') {
      return 'testing'
    }
    return 'design'
  }

  if (legacyTab === 'lifecycle') {
    return 'approval'
  }

  return DEFAULT_TEMPLATE_DEV_WORKSPACE_TAB
}

export function resolveDesignSubTabFromQuery(query: TemplateDevWorkspaceRouteQuery): TemplateAuthoringSubTab {
  const designTab = query.designTab
  if (typeof designTab === 'string') {
    if (designTab === 'testPreview') {
      return DEFAULT_TEMPLATE_AUTHORING_SUB_TAB
    }
    return resolveTemplateAuthoringSubTab(designTab)
  }

  if (query.authoringTab === 'testPreview') {
    return DEFAULT_TEMPLATE_AUTHORING_SUB_TAB
  }
  return resolveTemplateAuthoringSubTab(query.authoringTab)
}

export function resolveTestingSubTabFromQuery(query: TemplateDevWorkspaceRouteQuery): TemplateTestingSubTab {
  const testingTab = query.testingTab
  if (typeof testingTab === 'string') {
    return resolveTemplateTestingSubTab(testingTab)
  }
  return DEFAULT_TEMPLATE_TESTING_SUB_TAB
}

export function resolveApprovalSubTabFromQuery(query: TemplateDevWorkspaceRouteQuery): TemplateApprovalSubTab {
  const approvalTab = query.approvalTab
  if (typeof approvalTab === 'string') {
    return resolveTemplateApprovalSubTab(approvalTab)
  }

  const focus = query.focus
  if (focus === 'lifecycle') {
    return 'governance'
  }

  return DEFAULT_TEMPLATE_APPROVAL_SUB_TAB
}

export function buildDevWorkspaceQuery(
  query: TemplateDevWorkspaceRouteQuery,
  workspaceTab: TemplateDevWorkspaceTab,
  subTab?: TemplateDevWorkspaceSubTab,
): Record<string, string | string[]> {
  const normalized: Record<string, string | string[]> = {}
  for (const [key, value] of Object.entries(query)) {
    if (
      key === 'tab' ||
      key === 'authoringTab' ||
      key === 'workspaceTab' ||
      key === 'designTab' ||
      key === 'testingTab' ||
      key === 'approvalTab' ||
      value === null ||
      value === undefined
    ) {
      continue
    }
    if (Array.isArray(value)) {
      normalized[key] = value.filter((entry): entry is string => entry !== null)
      continue
    }
    normalized[key] = value
  }

  normalized.workspaceTab = workspaceTab
  if (workspaceTab === 'design' && subTab) {
    normalized.designTab = subTab as TemplateAuthoringSubTab
  }
  if (workspaceTab === 'testing' && subTab) {
    normalized.testingTab = subTab as TemplateTestingSubTab
  }
  if (workspaceTab === 'approval' && subTab) {
    normalized.approvalTab = subTab as TemplateApprovalSubTab
  }

  return normalized
}
