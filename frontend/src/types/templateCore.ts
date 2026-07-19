import type { ApprovalMatrixMode, ApprovalStage, ApprovalSubState } from '@/types/approvalMatrix'
import type { Schema } from '@/types/openapi'
import type { PasteCleaningEvidence } from '@/types/templatePaste'

export type { ApprovalMatrixMode, ApprovalStage, ApprovalSubState }

/**
 * OpenAPI-backed management DTO aliases. Types without a matching schema remain
 * hand-written below with a short comment when not yet in `openapi-v1.yaml`.
 */
export type TemplateLifecycleStatus = Schema<'TemplateLifecycleStatus'> | 'DELETED'

export type PreviewStatus = Schema<'PreviewStatus'>

/** Not yet modeled in `openapi-v1.yaml` (management template list). */
export interface TemplateSummary {
  id: string
  externalId: string
  groupCode: string
  name: string
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: ApprovalSubState | null
  releaseVersion: string | null
  releaseVersionCount: number
  masterId: string
  updatedBy: string
  updatedByDisplayName?: string | null
  updatedAt: string
  /** CE-G05 — UTC calendar date; null when not seeded. */
  nextReviewDue?: string | null
  /** IBL-E1 — authored body locale (BCP-47). */
  locale?: string
  /** IBL-E1 — optional locale-variant family grouping. */
  localeVariantFamilyId?: string | null
  /** IBL-E3 — package approval matrix mode (default SINGLE_TRACK). */
  approvalMatrixMode?: ApprovalMatrixMode
  /** IBL-E3 — current multi-stage stage when applicable. */
  approvalStage?: ApprovalStage | null
}

/** Not yet modeled in `openapi-v1.yaml` (management release version history). */
export interface TemplateReleaseVersion {
  releaseVersion: string
  devVersionNumber: number
  lifecycleStatus: TemplateLifecycleStatus
  updatedAt: string
  updatedBy: string
  updatedByDisplayName?: string | null
  defaultRouteTarget: boolean
}

export type TemplateVersionLineSummary = Omit<
  Schema<'TemplateVersionLineSummaryView'>,
  'lifecycleStatus'
> & {
  lifecycleStatus: TemplateLifecycleStatus
  updatedByDisplayName?: string | null
}

/** CE-U19 / CE-E01 — optional CE-K01 master revision pin on release detail. */
export type TemplateMasterPin = Schema<'TemplateExportMasterPinView'>

export type TemplateVersionLineDetail = Omit<
  Schema<'TemplateVersionLineDetailView'>,
  'lifecycleStatus'
> & {
  lifecycleStatus: TemplateLifecycleStatus
  updatedByDisplayName?: string | null
  /** CE-U19 — optional pin when reading published version-line detail. */
  masterPin?: TemplateMasterPin | null
}

export type TemplateDetail = Omit<
  Schema<'TemplateDetailView'>,
  | 'lifecycleStatus'
  | 'approvalSubState'
  | 'releaseVersion'
  | 'locale'
  | 'localeVariantFamilyId'
  | 'approvalMatrixMode'
  | 'approvalStage'
> & {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: ApprovalSubState | null
  releaseVersion: string | null
  updatedBy?: string | null
  updatedByDisplayName?: string | null
  /** CE-U19 — present on published release GET when CE-K01 pin fields exist. */
  masterPin?: TemplateMasterPin | null
  /** CE-G05 — UTC calendar date; null when not seeded (OpenAPI field pending codegen). */
  nextReviewDue?: string | null
  /** IBL-E1 — authored body locale (BCP-47). */
  locale?: string
  /** IBL-E1 — optional locale-variant family grouping. */
  localeVariantFamilyId?: string | null
  /** IBL-E3 — package approval matrix mode (default SINGLE_TRACK). */
  approvalMatrixMode?: ApprovalMatrixMode
  /** IBL-E3 — current multi-stage stage when applicable. */
  approvalStage?: ApprovalStage | null
}

/** CE-G05 — Dashboard Tasks projection for annual review due. */
export interface AnnualReviewDueAuthorTask {
  templateId: string
  externalId: string
  groupCode: string
  name: string
  nextReviewDue: string
  lifecycleStatus: TemplateLifecycleStatus
  updatedAt: string
}

/** CE-G05 — optional body for POST …/annual-review/complete. */
export interface CompleteTemplateAnnualReviewPayload {
  nextReviewDue?: string
}

export type TemplateDevVersionCreated = Schema<'TemplateDevVersionCreatedView'> & {
  lifecycleStatus?: TemplateLifecycleStatus
}

/** CE-G03 — optional PII classification on variable schema (UPPER_SNAKE_CASE). */
export type VariablePiiCategory =
  | 'NONE'
  | 'PERSONAL_NAME'
  | 'GOVERNMENT_ID'
  | 'FINANCIAL_ACCOUNT'
  | 'CONTACT'
  | 'ADDRESS'
  | 'OTHER_SENSITIVE'

export type VariableSchema = Schema<'TemplateExportVariableSchemaView'> & {
  computeExpression?: string | null
  /** CE-G03 — omitted / null / NONE = not PII-governed. */
  piiCategory?: VariablePiiCategory | null
}

export type AnchorBinding = Schema<'TemplateExportAnchorBindingView'>

export type CompositionRule = Schema<'TemplateExportCompositionRuleView'>

/** Not yet modeled in `openapi-v1.yaml` (management variable upsert). */
export interface UpsertVariablePayload {
  variableKey: string
  variableType: string
  required: boolean
  defaultValue?: string | null
  enumValues?: string | null
  description?: string | null
  computeExpression?: string | null
  /** CE-G03 — optional; omit or NONE = not PII-governed. */
  piiCategory?: VariablePiiCategory | null
}

/** Management binding upsert — aligns with OpenAPI `UpsertAnchorBindingRequest`. */
export interface UpsertBindingPayload {
  anchorId: string
  declaredContentType: string
  structuredContentJson: string
  /** Non-sensitive paste-cleaning residue (ops-paste-binding-seam / ADR-0019). */
  pasteCleaningEvidence?: PasteCleaningEvidence | null
  /** When true, clears persisted paste-cleaning residue (S5 clean rewrite). */
  clearPasteCleaningEvidence?: boolean
  /**
   * CE-U21 — concurrency token; required when updating an existing binding.
   * Omit on first create. Stale value → 409 BINDING_VERSION_CONFLICT.
   */
  expectedUpdatedAt?: string | null
}

/** Not yet modeled in `openapi-v1.yaml` (management template create). */
export interface CreateTemplatePayload {
  externalId: string
  groupCode: string
  name: string
  masterId: string
  description?: string
  /** IBL-E1 — required BCP-47 body locale. */
  locale: string
  /** IBL-E1 — optional locale-variant family id. */
  localeVariantFamilyId?: string | null
  /** IBL-E3 — optional package approval matrix mode (omitted → SINGLE_TRACK). */
  approvalMatrixMode?: ApprovalMatrixMode
}

/** Not yet modeled in `openapi-v1.yaml` (management template metadata update). */
export interface UpdateTemplateMetadataPayload {
  name?: string
  description?: string | null
  /** IBL-E1 — optional body locale update. */
  locale?: string
  /** IBL-E1 — optional locale-variant family id update. */
  localeVariantFamilyId?: string | null
  /** IBL-E3 — optional package approval matrix mode update. */
  approvalMatrixMode?: ApprovalMatrixMode
}
