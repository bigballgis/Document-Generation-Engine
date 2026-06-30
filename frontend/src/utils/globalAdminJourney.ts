import type { MasterDocumentSummary } from '@/types/master'
import type { TemplateSummary } from '@/types/template'
import type { CollaborationWorkItemQueue } from '@/types/collaboration'

export interface GlobalAdminCollaborationWorkItem {
  queue: CollaborationWorkItemQueue
  createdAt: string
}

export type GlobalAdminDashboardMaster = Pick<MasterDocumentSummary, 'id' | 'status' | 'updatedAt'>

export type GlobalAdminDashboardTemplate = Pick<TemplateSummary, 'id'>

export interface GlobalAdminJourneyContext {
  deleteTemplates?: boolean
  canMaintainCollaborationTimeoutConfig?: boolean
}

export interface GlobalAdminJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
}

const EMPTY_GUIDANCE = 'journey.roles.GLOBAL_ADMIN.empty.guidance'

const STEP_INDEX = {
  reviewOverview: 0,
  manageUsersGroups: 1,
  removeTemplates: 2,
  setReminderDefaults: 3,
  monitorOverdue: 4,
  reviewAllTodos: 5,
} as const

export function isPendingReviewMaster(
  master: Pick<MasterDocumentSummary, 'status'>,
): boolean {
  return master.status === 'PENDING_REVIEW'
}

function distinctQueuesWithOpenItems(
  items: GlobalAdminCollaborationWorkItem[],
): CollaborationWorkItemQueue[] {
  return [...new Set(items.map((item) => item.queue))]
}

function hasInheritedTeamLeadWork(
  masters: GlobalAdminDashboardMaster[],
  workItems: GlobalAdminCollaborationWorkItem[],
): boolean {
  if (masters.some(isPendingReviewMaster)) {
    return true
  }
  return workItems.some((item) => item.queue === 'PENDING_RELEASE')
}

export function resolveGlobalAdminDashboardJourneyIndex(
  masters: GlobalAdminDashboardMaster[],
  templates: GlobalAdminDashboardTemplate[],
  workItems: GlobalAdminCollaborationWorkItem[],
  context: GlobalAdminJourneyContext = {},
): GlobalAdminJourneyResolution {
  const escalationItems = workItems.filter((item) => item.queue === 'ESCALATION')
  if (escalationItems.length > 0) {
    return {
      currentStepIndex: STEP_INDEX.monitorOverdue,
      activeStepId: 'monitorOverdue',
    }
  }

  if (hasInheritedTeamLeadWork(masters, workItems)) {
    return {
      currentStepIndex: STEP_INDEX.reviewAllTodos,
      activeStepId: 'reviewAllTodos',
    }
  }

  const openQueues = distinctQueuesWithOpenItems(workItems)
  if (openQueues.length >= 2) {
    return {
      currentStepIndex: STEP_INDEX.reviewAllTodos,
      activeStepId: 'reviewAllTodos',
    }
  }

  if (context.canMaintainCollaborationTimeoutConfig) {
    return {
      currentStepIndex: STEP_INDEX.setReminderDefaults,
      activeStepId: 'setReminderDefaults',
    }
  }

  if (context.deleteTemplates && templates.length > 0) {
    return {
      currentStepIndex: STEP_INDEX.removeTemplates,
      activeStepId: 'removeTemplates',
    }
  }

  return {
    currentStepIndex: null,
    guidanceKey: EMPTY_GUIDANCE,
    activeStepId: 'reviewOverview',
  }
}

export function globalAdminStepCtaKey(stepId: string): string {
  return `journey.roles.GLOBAL_ADMIN.steps.${stepId}.cta`
}

export function shouldShowGlobalAdminJourney(options: { roles: string[] }): boolean {
  return options.roles.includes('GLOBAL_ADMIN')
}
