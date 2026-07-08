import type { Schema } from '@/types/openapi'

/**
 * OpenAPI-backed management DTO aliases. Types without a matching schema remain
 * hand-written below with a short comment when not yet in `openapi-v1.yaml`.
 */
export type TemplateLifecycleStatus = Schema<'TemplateLifecycleStatus'> | 'DELETED'

export type LifecycleDecision = 'PASSED' | 'FAILED' | 'APPROVED' | 'REJECTED'

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

export type TemplateVersionLineDetail = Omit<
  Schema<'TemplateVersionLineDetailView'>,
  'lifecycleStatus'
> & {
  lifecycleStatus: TemplateLifecycleStatus
  updatedByDisplayName?: string | null
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
}

export type TemplateDevVersionCreated = Schema<'TemplateDevVersionCreatedView'> & {
  lifecycleStatus?: TemplateLifecycleStatus
}

export type VariableSchema = Schema<'TemplateExportVariableSchemaView'> & {
  computeExpression?: string | null
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
}

/** Not yet modeled in `openapi-v1.yaml` (management binding upsert). */
export interface UpsertBindingPayload {
  anchorId: string
  declaredContentType: string
  structuredContentJson: string
}

/** Not yet modeled in `openapi-v1.yaml` (management template create). */
export interface CreateTemplatePayload {
  externalId: string
  groupCode: string
  name: string
  masterId: string
  description?: string
}

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

