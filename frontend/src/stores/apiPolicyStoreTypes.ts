import type {
  ApiAccessAlert,
  ApiCredentialCreated,
  ApiCredentialSummary,
  ApiPolicy,
} from '@/types/template'

export interface ApiPolicyRotatedCredential {
  credentialId: string
  externalId: string
  secret: string
}

export interface ApiPolicyEntry {
  policy: ApiPolicy | null
  credentials: ApiCredentialSummary[]
  lastCreatedCredential: ApiCredentialCreated | null
  lastRotatedCredential: ApiPolicyRotatedCredential | null
  loadingPolicy: boolean
  submitting: boolean
  lastErrorMessageKey: string | null
}

export function createEmptyApiPolicyEntry(): ApiPolicyEntry {
  return {
    policy: null,
    credentials: [],
    lastCreatedCredential: null,
    lastRotatedCredential: null,
    loadingPolicy: false,
    submitting: false,
    lastErrorMessageKey: null,
  }
}

export type { ApiAccessAlert }
