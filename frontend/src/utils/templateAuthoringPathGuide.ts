import type { LocationQuery } from 'vue-router'
import { templateDevVersionPath } from '@/routing/routeKeys'
import type { TemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'

export const AUTHORING_PATH_GUIDE_STEPS = [
  'master',
  'bindings',
  'variables',
  'preview',
] as const

export type AuthoringPathGuideStep = (typeof AUTHORING_PATH_GUIDE_STEPS)[number]

export const AUTHORING_PATH_GUIDE_LABEL_KEYS: Record<AuthoringPathGuideStep, string> = {
  master: 'templates.authoringPathGuide.steps.master',
  bindings: 'templates.authoringPathGuide.steps.bindings',
  variables: 'templates.authoringPathGuide.steps.variables',
  preview: 'templates.authoringPathGuide.steps.preview',
}

const DISMISS_STORAGE_PREFIX = 'docgen.authoringGuide.dismissed.'

export type AuthoringPathGuideNavigateQuery = TemplateJourneyWorkspaceQuery & {
  authoringGuide: '1'
  authoringGuideStep: AuthoringPathGuideStep
}

export function isAuthoringPathGuideQueryActive(query: LocationQuery): boolean {
  return query.authoringGuide === '1'
}

export function isAuthoringPathGuideDismissed(templateId: string): boolean {
  try {
    return sessionStorage.getItem(`${DISMISS_STORAGE_PREFIX}${templateId}`) === '1'
  } catch {
    return false
  }
}

export function dismissAuthoringPathGuide(templateId: string): void {
  try {
    sessionStorage.setItem(`${DISMISS_STORAGE_PREFIX}${templateId}`, '1')
  } catch {
    // sessionStorage may be unavailable; query clear still hides the guide
  }
}

export function isAuthoringPathGuideVisible(templateId: string, query: LocationQuery): boolean {
  return isAuthoringPathGuideQueryActive(query) && !isAuthoringPathGuideDismissed(templateId)
}

export function resolveAuthoringPathGuideStep(query: LocationQuery): AuthoringPathGuideStep {
  const raw = query.authoringGuideStep
  if (typeof raw === 'string' && (AUTHORING_PATH_GUIDE_STEPS as readonly string[]).includes(raw)) {
    return raw as AuthoringPathGuideStep
  }

  if (query.workspaceTab === 'testing' || query.testingTab === 'previewRuns') {
    return 'preview'
  }
  if (query.designTab === 'variables') {
    return 'variables'
  }
  if (query.designTab === 'bindings') {
    return 'bindings'
  }
  return 'master'
}

export function resolveAuthoringPathGuideNavigateQuery(
  step: AuthoringPathGuideStep,
): AuthoringPathGuideNavigateQuery {
  switch (step) {
    case 'master':
      return {
        workspaceTab: 'design',
        authoringGuide: '1',
        authoringGuideStep: 'master',
      }
    case 'bindings':
      return {
        workspaceTab: 'design',
        designTab: 'bindings',
        authoringGuide: '1',
        authoringGuideStep: 'bindings',
      }
    case 'variables':
      return {
        workspaceTab: 'design',
        designTab: 'variables',
        authoringGuide: '1',
        authoringGuideStep: 'variables',
      }
    case 'preview':
      return {
        workspaceTab: 'testing',
        testingTab: 'previewRuns',
        authoringGuide: '1',
        authoringGuideStep: 'preview',
      }
  }
}

export function nextAuthoringPathGuideStep(
  step: AuthoringPathGuideStep,
): AuthoringPathGuideStep | null {
  const index = AUTHORING_PATH_GUIDE_STEPS.indexOf(step)
  if (index < 0 || index >= AUTHORING_PATH_GUIDE_STEPS.length - 1) {
    return null
  }
  return AUTHORING_PATH_GUIDE_STEPS[index + 1]
}

/** Strip guide markers from a route query while preserving other keys. */
export function stripAuthoringPathGuideQuery(
  query: LocationQuery,
): Record<string, string | string[]> {
  const normalized: Record<string, string | string[]> = {}
  for (const [key, value] of Object.entries(query)) {
    if (key === 'authoringGuide' || key === 'authoringGuideStep' || value === null || value === undefined) {
      continue
    }
    if (Array.isArray(value)) {
      normalized[key] = value.filter((entry): entry is string => entry !== null)
      continue
    }
    normalized[key] = value
  }
  return normalized
}

export function buildPostCreateAuthoringPath(templateId: string, devVersionId: string): string {
  return templateDevVersionPath(templateId, devVersionId, undefined, {
    workspaceTab: 'design',
    authoringGuide: '1',
    authoringGuideStep: 'master',
  })
}
