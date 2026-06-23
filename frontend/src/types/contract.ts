export interface CallerContractErrorCode {
  category: string
  code: string
  messageKey: string
  retryable: boolean
  message: string
}

export interface CallerContractVersion {
  releaseVersion: string
  explicitVersionUrl: string
}

export interface CallerContractDefaultRoute {
  url: string
  currentTargetReleaseVersion: string | null
  currentTargetStatus: string
  updatedAt: string
  updatedBy: string
  explicitVersionUrl: string | null
}

export interface CallerContractPolicy {
  policyVersion: number
  updatedAt: string
  updatedBy: string
  allowedOutputFormats: string[]
  allowedOutputModes: string[]
  batchLimits: {
    syncMaxItems: number
    asyncMaxItems: number
  }
  encryptionCapabilities: {
    docxEnabled: boolean
    pdfEnabled: boolean
    permissions: string[]
  }
  adGroupAuthorizationSummary: {
    authorized: boolean
    cacheTtlSeconds: number
    authorizationScopeSummary: string
    effectivePolicyDescription: string
  }
  credentialSummary: {
    credentialExternalId: string
    status: string
    fingerprintSummary: string
  } | null
}

export interface CallerContract {
  templateId: string
  paths: string[]
  defaultRoute: CallerContractDefaultRoute
  apiPolicy: CallerContractPolicy
  callableVersions: CallerContractVersion[]
  schemas: string[]
  errorCodes: CallerContractErrorCode[]
  examples: string[]
}
