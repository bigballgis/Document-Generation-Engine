export interface ApiFieldError {
  field: string
  reason: string
  message: string
}

export interface ApiErrorDetail {
  code: string
  category: string
  message: string
  messageKey: string
  retryable: boolean
  fieldErrors?: ApiFieldError[]
}

export interface ApiMetadata {
  auditId?: string
  traceId?: string
}

export interface ApiEnvelope<T> {
  metadata: ApiMetadata
  result?: T
  error?: ApiErrorDetail
}

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
  /** ISO-8601 UTC instant when the session hits the absolute 8h limit (LR-B6). */
  absoluteSessionExpiresAt?: string
}

/** Shared shape of the login and renew responses (LR-B6 renewal contract). */
export interface LoginResult {
  accessToken: string
  tokenType: string
  session: ManagementSession
  /** ISO-8601 UTC expiry of the issued access token. */
  accessTokenExpiresAt?: string
  /** ISO-8601 UTC absolute session deadline (first login + 8h). */
  sessionAbsoluteDeadline?: string
}
