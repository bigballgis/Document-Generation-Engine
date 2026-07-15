import type { Schema } from '@/types/openapi'
import type { AnchorBinding, PreviewStatus } from '@/types/templateCore'

type ChangeDiffDimensionCode =
  | 'CONTENT'
  | 'ANCHORS'
  | 'VARIABLES'
  | 'RULES'
  | 'CONTRACT_SUMMARY'

export interface ChangeDiffHumanReadableEntry {
  changeType: string
  path: string
  summary: string
}

interface ChangeDiffModification {
  key: string
  changeType: string
  summary: string
}

interface ChangeDiffDimension {
  dimension: ChangeDiffDimensionCode
  added: string[]
  removed: string[]
  modified: ChangeDiffModification[]
}

export interface ChangeDiffSummary {
  templateId: string
  baselineReleaseVersion: string | null
  candidateVersionId: string
  candidateReleaseVersion?: string | null
  hasChanges: boolean
  totalChangeCount: number
  dimensions: ChangeDiffDimension[]
  humanReadableEntries?: ChangeDiffHumanReadableEntry[]
}

type PreviewComparisonLocationType = 'PAGE' | 'ANCHOR' | 'SECTION' | 'COMPONENT'
type PreviewComparisonSeverity = 'WARNING' | 'BLOCKER'

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
interface BatchTestSampleResult {
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
interface CoverageDimension {
  dimensionCode: string
  totalCount: number
  exercisedCount: number
  percentage: number
  thresholdPercentage: number
  belowThreshold: boolean
}

/** Not yet modeled in `openapi-v1.yaml` (management coverage). */
interface CoverageThreshold {
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

/** CE-G03 — required when payload touches PII-marked fields. */
export type TestDataSetPiiHandling = 'SYNTHETIC' | 'EXPLICIT_SENSITIVE'

/** Not yet modeled in `openapi-v1.yaml` (management test data set upsert). */
export interface UpsertTestDataSetPayload {
  name: string
  description?: string
  variables: Record<string, unknown>
  required?: boolean
  scenarioName?: string
  coverageTags?: string[]
  /** CE-G03 — SYNTHETIC or EXPLICIT_SENSITIVE when PII fields have values. */
  piiHandling?: TestDataSetPiiHandling | null
  piiConfirmReason?: string | null
  secondaryConfirmed?: boolean | null
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

type BatchTestRunStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'INVALIDATED'

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
