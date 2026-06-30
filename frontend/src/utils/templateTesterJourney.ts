import type { TemplateLifecycleStatus, TemplateSummary } from '@/types/template'

export interface TemplateTesterJourneyContext {
  lifecycleStatus: TemplateLifecycleStatus
  hasPreviewArtifact?: boolean
  fidelityViewedConfirmed?: boolean
  coverageViewedConfirmed?: boolean
  previewViewedConfirmed?: boolean
}

export interface TemplateTesterJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
  targetTemplateId?: string
}

export type TemplateTesterDashboardTemplate = TemplateSummary

export interface TemplateTesterTestWorkItem {
  templateId: string
  createdAt: string
  updatedAt?: string
}

const EMPTY_GUIDANCE = 'journey.roles.TEMPLATE_TESTER.empty.guidance'

export function hasTemplatePreviewArtifact(context: { hasPreviewArtifact?: boolean }): boolean {
  return context.hasPreviewArtifact === true
}

export function hasAnyEvidenceViewed(context: {
  fidelityViewedConfirmed?: boolean
  coverageViewedConfirmed?: boolean
  previewViewedConfirmed?: boolean
}): boolean {
  return (
    context.fidelityViewedConfirmed === true ||
    context.coverageViewedConfirmed === true ||
    context.previewViewedConfirmed === true
  )
}

export function hasAllEvidenceViewed(context: {
  fidelityViewedConfirmed?: boolean
  coverageViewedConfirmed?: boolean
  previewViewedConfirmed?: boolean
}): boolean {
  return (
    context.fidelityViewedConfirmed === true &&
    context.coverageViewedConfirmed === true &&
    context.previewViewedConfirmed === true
  )
}

function testingInnerStep(
  context: TemplateTesterJourneyContext,
): Pick<TemplateTesterJourneyResolution, 'currentStepIndex' | 'activeStepId'> {
  if (hasAllEvidenceViewed(context)) {
    return { currentStepIndex: 2, activeStepId: 'recordResult' }
  }
  if (hasTemplatePreviewArtifact(context) || hasAnyEvidenceViewed(context)) {
    return { currentStepIndex: 1, activeStepId: 'checkEvidence' }
  }
  return { currentStepIndex: 0, activeStepId: 'reviewRequest' }
}

export function resolveTemplateTesterJourneyIndex(
  context: TemplateTesterJourneyContext,
): TemplateTesterJourneyResolution {
  if (context.lifecycleStatus !== 'TESTING') {
    return { currentStepIndex: null }
  }
  return testingInnerStep(context)
}

function pickNewestTestWorkItem(
  items: TemplateTesterTestWorkItem[],
): TemplateTesterTestWorkItem | undefined {
  if (items.length === 0) {
    return undefined
  }
  return [...items].sort(
    (left, right) =>
      Date.parse(right.updatedAt ?? right.createdAt) -
      Date.parse(left.updatedAt ?? left.createdAt),
  )[0]
}

function pickNewestTestingTemplate(
  templates: TemplateTesterDashboardTemplate[],
): TemplateTesterDashboardTemplate | undefined {
  const testingTemplates = templates.filter((template) => template.lifecycleStatus === 'TESTING')
  if (testingTemplates.length === 0) {
    return undefined
  }
  return [...testingTemplates].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  )[0]
}

function dashboardContextForTestWorkItem(): TemplateTesterJourneyContext {
  return {
    lifecycleStatus: 'TESTING',
  }
}

function dashboardContextForTestingTemplate(
  template: TemplateTesterDashboardTemplate | undefined,
): TemplateTesterJourneyContext {
  return {
    lifecycleStatus: 'TESTING',
    hasPreviewArtifact: template?.lifecycleStatus === 'TESTING' ? true : undefined,
  }
}

export function resolveTemplateTesterDashboardJourneyIndex(
  templates: TemplateTesterDashboardTemplate[],
  testWorkItems: TemplateTesterTestWorkItem[],
): TemplateTesterJourneyResolution {
  if (testWorkItems.length === 0 && !templates.some((template) => template.lifecycleStatus === 'TESTING')) {
    return { currentStepIndex: null, guidanceKey: EMPTY_GUIDANCE }
  }

  const testTarget = pickNewestTestWorkItem(testWorkItems)
  if (testTarget) {
    return {
      ...testingInnerStep(dashboardContextForTestWorkItem()),
      targetTemplateId: testTarget.templateId,
    }
  }

  const testingTarget = pickNewestTestingTemplate(templates)
  return {
    ...testingInnerStep(dashboardContextForTestingTemplate(testingTarget)),
    targetTemplateId: testingTarget?.id,
  }
}

export function templateTesterStepCtaKey(stepId: string): string {
  return `journey.roles.TEMPLATE_TESTER.steps.${stepId}.cta`
}

export function shouldShowTemplateTesterJourney(options: { decideTests: boolean }): boolean {
  return options.decideTests
}
