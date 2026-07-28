export interface CallerContractErrorCode {
  category: string
  code: string
  messageKey: string
  retryable: boolean
  message: string
}

export interface CallerContractVariable {
  variableKey: string
  variableType: string
  required: boolean
  computed?: boolean
  piiCategory?: string
  enumValues?: string[] | null
  description?: string | null
}

export interface CallerContractVersion {
  releaseVersion: string
  explicitVersionUrl: string
  deprecated?: boolean
  sunsetAt?: string | null
  /** FOS-W9-4: always present on GET contract (may be empty). */
  variables?: CallerContractVariable[]
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
    /** OpenAPI CredentialSummary.credentialId (FOS-W9-6). */
    credentialId?: string | null
    /** Legacy wire name — prefer credentialId. */
    credentialExternalId?: string | null
    status: string
    fingerprintSummary?: string | null
    expiresAt?: string | null
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
