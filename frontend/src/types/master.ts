import type { PageView } from '@/types/identity'
import type { Schema } from '@/types/openapi'

export type MasterDocumentStatus = Schema<'MasterDocumentReviewStatus'>

export type MasterRevisionLineLabel = Schema<'MasterRevisionLineLabel'>

export type MasterReviewAction = 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export type MasterReviewDecision = 'APPROVED' | 'REJECTED'

/** Not yet modeled in `openapi-v1.yaml` (management master list/detail). */
export interface MasterDocumentSummary {
  id: string
  groupCode: string
  name: string
  status: MasterDocumentStatus
  originalFilename: string
  anchorCount: number
  updatedBy: string
  updatedAt: string
}

export type MasterAnchor = Schema<'MasterAnchorSummaryView'>

export type MasterReviewRecord = Omit<Schema<'MasterReviewRecordView'>, 'changeSummary' | 'commentSummary'> & {
  changeSummary?: string | null
  commentSummary?: string | null
}

/** Not yet modeled in `openapi-v1.yaml` (management master detail). */
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
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

/** Not yet modeled in `openapi-v1.yaml` (management master impact analysis). */
export interface MasterImpactAnalysis {
  masterId: string
  referencedTemplateIds: string[]
  retestRequired: boolean
}

/** Not yet modeled in `openapi-v1.yaml` (management master create multipart). */
export interface CreateMasterPayload {
  groupCode: string
  name: string
  description?: string
}

/** Not yet modeled in `openapi-v1.yaml` (management master review submit). */
export interface SubmitMasterReviewPayload {
  changeSummary: string
}

/** Not yet modeled in `openapi-v1.yaml` (management master review decision). */
export interface DecideMasterReviewPayload {
  decision: MasterReviewDecision
  commentSummary?: string
}

/** Not yet modeled in `openapi-v1.yaml` (management master metadata update). */
export interface UpdateMasterMetadataPayload {
  name?: string
  description?: string | null
}

export type MasterRevisionLineSummary = Schema<'MasterRevisionLineSummaryView'>

export type MasterRevisionLineDetail = Omit<Schema<'MasterRevisionLineDetailView'>, 'changeSummary'> & {
  changeSummary?: string | null
}

export type MasterRevisionLinePage = PageView<MasterRevisionLineSummary>
