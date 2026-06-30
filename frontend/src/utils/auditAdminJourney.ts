import { MANAGEMENT_ROLES } from '@/auth/roles'

export interface AuditAdminJourneyFilters {
  eventType?: string
  eventAtFrom?: string
  eventAtTo?: string
  groupScope?: string
  templateId?: string
}

export interface AuditAdminJourneyContext {
  filters?: AuditAdminJourneyFilters
  managementEventCount?: number
  lifecycleEventCount?: number
  exportInProgress?: boolean
  exportJustCompleted?: boolean
}

export interface AuditAdminJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
}

const EMPTY_GUIDANCE = 'journey.roles.AUDIT_ADMIN.empty.guidance'

const STEP_INDEX = {
  openActivityLog: 0,
  searchAndFilter: 1,
  reviewEntries: 2,
  exportRecords: 3,
  viewOnlyMode: 4,
} as const

export function hasActiveAuditFilters(filters: AuditAdminJourneyFilters = {}): boolean {
  return Boolean(
    filters.eventType?.trim() ||
      filters.eventAtFrom?.trim() ||
      filters.eventAtTo?.trim() ||
      filters.groupScope?.trim() ||
      filters.templateId?.trim(),
  )
}

export function hasLoadedAuditEvents(context: AuditAdminJourneyContext): boolean {
  return (context.managementEventCount ?? 0) > 0 || (context.lifecycleEventCount ?? 0) > 0
}

export function resolveAuditAdminJourneyIndex(
  context: AuditAdminJourneyContext = {},
): AuditAdminJourneyResolution {
  if (context.exportInProgress || context.exportJustCompleted) {
    return {
      currentStepIndex: STEP_INDEX.exportRecords,
      activeStepId: 'exportRecords',
    }
  }

  if (hasLoadedAuditEvents(context)) {
    return {
      currentStepIndex: STEP_INDEX.reviewEntries,
      activeStepId: 'reviewEntries',
    }
  }

  if (hasActiveAuditFilters(context.filters)) {
    return {
      currentStepIndex: STEP_INDEX.searchAndFilter,
      activeStepId: 'searchAndFilter',
    }
  }

  return {
    currentStepIndex: null,
    guidanceKey: EMPTY_GUIDANCE,
    activeStepId: 'openActivityLog',
  }
}

export function auditAdminStepCtaKey(stepId: string): string {
  return `journey.roles.AUDIT_ADMIN.steps.${stepId}.cta`
}

export function shouldShowAuditAdminJourney(options: { roles: string[] }): boolean {
  return options.roles.includes(MANAGEMENT_ROLES.AUDIT_ADMIN)
}