/** Not yet modeled in `openapi-v1.yaml` (management template metadata update). */
export interface UpdateTemplateMetadataPayload {
  name?: string
  description?: string | null
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
  | 'BLOCKER_STATUS'

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

export type ChangeDiffDimensionCode =
  | 'CONTENT'
  | 'ANCHORS'
  | 'VARIABLES'
  | 'RULES'
  | 'CONTRACT_SUMMARY'

/** Not yet modeled in `openapi-v1.yaml` (management change diff). */
export interface ChangeDiffModification {
  key: string
  changeType: string
  summary: string
}

/** Not yet modeled in `openapi-v1.yaml` (management change diff). */
export interface ChangeDiffDimension {
  dimension: ChangeDiffDimensionCode
  added: string[]
  removed: string[]
  modified: ChangeDiffModification[]
}

/** Not yet modeled in `openapi-v1.yaml` (management change diff). */
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

/** Not yet modeled in `openapi-v1.yaml` (management preview comparison). */
export interface PreviewComparisonItem {
  locationType: PreviewComparisonLocationType
  locationRef: string
  severity: PreviewComparisonSeverity
  diffCode: string
  summary: string
}

/** Not yet modeled in `openapi-v1.yaml` (management preview comparison). */
export interface PreviewComparison {
  totalDiffCount: number
  blockerCount: number
  warningCount: number
  items: PreviewComparisonItem[]
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
}

export type PasteCleaningCategory = 'TRANSFORMED' | 'REMOVED' | 'WARNING' | 'BLOCKED'

/** Not yet modeled in `openapi-v1.yaml` (management paste clean). */
export interface PasteCleaningSummaryItem {
  category: PasteCleaningCategory
  messageKey: string
  detectionSummary: string
}

/** Not yet modeled in `openapi-v1.yaml` (management paste clean). */
export interface PasteCleaningSummary {
  items: PasteCleaningSummaryItem[]
  transformedCount: number
  removedCount: number
  warningCount: number
  blockedCount: number
}

/** Not yet modeled in `openapi-v1.yaml` (management paste clean). */
export interface PasteCleanResult {
  blocked: boolean
  cleanedStructuredContentJson: string | null
  summary: PasteCleaningSummary
  prePasteSnapshotJson: string
}

/** Not yet modeled in `openapi-v1.yaml` (management master style catalog). */
export interface MasterStyleCatalogEntry {
  styleKey: string
  applicableNodeTypes: string[]
  renderPurpose: string
}

/** Not yet modeled in `openapi-v1.yaml` (management master style catalog). */
export interface MasterStyleCatalog {
  catalogVersion: string
  entries: MasterStyleCatalogEntry[]
}

/** Management preview fidelity warning (shape differs from dynamic API `FidelityWarning`). */
export interface FidelityWarning {
  code: string
  messageKey: string
  location?: string | null
  artifact?: string | null
  viewed?: boolean
}

/** Not yet modeled in `openapi-v1.yaml` (management preview record). */
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

/** Not yet modeled in `openapi-v1.yaml` (management preview run summary). */
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

export type RuleValidationStatus = Schema<'RuleValidationStatus'>

export type CompositionRuleInput = Schema<'TemplateRuleValidationItemRequest'>

export type RuleValidationItem = Schema<'TemplateRuleValidationItemResponse'>

export type RuleValidationSummary = Schema<'TemplateRuleValidationSummary'>

export type RuleValidationResult = Schema<'TemplateRuleValidationResponse'> & {
  summary: RuleValidationSummary
}

export type BindingValidationSummary = Schema<'TemplateBindingValidationSummary'>

/**
 * Management binding validation response shape; OpenAPI `TemplateBindingValidationResponse`
 * uses item `status` while management UI maps to `AnchorBinding.validationStatus`.
 */
export interface BindingValidationResult {
  bindings: AnchorBinding[]
  summary: BindingValidationSummary
}

/** Not yet modeled in `openapi-v1.yaml` (management test generate). */
export interface TestGeneratePayload {
  variables?: Record<string, unknown>
  testDataSetId?: string
}

/** Not yet modeled in `openapi-v1.yaml` (management batch test generate). */
export interface BatchTestGeneratePayload {
  testDataSetIds: string[]
}

/** Not yet modeled in `openapi-v1.yaml` (management batch test sample). */
export interface BatchTestSampleResult {
  testDataSetId: string
  previewId: string
  status: PreviewStatus
  warningCount: number
  blockerCount: number
}

/** Not yet modeled in `openapi-v1.yaml` (management batch test summary). */
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

/** Not yet modeled in `openapi-v1.yaml` (management coverage). */
export interface CoverageDimension {
  dimensionCode: string
  totalCount: number
  exercisedCount: number
  percentage: number
  thresholdPercentage: number
  belowThreshold: boolean
}

/** Not yet modeled in `openapi-v1.yaml` (management coverage). */
export interface CoverageThreshold {
  scopeType: string
  groupCode: string | null
  minRequiredVariablePct: number
  minRequiredSamplePct: number
  minAnchorBindingPct: number
}

/** Not yet modeled in `openapi-v1.yaml` (management coverage). */
export interface CoverageSummary {
  templateId: string
  aggregatePercentage: number
  belowThreshold: boolean
  blockerCodes: string[]
  dimensions: CoverageDimension[]
  appliedThreshold: CoverageThreshold
}

/** Not yet modeled in `openapi-v1.yaml` (management test data set). */
export interface TestDataSet {
  testDataSetId: string
  externalId?: string
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

/** Not yet modeled in `openapi-v1.yaml` (management test data set upsert). */
export interface UpsertTestDataSetPayload {
  name: string
  description?: string
  variables: Record<string, unknown>
  required?: boolean
  scenarioName?: string
  coverageTags?: string[]
}

/** Not yet modeled in `openapi-v1.yaml` (management API policy read model). */
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
  saveGeneratedDocuments: boolean
  invocationRecordRetentionDays: number
  documentRetentionDays: number
  updatedAt: string
}

/** Not yet modeled in `openapi-v1.yaml` (management invocation history). */
export interface ManagementInvocationSummary {
  invocationId: string
  invocationKind: string
  status: string
  requestId: string
  resolvedReleaseVersion: string | null
  routeType: string | null
  createdAt: string
  accessAccountSummary: string
}

/** Query filters for paginated management invocation list. */
export interface ManagementInvocationFilters {
  status?: string
  invocationKind?: string
  requestId?: string
  createdAfter?: string
  createdBefore?: string
  credentialId?: string
}

/** Not yet modeled in `openapi-v1.yaml` (management invocation detail). */
export interface ManagementInvocationAuditLinkHint {
  requestId: string
  auditId: string | null
}

/** Not yet modeled in `openapi-v1.yaml` (management invocation detail). */
export interface ManagementInvocationDetail {
  invocationId: string
  requestId: string
  routeType: string | null
  resolvedReleaseVersion: string | null
  outcome: string | null
  durationMs: number | null
  accessAccountSummary: string
  credentialId: string | null
  batchId: string | null
  parentInvocationId: string | null
  createdAt: string
  documentPresent: boolean
  auditLinkHint: ManagementInvocationAuditLinkHint
}

/** Not yet modeled in `openapi-v1.yaml` (management API policy upsert). */
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

/** Not yet modeled in `openapi-v1.yaml` (management API policy impact preview). */
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

/** Not yet modeled in `openapi-v1.yaml` (management routes summary). */
export interface CallableRouteSummary {
  releaseVersion: string
  explicitVersionUrl: string
}

/** Not yet modeled in `openapi-v1.yaml` (management routes summary). */
export interface RoutesSummary {
  templateId: string
  externalId: string
  defaultPath: string
  defaultRouteReleaseVersion: string
  defaultRouteStatus?: string | null
  explicitPaths: CallableRouteSummary[]
}

export type ApiAccessAlertKind = 'MISSING_AD_GROUP' | 'EXPIRING_CREDENTIAL' | 'NO_CREDENTIALS'

/** Not yet modeled in `openapi-v1.yaml` (cross-package API access alerts). */
export interface ApiAccessAlert {
  alertKind: ApiAccessAlertKind
  templateId: string
  templateName: string
  templateExternalId: string
  groupCode?: string | null
  credentialExternalId?: string | null
  credentialExpiresAt?: string | null
}

/** Not yet modeled in `openapi-v1.yaml` (management API credential). */
export interface ApiCredentialSummary {
  credentialId: string
  externalId: string
  status: string
  createdAt: string
  revokedAt: string | null
}

/** Not yet modeled in `openapi-v1.yaml` (management API credential create). */
export interface ApiCredentialCreated {
  credentialId: string
  externalId: string
  secret: string
  status: string
  createdAt: string
}

/** Not yet modeled in `openapi-v1.yaml` (management template delete). */
export interface DeleteTemplatePayload {
  reason: string
}

export type TemplateImportConflictPolicy = Schema<'TemplateImportConflictPolicy'>

export type TemplateExportMetadata = Schema<'TemplateExportMetadataView'>

export type TemplateContentModuleReference = Schema<'TemplateExportContentModuleReferenceView'>

/** Not yet modeled in `openapi-v1.yaml` (management content module reference upsert). */
export interface UpsertContentModuleReferencePayload {
  referenceKey: string
  moduleId: string
  semanticVersion: string
}

export type TemplateExportBundle = Schema<'TemplateExportBundleView'>

export type TemplateExportResult = Schema<'TemplateExportResult'>

export type TemplateImportSummary = Schema<'TemplateImportSummaryView'>

export type ImportTemplatePayload = Schema<'ImportTemplateRequest'>

export type TemplateImportResult = Omit<Schema<'TemplateImportResult'>, 'template'> & {
  template: TemplateDetail
}

/** Not yet modeled in `openapi-v1.yaml` (management async preview stream). */
export interface AsyncPreviewStarted {
  previewId: string
  streamUrl: string
}

/** Not yet modeled in `openapi-v1.yaml` (management batch test stream). */
export interface BatchTestStarted {
  runId: string
  streamUrl: string
}

/** Not yet modeled in `openapi-v1.yaml` (management submit-for-test eligibility). */
export interface SubmitTestEligibility {
  eligible: boolean
  hasValidTestResult: boolean
  allSamplesSucceeded: boolean
  coverageGatePassed: boolean
  failedDataSetNames: string[]
  uncoveredAnchors: string[]
  uncoveredVariables: string[]
}

export type BatchTestRunStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'INVALIDATED'

/** Not yet modeled in `openapi-v1.yaml` (management batch test run history). */
export interface BatchTestRunSummary {
  runId: string
  createdAt: string
  createdBy: string
  createdByDisplayName?: string | null
  status: BatchTestRunStatus
  totalSamples: number
  succeededCount: number
  failedCount: number
  anchorCoveragePct: number | null
  variableCoveragePct: number | null
  sampleCoveragePct: number | null
  gatePassed: boolean | null
  invalidatedAt: string | null
}
