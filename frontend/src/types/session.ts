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
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  session: ManagementSession
}
