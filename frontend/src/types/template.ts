export type TemplateLifecycleStatus =
  | 'DRAFT'
  | 'TESTING'
  | 'APPROVAL'
  | 'PENDING_RELEASE'
  | 'PUBLISHED'
  | 'STOPPED'
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
  masterId: string
  updatedAt: string
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
  devVersionId: string
  devVersionNumber: number
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  createdAt: string
  updatedAt: string
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

export interface LifecycleDecisionPayload {
  decision: LifecycleDecision
  commentSummary?: string
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

export interface TestDataSet {
  testDataSetId: string
  templateId: string
  name: string
  description: string | null
  variables: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface UpsertTestDataSetPayload {
  name: string
  description?: string
  variables: Record<string, unknown>
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
