export type ContentModuleReviewState = 'DRAFT' | 'SUBMITTED' | 'APPROVED'

export type ContentModuleLifecycleState = 'ACTIVE' | 'STOPPED' | 'DEPRECATED'

export type ContentModuleReviewOperation =
  | 'SUBMIT_FOR_REVIEW'
  | 'APPROVE_REVIEW'
  | 'REJECT_REVIEW'

export type ContentModuleLifecycleOperation = 'STOP_USE' | 'RECOVER' | 'DEPRECATE'

export type ContentModuleGovernanceActorRole =
  | 'GLOBAL_ADMIN'
  | 'GROUP_ADMIN'
  | 'APPROVER'
  | 'TEMPLATE_AUTHOR'
  | 'MASTER_DESIGNER'

export interface ContentModuleVersion {
  versionId: string
  semanticVersion: string
  reviewState: ContentModuleReviewState
  lifecycleState?: ContentModuleLifecycleState
  /** Present for authoring roles when loading draft versions for edit. */
  contentStructureJson?: string | null
  changeDescription?: string | null
  createdAt: string
  updatedAt: string
}

export interface ContentModuleSummary {
  moduleId: string
  moduleCode: string
  groupCode: string
  name: string
  description?: string | null
  sharedGroupCodes?: string[]
  createdAt: string
  updatedAt: string
}

export interface ContentModuleDetail {
  moduleId: string
  moduleCode: string
  groupCode: string
  name: string
  description?: string | null
  sharedGroupCodes?: string[]
  versions: ContentModuleVersion[]
}

export interface CreateContentModulePayload {
  moduleCode: string
  groupCode: string
  name: string
  description?: string
  sharedGroupCodes?: string[]
  semanticVersion: string
  contentStructureJson: string
  changeDescription?: string
}

export interface CreateContentModuleVersionPayload {
  semanticVersion: string
  contentStructureJson: string
  changeDescription?: string
}

export interface UpdateContentModuleVersionPayload {
  contentStructureJson: string
  changeDescription?: string
}

export interface ContentModuleReviewTransitionPayload {
  operation: ContentModuleReviewOperation
  actorRole: ContentModuleGovernanceActorRole
  actorId: string
  changeDescription?: string
  rejectionReason?: string
}

export interface ContentModuleReviewSnapshot {
  moduleId: string
  state: ContentModuleReviewState
  updatedAt: string
  updatedBy: string
  rejectionReason?: string | null
}

export interface ContentModuleReviewTransitionResult {
  applied: boolean
  errorCode?: string | null
  errorMessage?: string | null
  snapshot: ContentModuleReviewSnapshot
}

export interface ContentModuleLifecycleImpactSummary {
  referenceTemplateCount: number
  referenceTemplateListHint: string
  impactedReleaseVersionsHint: string
  defaultRouteAffected: boolean
  recentCallSummary: string
  remediationHint: string
}

export interface ContentModuleLifecycleOperationPayload {
  operationType: ContentModuleLifecycleOperation
  actorRole: ContentModuleGovernanceActorRole
  actorId: string
  impactSummaryViewed: boolean
  secondConfirmation: boolean
  impactSummary?: ContentModuleLifecycleImpactSummary
}

export interface ContentModuleLifecycleSnapshot {
  moduleId: string
  state: ContentModuleLifecycleState
  updatedAt: string
  updatedBy: string
}

export interface ContentModuleLifecycleOperationResult {
  applied: boolean
  errorCode?: string | null
  errorMessage?: string | null
  snapshot: ContentModuleLifecycleSnapshot
  impactSummary?: ContentModuleLifecycleImpactSummary
}
