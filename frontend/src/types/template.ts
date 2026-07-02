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
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION'
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

export interface TemplateVersionLineSummary {
  devVersionId: string
  devVersionNumber: number
  releaseVersion: string | null
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION'
  lineKind?: 'IN_FLIGHT' | 'PUBLISHED'
  updatedAt: string
  updatedBy: string
  defaultRouteTarget: boolean | null
  cloneable?: boolean
}

export interface TemplateVersionLineDetail extends TemplateVersionLineSummary {
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[]
}

export interface TemplateDevVersionCreated {
  devVersionId: string
  devVersionNumber: number
  lifecycleStatus: TemplateLifecycleStatus
}

export interface VariableSchema {
  variableKey: string
  variableType: string
  required: boolean
  defaultValue: string | null
  enumValues: string[]
  description: string | null
  computeExpression?: string | null
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
  computeExpression?: string | null
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
  commentSummary?: string
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
  | 'BLOCKER_STATUS'

export interface PublishGateItem {
  checkCode: PublishGateCheckCode
  ready: boolean
  blocker: boolean
  messageKey: string
  summary: string
}

export interface PublishGateChecklist {
  templateId: string
  ready: boolean
  blockerCount: number
  items: PublishGateItem[]
}

export type ChangeDiffDimensionCode =
  | 'CONTENT'
  | 'ANCHORS'
  | 'VARIABLES'
  | 'RULES'
  | 'CONTRACT_SUMMARY'

export interface ChangeDiffModification {
  key: string
  changeType: string
  summary: string
}

export interface ChangeDiffDimension {
  dimension: ChangeDiffDimensionCode
  added: string[]
  removed: string[]
  modified: ChangeDiffModification[]
}

export interface ChangeDiffSummary {
  templateId: string
  baselineReleaseVersion: string | null
  candidateVersionId: string
  hasChanges: boolean
  totalChangeCount: number
  dimensions: ChangeDiffDimension[]
}

export type PreviewComparisonLocationType = 'PAGE' | 'ANCHOR' | 'SECTION' | 'COMPONENT'
export type PreviewComparisonSeverity = 'WARNING' | 'BLOCKER'

export interface PreviewComparisonItem {
  locationType: PreviewComparisonLocationType
  locationRef: string
  severity: PreviewComparisonSeverity
  diffCode: string
  summary: string
}

export interface PreviewComparison {
  totalDiffCount: number
  blockerCount: number
  warningCount: number
  items: PreviewComparisonItem[]
}

export interface RiskPromptConfig {
  scopeType: 'GLOBAL'
  groupCode: null
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
  updatedAt: string
}

export interface UpsertGlobalRiskPromptConfigPayload {
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
}

export interface TemplateRiskPromptConfig {
  useDefault: boolean
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
  updatedAt: string
}

export interface UpsertTemplateRiskPromptConfigPayload {
  useDefault: boolean
  reasonCategories?: string[]
  riskPromptCopy?: Record<string, string>
}

export interface DecisionFormConfig {
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
}

export interface TemplateRiskPromptFormState {
  customize: boolean
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
}

export interface PublishTemplatePayload {
  releaseVersion: string
}

export type PasteCleaningCategory = 'TRANSFORMED' | 'REMOVED' | 'WARNING' | 'BLOCKED'

export interface PasteCleaningSummaryItem {
  category: PasteCleaningCategory
  messageKey: string
  detectionSummary: string
}

export interface PasteCleaningSummary {
  items: PasteCleaningSummaryItem[]
  transformedCount: number
  removedCount: number
  warningCount: number
  blockedCount: number
}

export interface PasteCleanResult {
  blocked: boolean
  cleanedStructuredContentJson: string | null
  summary: PasteCleaningSummary
  prePasteSnapshotJson: string
}

export interface MasterStyleCatalogEntry {
  styleKey: string
  applicableNodeTypes: string[]
  renderPurpose: string
}

export interface MasterStyleCatalog {
  catalogVersion: string
  entries: MasterStyleCatalogEntry[]
}

export interface FidelityWarning {
  code: string
  messageKey: string
  location?: string | null
  artifact?: string | null
  viewed?: boolean
}

export interface PreviewRecord {
  previewId: string
  templateId: string
  templateVersionId: string
  status: PreviewStatus
  outputFormat: string
  artifactStorageKey: string | null
  pdfArtifactStorageKey: string | null
  fidelityWarnings: FidelityWarning[]
  previewComparison: PreviewComparison | null
  testDataSetId: string | null
  createdAt: string
}

export interface PreviewRunSummary {
  previewId: string
  templateVersionId: string
  status: PreviewStatus
  testDataSetId: string | null
  createdAt: string
  createdBy: string
  fidelityWarningCount: number
  comparisonBlockerCount: number
  comparisonWarningCount: number
  docxAvailable: boolean
  pdfAvailable: boolean
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

export interface CoverageDimension {
  dimensionCode: string
  totalCount: number
  exercisedCount: number
  percentage: number
  thresholdPercentage: number
  belowThreshold: boolean
}

export interface CoverageThreshold {
  scopeType: string
  groupCode: string | null
  minRequiredVariablePct: number
  minRequiredSamplePct: number
  minAnchorBindingPct: number
}

export interface CoverageSummary {
  templateId: string
  aggregatePercentage: number
  belowThreshold: boolean
  blockerCodes: string[]
  dimensions: CoverageDimension[]
  appliedThreshold: CoverageThreshold
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
  batchSyncMaxItems?: number
  batchAsyncMaxItems?: number
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
  changedAreas: string[]
  blocking: boolean
  warnings: string[]
  defaultRouteImpacted: boolean
  currentPolicyVersion: number
  nextPolicyVersion: number
  summaryMessageKey: string
  contractDiffSummary: string | null
  idempotencyImpactSummary: string | null
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

export type TemplateImportConflictPolicy = 'KEEP_TEMPLATE_ID' | 'REJECT_IMPORT'

export interface TemplateExportMetadata {
  templateId: string
  externalId: string
  groupCode: string
  name: string
  description: string | null
  masterId: string
  lifecycleStatus: TemplateLifecycleStatus
  releaseVersion: string | null
  devVersionId: string
  devVersionNumber: number
  exportedAt: string
}

export interface TemplateContentModuleReference {
  referenceKey: string
  moduleId: string
  semanticVersion: string
  locked: boolean
}

export interface UpsertContentModuleReferencePayload {
  referenceKey: string
  moduleId: string
  semanticVersion: string
}

export interface TemplateExportBundle {
  format: string
  metadata: TemplateExportMetadata
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[]
  contentModuleReferences: TemplateContentModuleReference[]
  policySnapshot: ApiPolicy | null
}

export interface TemplateExportResult {
  format: string
  bundle: TemplateExportBundle
}

export interface TemplateImportSummary {
  resolvedTemplateId: string
  newDevelopmentVersion: number
  importBatchId: string
}

export interface ImportTemplatePayload {
  masterId: string
  bundle: TemplateExportBundle
  importConflictPolicy?: TemplateImportConflictPolicy
}

export interface TemplateImportResult {
  importSummary: TemplateImportSummary
  template: TemplateDetail
}
