import type { TemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'

/**
 * CE-U15-D7: checkCode → workspace deep-link for pending pre-release checks.
 * Unknown codes return null (no dead links — U15-D8).
 */
const PUBLISH_GATE_GO_FIX_QUERY: Readonly<Record<string, TemplateJourneyWorkspaceQuery>> = {
  ANCHOR_INTEGRITY: { workspaceTab: 'design', designTab: 'bindings' },
  BLOCKER_STATUS: { workspaceTab: 'design', designTab: 'bindings' },
  PASTE_CLEANING_BLOCKERS: { workspaceTab: 'design', designTab: 'bindings' },
  UNSUPPORTED_STRUCTURED_NODES: { workspaceTab: 'design', designTab: 'bindings' },
  VARIABLE_SCHEMA: { workspaceTab: 'design', designTab: 'variables' },
  RULE_BOUNDS: { workspaceTab: 'design', designTab: 'bindings' },
  CONTENT_MODULE_REFERENCES: { workspaceTab: 'design', designTab: 'contentModules' },
  CONTENT_MODULE_EFFECTIVE_EXPIRED: { workspaceTab: 'design', designTab: 'contentModules' },
  CONTENT_MODULE_EFFECTIVE_NOT_STARTED: { workspaceTab: 'design', designTab: 'contentModules' },
  CONTENT_MODULE_LOCALE_MISMATCH: { workspaceTab: 'design', designTab: 'contentModules' },
  CONTENT_MODULE_NESTING_CYCLE: { workspaceTab: 'design', designTab: 'contentModules' },
  CONTENT_MODULE_NESTING_DEPTH_EXCEEDED: { workspaceTab: 'design', designTab: 'contentModules' },
  CONTENT_MODULE_NESTING_UNPINNED: { workspaceTab: 'design', designTab: 'contentModules' },
  COMPOSITION_INCLUSION_REFERENCE_INVALID: { workspaceTab: 'design', designTab: 'contentModules' },
  PAGINATION_DELTA_BUDGET: { workspaceTab: 'testing', testingTab: 'previewRuns' },
  TEST_RESULTS: { workspaceTab: 'testing', testingTab: 'previewRuns' },
  PREVIEW_PRESENT: { workspaceTab: 'testing', testingTab: 'previewRuns' },
  COVERAGE_THRESHOLDS: { workspaceTab: 'testing', testingTab: 'coverage' },
  CHANGE_DIFF: { workspaceTab: 'testing', testingTab: 'changeDiff' },
  FIDELITY_WARNINGS_VIEWED: { workspaceTab: 'testing', testingTab: 'previewRuns' },
  APPROVAL_SUMMARY: { workspaceTab: 'approval', approvalTab: 'submitApproval' },
  // Dev workspace has no dedicated API policy route; stay on publish readiness (U15-D7).
  API_POLICY: { workspaceTab: 'approval', approvalTab: 'publishReadiness' },
}

export function resolvePublishGateGoFixQuery(
  checkCode: string | null | undefined,
): TemplateJourneyWorkspaceQuery | null {
  if (!checkCode) {
    return null
  }
  return PUBLISH_GATE_GO_FIX_QUERY[checkCode] ?? null
}

/** Go fix is shown only when the check is pending and a repair surface is mapped (U15-D6/D8). */
export function resolvePublishGateGoFixTarget(
  checkCode: string | null | undefined,
  ready: boolean,
): TemplateJourneyWorkspaceQuery | null {
  if (ready) {
    return null
  }
  return resolvePublishGateGoFixQuery(checkCode)
}
