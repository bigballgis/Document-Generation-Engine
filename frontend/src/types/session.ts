import type { Schema } from '@/types/openapi'

/** Management API field errors may include reasons beyond the dynamic API OpenAPI enum. */
export interface ApiFieldError {
  field: string
  reason: string
  message: string
}

/** Management API errors include codes beyond the dynamic API OpenAPI enum. */
export interface ApiErrorDetail {
  code: string
  category: string
  message: string
  messageKey: string
  retryable: boolean
  fieldErrors?: ApiFieldError[]
}

/** Envelope metadata; management responses may omit dynamic-API-only fields. */
export type ApiMetadata = Partial<Schema<'Metadata'>>

export interface ApiEnvelope<T> {
  metadata: ApiMetadata
  result?: T
  error?: ApiErrorDetail
}

/** Not yet modeled in `openapi-v1.yaml` (management auth session). */
export interface ManagementCapabilities {
  manageMasters: boolean
  reviewMasters: boolean
  authorTemplates: boolean
  decideTests: boolean
  decideApprovals: boolean
  publishTemplates: boolean
  stopTemplates: boolean
  restoreOrDeprecateTemplates: boolean
  deleteTemplates: boolean
  exportTemplates: boolean
  viewCollaborationWorkItems: boolean
  maintainCollaborationTimeoutConfig: boolean
  authorContentModules: boolean
  decideContentModuleReviews: boolean
  manageContentModuleLifecycle: boolean
  manageApiPolicy: boolean
  readAudit: boolean
}

/** Not yet modeled in `openapi-v1.yaml` (management auth session). */
export interface ManagementSession {
  username: string
  displayName: string
  email: string
  authSource: string
  roles: string[]
  authorizedGroupCodes: string[]
  defaultRoute: string
  visibleRoutes: string[]
  capabilities?: ManagementCapabilities
  expiresAt: string
}

/** Not yet modeled in `openapi-v1.yaml` (management auth login). */
export interface LoginResult {
  accessToken: string
  tokenType: string
  session: ManagementSession
}
