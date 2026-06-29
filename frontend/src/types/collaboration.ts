export type CollaborationWorkItemQueue =
  | 'TEST'
  | 'APPROVAL'
  | 'REMEDIATION'
  | 'PENDING_RELEASE'
  | 'ESCALATION'

export type CollaborationWorkItemTriggerType =
  | 'SUBMIT_FOR_TEST'
  | 'TEST_FAILURE_OR_RETURN_TO_DRAFT'
  | 'SUBMIT_FOR_APPROVAL'
  | 'APPROVAL_FAILURE_OR_RETURN_TO_DRAFT'
  | 'APPROVAL_PENDING_RELEASE'
  | 'TIMEOUT_ESCALATION'

export type CollaborationTimeoutScopeType = 'GLOBAL' | 'GROUP'

export interface CollaborationWorkItemSummary {
  workItemId: string
  templateId: string
  templateName: string
  groupCode: string
  queue: CollaborationWorkItemQueue
  triggerType: CollaborationWorkItemTriggerType
  submitterUserId: string
  summaryText: string
  createdAt: string
  ageSeconds: number
}

export interface CollaborationTimeoutConfig {
  scopeType: CollaborationTimeoutScopeType
  groupCode: string | null
  testThresholdHours: number
  approvalThresholdHours: number
  pendingReleaseThresholdHours: number
  remediationThresholdHours: number
  updatedAt: string
}

export interface UpsertCollaborationTimeoutConfigPayload {
  scopeType: CollaborationTimeoutScopeType
  groupCode: string | null
  testThresholdHours: number
  approvalThresholdHours: number
  pendingReleaseThresholdHours: number
  remediationThresholdHours: number
}

export interface ListCollaborationWorkItemsParams {
  groupCode?: string
  queue?: CollaborationWorkItemQueue
}
