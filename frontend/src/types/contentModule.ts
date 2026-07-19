import type { Schema } from '@/types/openapi'

export type ContentModuleReviewState = Schema<'ContentModuleVersionView'>['reviewState']

export type ContentModuleLifecycleState = NonNullable<
  Schema<'ContentModuleVersionView'>['lifecycleState']
>


export type ContentModuleLifecycleOperation =
  Schema<'ContentModuleLifecycleOperationApplyRequest'>['operationType']

export type ContentModuleGovernanceActorRole =
  Schema<'ContentModuleReviewTransitionRequest'>['actorRole']

export type ContentModuleVersion = Schema<'ContentModuleVersionView'>

/** IBL-E1 locale fields pending OpenAPI codegen sync. */
export type ContentModuleLocaleFields = {
  locale?: string
  localeVariantFamilyId?: string | null
}

export type ContentModuleSummary = Schema<'ContentModuleSummaryView'> & ContentModuleLocaleFields

export type ContentModuleDetail = Schema<'ContentModuleDetailView'> & ContentModuleLocaleFields

export type CreateContentModulePayload = Schema<'CreateContentModuleRequest'> & {
  /** IBL-E1 — required BCP-47 body locale. */
  locale: string
  /** IBL-E1 — optional locale-variant family id. */
  localeVariantFamilyId?: string | null
}

/** Minimal update body for PUT …/content-modules/{id}/shared-group-codes (CE-U10 / U10-C4). */
export type UpdateContentModuleSharedGroupCodesPayload = {
  sharedGroupCodes: string[]
}

export type CreateContentModuleVersionPayload = Schema<'CreateContentModuleVersionRequest'>

export type UpdateContentModuleVersionPayload = Schema<'UpdateContentModuleVersionRequest'>

export type ContentModuleReviewTransitionPayload = Schema<'ContentModuleReviewTransitionRequest'>

export type ContentModuleReviewSnapshot = Schema<'ContentModuleReviewSnapshot'>

export type ContentModuleReviewTransitionResult = Schema<'ContentModuleReviewTransitionResult'>

export type ContentModuleLifecycleImpactSummary = Schema<'ContentModuleLifecycleImpactSummary'>

export type ContentModuleLifecycleOperationPayload =
  Schema<'ContentModuleLifecycleOperationApplyRequest'>

export type ContentModuleLifecycleSnapshot = Schema<'ContentModuleLifecycleSnapshot'>

export type ContentModuleLifecycleOperationResult = Schema<'ContentModuleLifecycleOperationResult'>

export type ContentModuleReviewRecord = Schema<'ContentModuleReviewRecordView'>

export type ContentModuleWorkflowTask = Schema<'ContentModuleWorkflowTaskView'>

/** CE-G05 — catalog search mode (`NAME` default ILIKE; `FULL_TEXT` tsvector). */
export type ContentModuleSearchMode = 'NAME' | 'FULL_TEXT'

/** CE-G05 — where-used row (authorized templates referencing the module). */
export interface ContentModuleWhereUsedTemplate {
  id: string
  externalId: string
  name: string
  groupCode: string
  lifecycleStatus: string
  pinnedSemanticVersion?: string | null
}
