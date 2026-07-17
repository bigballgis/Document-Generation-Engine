import type { Schema } from '@/types/openapi'
import type { PasteCleaningEvidence } from '@/types/templatePaste'

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
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
  releaseVersion: string | null
  releaseVersionCount: number
  masterId: string
  updatedBy: string
  updatedByDisplayName?: string | null
  updatedAt: string
  /** CE-G05 — UTC calendar date; null when not seeded. */
  nextReviewDue?: string | null
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
  'lifecycleStatus' | 'approvalSubState' | 'releaseVersion'
> & {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
  releaseVersion: string | null
  updatedBy?: string | null
  updatedByDisplayName?: string | null
  /** CE-U19 — present on published release GET when CE-K01 pin fields exist. */
  masterPin?: TemplateMasterPin | null
  /** CE-G05 — UTC calendar date; null when not seeded (OpenAPI field pending codegen). */
  nextReviewDue?: string | null
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
}

/** Not yet modeled in `openapi-v1.yaml` (management template metadata update). */
export interface UpdateTemplateMetadataPayload {
  name?: string
  description?: string | null
}
