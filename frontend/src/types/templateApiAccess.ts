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

/** Management invocation list row (`ManagementInvocationSummaryView`). */
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
  resolvedReleaseVersion?: string
}

/** Audit deep-link hint on invocation detail. */
export interface ManagementInvocationAuditLinkHint {
  requestId: string
  auditId: string | null
}

/** Management invocation detail (`ManagementInvocationDetailView`), including optional error envelope. */
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
  errorCode?: string | null
  errorCategory?: string | null
  errorMessageKey?: string | null
  errorRetryable?: boolean | null
  errorMessage?: string | null
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
interface CallableRouteSummary {
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

/**
 * Lightweight cross-package readiness counts (SCEN-AOD-06).
 * Field names aligned with backend `ApiAccessReadinessSummaryView`.
 */
export interface ApiAccessReadinessSummary {
  publishedInScopeCount: number
  attentionCount: number
  pendingReleaseNeedingSetupCount: number
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
