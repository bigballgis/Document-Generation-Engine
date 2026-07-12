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

export type ContentModuleSummary = Schema<'ContentModuleSummaryView'>

export type ContentModuleDetail = Schema<'ContentModuleDetailView'>

export type CreateContentModulePayload = Schema<'CreateContentModuleRequest'>

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
