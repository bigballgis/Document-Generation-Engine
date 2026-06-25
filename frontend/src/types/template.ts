export type TemplateLifecycleStatus =
  | 'DRAFT'
  | 'TESTING'
  | 'APPROVAL'
  | 'PENDING_RELEASE'
  | 'PUBLISHED'
  | 'STOPPED'
  | 'DELETED'
  | 'DEPRECATED'

export type LifecycleDecision = 'PASSED' | 'FAILED' | 'APPROVED' | 'REJECTED'

export type PreviewStatus = 'ACCEPTED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED'

export interface TemplateSummary {
  id: string
  externalId: string
  groupCode: string
  name: string
  lifecycleStatus: TemplateLifecycleStatus
  releaseVersion: string | null
  releaseVersionCount: number
  masterId: string
  updatedBy: string
  updatedAt: string
}

export interface TemplateReleaseVersion {
  releaseVersion: string
  devVersionNumber: number
  lifecycleStatus: TemplateLifecycleStatus
  updatedAt: string
  updatedBy: string
  defaultRouteTarget: boolean
}

export interface VariableSchema {
  variableKey: string
  variableType: string
  required: boolean
  defaultValue: string | null
  enumValues: string[]
  description: string | null
}

export interface AnchorBinding {
  anchorId: string
  declaredContentType: string
  structuredContentJson: string | null
  validationStatus?: string
}

export interface CompositionRule {
  ruleId: string
  conditionExpression: string
  targetAnchorId: string
  trueBranchRuleId?: string | null
  falseBranchRuleId?: string | null
}

export interface TemplateDetail {
  id: string
  externalId: string
  groupCode: string
  name: string
  description: string | null
  masterId: string
  lifecycleStatus: TemplateLifecycleStatus
  releaseVersion: string | null
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION'
  devVersionId: string
  devVersionNumber: number
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[]
  createdAt: string
  updatedAt: string
}

export interface UpsertVariablePayload {
  variableKey: string
  variableType: string
  required: boolean
  defaultValue?: string | null
  enumValues?: string | null
  description?: string | null
}

export interface UpsertBindingPayload {
  anchorId: string
  declaredContentType: string
  structuredContentJson: string
}

export interface CreateTemplatePayload {
  externalId: string
  groupCode: string
  name: string
  masterId: string
  description?: string
}

export interface LifecycleCommentPayload {
  commentSummary: string
}

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

export interface LifecycleImpactPreviewRequest {
  action: LifecycleGovernanceAction
  releaseVersion?: string
}

export interface LifecycleImpactPreview {
  action: LifecycleGovernanceAction
  releaseVersion: string | null
  callableReleaseVersions: string[]
  defaultRouteReleaseVersion: string | null
  defaultRouteImpacted: boolean
  summaryMessageKey: string
}

export interface UpdateTemplateMetadataPayload {
  name?: string
  description?: string | null
}

export interface LifecycleDecisionPayload {
  decision: LifecycleDecision
  commentSummary?: string
  reasonCategory?: string
  impactSummary?: string
}

export interface PublishTemplatePayload {
  releaseVersion: string
}

export interface PreviewRecord {
  previewId: string
  templateId: string
  templateVersionId: string
  status: PreviewStatus
  outputFormat: string
  artifactStorageKey: string | null
  fidelityWarnings: Array<{ messageKey: string }>
  comparisonSummary: string | null
  testDataSetId: string | null
  createdAt: string
}

export type RuleValidationStatus =
  | 'VALID'
  | 'MISSING_VARIABLE'
  | 'MISSING_ANCHOR'
  | 'INVALID_BRANCH_REFERENCE'
  | 'MALFORMED_RULE'

export interface CompositionRuleInput {
  ruleId: string
  conditionExpression: string
  targetAnchorId: string
  trueBranchRuleId?: string
  falseBranchRuleId?: string
}

export interface RuleValidationItem {
  ruleId: string
  conditionExpression: string
  targetAnchorId: string
  trueBranchRuleId: string | null
  falseBranchRuleId: string | null
  status: RuleValidationStatus
}

export interface RuleValidationSummary {
  blocking: boolean
  totalRules: number
  validCount: number
  missingVariableCount: number
  missingAnchorCount: number
  invalidBranchReferenceCount: number
  malformedRuleCount: number
}

export interface RuleValidationResult {
  validated: boolean
  rules: RuleValidationItem[]
  summary: RuleValidationSummary
}

export interface BindingValidationSummary {
  blocking: boolean
  totalBindings: number
  validCount: number
  missingAnchorCount: number
  duplicateBindingCount: number
  incompatibleContentTypeCount: number
}

export interface BindingValidationResult {
  bindings: AnchorBinding[]
  summary: BindingValidationSummary
}

export interface TestGeneratePayload {
  variables?: Record<string, unknown>
  testDataSetId?: string
}

export interface BatchTestGeneratePayload {
  testDataSetIds: string[]
}

export interface BatchTestSampleResult {
  testDataSetId: string
  previewId: string
  status: PreviewStatus
  warningCount: number
  blockerCount: number
}

export interface BatchTestSummary {
  batchTestRunId: string
  templateId: string
  totalSamples: number
  succeededCount: number
  failedCount: number
  warningCount: number
  blockerCount: number
  samples: BatchTestSampleResult[]
  createdAt: string
}

export interface TestDataSet {
  testDataSetId: string
  templateId: string
  name: string
  description: string | null
  variables: Record<string, unknown>
  required: boolean
  scenarioName: string | null
  coverageTags: string[]
  datasetVersion: number
  locked: boolean
  derivedFromId: string | null
  createdAt: string
  updatedAt: string
}

export interface UpsertTestDataSetPayload {
  name: string
  description?: string
  variables: Record<string, unknown>
  required?: boolean
  scenarioName?: string
  coverageTags?: string[]
}

export interface ApiPolicy {
  templateId: string
  policyVersion: number
  allowedAdGroups: string[]
  defaultRouteReleaseVersion: string
  outputFormats: string[]
  outputModes: string[]
  batchEnabled: boolean
  maxBatchSize: number
  docxEncryptionEnabled: boolean
  pdfEncryptionEnabled: boolean
  updatedAt: string
}

export interface UpsertApiPolicyPayload {
  allowedAdGroups: string[]
  defaultRouteReleaseVersion: string
  outputFormats: string[]
  outputModes: string[]
  batchEnabled: boolean
  maxBatchSize: number
  docxEncryptionEnabled: boolean
  pdfEncryptionEnabled: boolean
}

export interface ApiPolicyImpactPreview {
  currentPolicyVersion: number | null
  nextPolicyVersion: number | null
  changedFields: string[]
}

export interface ApiCredentialSummary {
  credentialId: string
  externalId: string
  status: string
  createdAt: string
  revokedAt: string | null
}

export interface ApiCredentialCreated {
  credentialId: string
  externalId: string
  secret: string
  status: string
  createdAt: string
}

export interface DeleteTemplatePayload {
  reason: string
}
