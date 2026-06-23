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

export interface ApiEnvelope<T> {
  metadata: {
    auditId?: string
    traceId?: string
  }
  result?: T
  error?: {
    code: string
    message: string
    messageKey: string
  }
}
