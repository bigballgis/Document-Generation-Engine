export interface ManagementSession {
  username: string
  displayName: string
  email: string
  authSource: string
  roles: string[]
  authorizedGroupCodes: string[]
  defaultRoute: string
  visibleRoutes: string[]
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
