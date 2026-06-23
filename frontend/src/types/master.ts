export type MasterDocumentStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'

export type MasterReviewAction = 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export type MasterReviewDecision = 'APPROVED' | 'REJECTED'

export interface MasterDocumentSummary {
  id: string
  groupCode: string
  name: string
  status: MasterDocumentStatus
  originalFilename: string
  anchorCount: number
  updatedAt: string
}

export interface MasterAnchor {
  anchorId: string
  displayLabel: string
}

export interface MasterReviewRecord {
  action: MasterReviewAction
  decision: MasterReviewDecision | null
  changeSummary: string | null
  commentSummary: string | null
  actorUsername: string
  createdAt: string
}

export interface MasterDocumentDetail {
  id: string
  groupCode: string
  name: string
  description: string | null
  status: MasterDocumentStatus
  originalFilename: string
  changeSummary: string | null
  anchors: MasterAnchor[]
  reviewHistory: MasterReviewRecord[]
  createdAt: string
  updatedAt: string
}

export interface MasterImpactAnalysis {
  masterId: string
  referencedTemplateIds: string[]
  retestRequired: boolean
}

export interface CreateMasterPayload {
  groupCode: string
  name: string
  description?: string
}

export interface SubmitMasterReviewPayload {
  changeSummary: string
}

export interface DecideMasterReviewPayload {
  decision: MasterReviewDecision
  commentSummary?: string
}
