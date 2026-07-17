type LifecycleDecision = 'PASSED' | 'FAILED' | 'APPROVED' | 'REJECTED'

/** Not yet modeled in `openapi-v1.yaml` (management lifecycle comment). */
export interface LifecycleCommentPayload {
  commentSummary?: string
}

/** Not yet modeled in `openapi-v1.yaml` (management lifecycle governance). */
export interface LifecycleGovernancePayload {
  reason: string
  confirmed: boolean
}

export type LifecycleGovernanceAction =
  | 'STOP'
  | 'RESTORE'
  | 'DEPRECATE'
  | 'DEACTIVATE_VERSION'
  | 'RESTORE_VERSION'

/** Not yet modeled in `openapi-v1.yaml` (management lifecycle impact preview request). */
export interface LifecycleImpactPreviewRequest {
  action: LifecycleGovernanceAction
  releaseVersion?: string
}

/** Not yet modeled in `openapi-v1.yaml` (management lifecycle impact preview). */
export interface LifecycleImpactPreview {
  action: LifecycleGovernanceAction
  releaseVersion: string | null
  callableReleaseVersions: string[]
  defaultRouteReleaseVersion: string | null
  defaultRouteImpacted: boolean
  summaryMessageKey: string
}

/** Not yet modeled in `openapi-v1.yaml` (management lifecycle decision). */
export interface LifecycleDecisionPayload {
  decision: LifecycleDecision
  commentSummary?: string
  reasonCategory?: string
  impactSummary?: string
  fidelityViewedConfirmed?: boolean
  coverageViewedConfirmed?: boolean
  previewViewedConfirmed?: boolean
  keyEvidenceConfirmed?: boolean
  remediationTestRecordId?: string
  remediationChangeDiffRef?: string
  remediationChecklistCode?: string
  exceptionIntervention?: boolean
  exceptionReason?: string
  secondaryConfirmed?: boolean
}

export type PublishGateCheckCode =
  | 'ANCHOR_INTEGRITY'
  | 'VARIABLE_SCHEMA'
  | 'RULE_BOUNDS'
  | 'TEST_RESULTS'
  | 'PREVIEW_PRESENT'
  | 'CHANGE_DIFF'
  | 'APPROVAL_SUMMARY'
  | 'COVERAGE_THRESHOLDS'
  | 'API_POLICY'
  | 'CONTENT_MODULE_REFERENCES'
  | 'CONTENT_MODULE_EFFECTIVE_EXPIRED'
  | 'UNSUPPORTED_STRUCTURED_NODES'
  | 'PASTE_CLEANING_BLOCKERS'
  | 'PAGINATION_DELTA_BUDGET'
  | 'BLOCKER_STATUS'
  | 'FIDELITY_WARNINGS_VIEWED'

/** Not yet modeled in `openapi-v1.yaml` (management publish gate). */
export interface PublishGateItem {
  checkCode: PublishGateCheckCode
  ready: boolean
  blocker: boolean
  messageKey: string
  summary: string
}

/** Not yet modeled in `openapi-v1.yaml` (management publish gate). */
export interface PublishGateChecklist {
  templateId: string
  ready: boolean
  blockerCount: number
  items: PublishGateItem[]
}

/** Not yet modeled in `openapi-v1.yaml` (management risk prompt config). */
export interface RiskPromptConfig {
  scopeType: 'GLOBAL'
  groupCode: null
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
  updatedAt: string
}

/** Not yet modeled in `openapi-v1.yaml` (management risk prompt config). */
export interface UpsertGlobalRiskPromptConfigPayload {
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
}

/** Not yet modeled in `openapi-v1.yaml` (management risk prompt config). */
export interface TemplateRiskPromptConfig {
  useDefault: boolean
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
  updatedAt: string
}

/** Not yet modeled in `openapi-v1.yaml` (management risk prompt config). */
export interface UpsertTemplateRiskPromptConfigPayload {
  useDefault: boolean
  reasonCategories?: string[]
  riskPromptCopy?: Record<string, string>
}

/** UI-only form state; not an API DTO. */
export interface DecisionFormConfig {
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
}

/** UI-only form state; not an API DTO. */
export interface TemplateRiskPromptFormState {
  customize: boolean
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
}

/** Not yet modeled in `openapi-v1.yaml` (management publish). */
export interface PublishTemplatePayload {
  releaseVersion: string
  /** Release Summary fidelity-viewed confirmation (BDD-CDP-FID-003). */
  fidelityViewedConfirmed?: boolean
}

/** Not yet modeled in `openapi-v1.yaml` (management template delete). */
export interface DeleteTemplatePayload {
  reason: string
}
