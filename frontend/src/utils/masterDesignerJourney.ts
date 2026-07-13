import type {
  MasterDocumentStatus,
  MasterDocumentSummary,
  MasterReviewRecord,
} from '@/types/master'
import { MANAGEMENT_ROLES } from '@/auth/roles'

export interface MasterDesignerJourneyContext {
  status: MasterDocumentStatus
  originalFilename?: string | null
  anchorCount?: number
  anchorsLength?: number
  reviewHistory?: MasterReviewRecord[]
}

export interface MasterDesignerJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
  targetMasterId?: string
}

export type MasterDesignerDashboardMaster = MasterDocumentSummary & {
  reviewHistory?: MasterReviewRecord[]
}

const WAITING_REVIEW_GUIDANCE = 'journey.roles.MASTER_DESIGNER.waitingReview.guidance'
const COMPLETE_GUIDANCE = 'journey.roles.MASTER_DESIGNER.complete.guidance'
const EMPTY_GUIDANCE = 'journey.roles.MASTER_DESIGNER.empty.guidance'

function hasMasterUploadedFile(originalFilename?: string | null): boolean {
  return Boolean(originalFilename && originalFilename.trim().length > 0)
}

function masterPlaceholdersSatisfied(context: {
  anchorCount?: number
  anchorsLength?: number
}): boolean {
  if (context.anchorCount !== undefined && context.anchorCount > 0) {
    return true
  }
  if (context.anchorsLength !== undefined && context.anchorsLength > 0) {
    return true
  }
  return false
}

export function isMasterReworkState(
  status: MasterDocumentStatus,
  reviewHistory?: MasterReviewRecord[],
): boolean {
  if (status === 'REJECTED') {
    return true
  }
  if (status !== 'DRAFT' || !reviewHistory || reviewHistory.length === 0) {
    return false
  }
  const latest = reviewHistory[reviewHistory.length - 1]
  return latest?.decision === 'REJECTED' || latest?.action === 'REJECTED'
}

export function resolveMasterDesignerJourneyIndex(
  context: MasterDesignerJourneyContext,
): MasterDesignerJourneyResolution {
  if (!hasMasterUploadedFile(context.originalFilename)) {
    return { currentStepIndex: 0, activeStepId: 'upload' }
  }

  if (!masterPlaceholdersSatisfied(context)) {
    return { currentStepIndex: 1, activeStepId: 'placeholders' }
  }

  if (isMasterReworkState(context.status, context.reviewHistory)) {
    return { currentStepIndex: 3, activeStepId: 'rework' }
  }

  if (context.status === 'DRAFT') {
    return { currentStepIndex: 2, activeStepId: 'submitReview' }
  }

  if (context.status === 'PENDING_REVIEW') {
    return { currentStepIndex: null, guidanceKey: WAITING_REVIEW_GUIDANCE }
  }

  if (context.status === 'APPROVED') {
    return { currentStepIndex: null, guidanceKey: COMPLETE_GUIDANCE }
  }

  return { currentStepIndex: null }
}

function pickNewestMaster(
  masters: MasterDesignerDashboardMaster[],
): MasterDesignerDashboardMaster | undefined {
  if (masters.length === 0) {
    return undefined
  }
  return [...masters].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  )[0]
}

function withTargetMaster(
  resolution: Omit<MasterDesignerJourneyResolution, 'targetMasterId'>,
  master: MasterDesignerDashboardMaster | undefined,
): MasterDesignerJourneyResolution {
  return {
    ...resolution,
    targetMasterId: master?.id,
  }
}

export function resolveMasterDesignerDashboardJourneyIndex(
  masters: MasterDesignerDashboardMaster[],
): MasterDesignerJourneyResolution {
  if (masters.length === 0) {
    return { currentStepIndex: 0, activeStepId: 'upload', guidanceKey: undefined }
  }

  const reworkMasters = masters.filter((master) =>
    isMasterReworkState(master.status, master.reviewHistory),
  )
  if (reworkMasters.length > 0) {
    const target = pickNewestMaster(reworkMasters)
    return withTargetMaster({ currentStepIndex: 3, activeStepId: 'rework' }, target)
  }

  const draftReady = masters.filter(
    (master) =>
      master.status === 'DRAFT' &&
      hasMasterUploadedFile(master.originalFilename) &&
      masterPlaceholdersSatisfied({ anchorCount: master.anchorCount }) &&
      !isMasterReworkState(master.status, master.reviewHistory),
  )
  if (draftReady.length > 0) {
    const target = pickNewestMaster(draftReady)
    return withTargetMaster({ currentStepIndex: 2, activeStepId: 'submitReview' }, target)
  }

  const needsPlaceholders = masters.filter(
    (master) =>
      hasMasterUploadedFile(master.originalFilename) &&
      !masterPlaceholdersSatisfied({ anchorCount: master.anchorCount }),
  )
  if (needsPlaceholders.length > 0) {
    const target = pickNewestMaster(needsPlaceholders)
    return withTargetMaster({ currentStepIndex: 1, activeStepId: 'placeholders' }, target)
  }

  const inProgress = masters.filter((master) => master.status !== 'APPROVED')
  const inProgressWithoutFile = inProgress.filter(
    (master) => !hasMasterUploadedFile(master.originalFilename),
  )
  if (inProgressWithoutFile.length > 0) {
    const target = pickNewestMaster(inProgressWithoutFile)
    return withTargetMaster({ currentStepIndex: 0, activeStepId: 'upload' }, target)
  }

  const hasDraftOrRework = masters.some(
    (master) =>
      master.status === 'DRAFT' ||
      master.status === 'REJECTED' ||
      isMasterReworkState(master.status, master.reviewHistory),
  )
  const hasPendingReview = masters.some((master) => master.status === 'PENDING_REVIEW')
  if (hasPendingReview && !hasDraftOrRework) {
    return { currentStepIndex: null, guidanceKey: WAITING_REVIEW_GUIDANCE }
  }

  if (masters.every((master) => master.status === 'APPROVED')) {
    return { currentStepIndex: null, guidanceKey: EMPTY_GUIDANCE }
  }

  const target = pickNewestMaster(inProgress)
  return withTargetMaster({ currentStepIndex: 0, activeStepId: 'upload' }, target)
}

export function masterDesignerStepCtaKey(stepId: string): string {
  return `journey.roles.MASTER_DESIGNER.steps.${stepId}.cta`
}

export function shouldShowMasterDesignerJourney(options: {
  roles: string[]
  manageMasters: boolean
  reviewMasters: boolean
  status?: MasterDocumentStatus
}): boolean {
  const { roles, manageMasters, reviewMasters, status } = options
  const isMasterDesigner = roles.includes(MANAGEMENT_ROLES.MASTER_DESIGNER)
  const isAuditOnly =
    roles.includes(MANAGEMENT_ROLES.AUDIT_ADMIN) &&
    !isMasterDesigner &&
    !manageMasters &&
    roles.every((role) => role === MANAGEMENT_ROLES.AUDIT_ADMIN)

  if (isAuditOnly) {
    return false
  }

  if (reviewMasters && !manageMasters && !isMasterDesigner) {
    return false
  }

  if (!isMasterDesigner && !manageMasters) {
    return false
  }

  if (
    !isMasterDesigner &&
    manageMasters &&
    reviewMasters &&
    status === 'PENDING_REVIEW'
  ) {
    return false
  }

  return true
}
