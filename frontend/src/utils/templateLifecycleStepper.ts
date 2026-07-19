import type { ApprovalSubState } from '@/types/approvalMatrix'
import type { TemplateLifecycleStatus } from '@/types/template'
import { isAwaitingAnyApprovalDecision } from '@/utils/approvalMatrix'
import type { TemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'

export const LIFECYCLE_STEPPER_STEP_IDS = [
  'draft',
  'testing',
  'readyForApproval',
  'pendingApproval',
  'pendingRelease',
  'published',
] as const

export type LifecycleStepperStepId = (typeof LIFECYCLE_STEPPER_STEP_IDS)[number]

export type LifecycleStepperStepStatus = 'completed' | 'current' | 'upcoming' | 'inactive'

export type LifecycleApprovalSubState = ApprovalSubState | null | undefined

export type LifecycleStepperModel = {
  /** When true, template is off the publish channel (STOPPED/DEPRECATED). */
  terminal: boolean
  /** Index into LIFECYCLE_STEPPER_STEP_IDS; null when terminal or unknown. */
  currentIndex: number | null
}

export const LIFECYCLE_STEPPER_LABEL_KEYS: Record<LifecycleStepperStepId, string> = {
  draft: 'templates.lifecycleStepper.steps.draft',
  testing: 'templates.lifecycleStepper.steps.testing',
  readyForApproval: 'templates.lifecycleStepper.steps.readyForApproval',
  pendingApproval: 'templates.lifecycleStepper.steps.pendingApproval',
  pendingRelease: 'templates.lifecycleStepper.steps.pendingRelease',
  published: 'templates.lifecycleStepper.steps.published',
}

/**
 * Maps product lifecycleStatus (+ approvalSubState) to the linear CE-U15 stepper model.
 * STOPPED / DEPRECATED do not participate as linear current steps (U15-D4).
 */
export function resolveLifecycleStepperModel(
  lifecycleStatus: TemplateLifecycleStatus | null | undefined,
  approvalSubState?: LifecycleApprovalSubState,
): LifecycleStepperModel {
  if (!lifecycleStatus) {
    return { terminal: false, currentIndex: null }
  }

  if (lifecycleStatus === 'STOPPED' || lifecycleStatus === 'DEPRECATED') {
    return { terminal: true, currentIndex: null }
  }

  switch (lifecycleStatus) {
    case 'DRAFT':
      return { terminal: false, currentIndex: 0 }
    case 'TESTING':
      return { terminal: false, currentIndex: 1 }
    case 'APPROVAL':
      // Missing sub-state aligns with existing FE: treat as ready to submit (PENDING_SUBMIT).
      // IBL-E3: LEGAL / COMPLIANCE / SINGLE_TRACK pending decision all map to pendingApproval.
      if (isAwaitingAnyApprovalDecision(approvalSubState)) {
        return { terminal: false, currentIndex: 3 }
      }
      return { terminal: false, currentIndex: 2 }
    case 'PENDING_RELEASE':
      return { terminal: false, currentIndex: 4 }
    case 'PUBLISHED':
      return { terminal: false, currentIndex: 5 }
    default:
      return { terminal: false, currentIndex: null }
  }
}

export function resolveLifecycleStepperStepStatus(
  index: number,
  model: LifecycleStepperModel,
): LifecycleStepperStepStatus {
  if (model.terminal || model.currentIndex === null) {
    return 'inactive'
  }
  if (index < model.currentIndex) {
    return 'completed'
  }
  if (index === model.currentIndex) {
    return 'current'
  }
  return 'upcoming'
}

/** Optional orientation-only navigation; never triggers lifecycle transitions (U15-D3). */
export function resolveLifecycleStepperWorkspaceQuery(
  stepId: LifecycleStepperStepId,
): TemplateJourneyWorkspaceQuery | null {
  switch (stepId) {
    case 'draft':
      return { workspaceTab: 'design' }
    case 'testing':
      return { workspaceTab: 'testing' }
    case 'readyForApproval':
    case 'pendingApproval':
      return { workspaceTab: 'approval', approvalTab: 'submitApproval' }
    case 'pendingRelease':
      return { workspaceTab: 'approval', approvalTab: 'publishReadiness' }
    case 'published':
      return null
    default:
      return null
  }
}
