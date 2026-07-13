import type { ApiPolicy, UpsertApiPolicyPayload } from '@/types/template'

export const API_POLICY_DOMAINS = [
  'AD_GROUP_AUTHORIZATION',
  'OUTPUT_POLICY',
  'BATCH_LIMIT',
  'ENCRYPTION_CAPABILITY',
  'DEFAULT_ROUTE_TARGET',
] as const

export type ApiPolicyDomain = (typeof API_POLICY_DOMAINS)[number]

export interface AdGroupsDomainForm {
  allowedAdGroups: string[]
}

export interface OutputPolicyDomainForm {
  outputFormats: string[]
  outputModes: string[]
}

export interface BatchLimitsDomainForm {
  batchEnabled: boolean
  syncMaxItems: number
  asyncMaxItems: number
}

export interface EncryptionDomainForm {
  docxEncryptionEnabled: boolean
  pdfEncryptionEnabled: boolean
}

export interface DefaultRouteDomainForm {
  defaultRouteReleaseVersion: string
}

export interface InvocationRetentionDomainForm {
  saveGeneratedDocuments: boolean
  invocationRecordRetentionDays: number
  documentRetentionDays: number
}

export type ApiPolicyDomainFormMap = {
  AD_GROUP_AUTHORIZATION: AdGroupsDomainForm
  OUTPUT_POLICY: OutputPolicyDomainForm
  BATCH_LIMIT: BatchLimitsDomainForm
  ENCRYPTION_CAPABILITY: EncryptionDomainForm
  DEFAULT_ROUTE_TARGET: DefaultRouteDomainForm
  INVOCATION_RETENTION: InvocationRetentionDomainForm
}

export function resolveBatchSyncMaxItems(policy: ApiPolicy): number {
  return policy.batchSyncMaxItems ?? policy.maxBatchSize
}

export function resolveBatchAsyncMaxItems(policy: ApiPolicy): number {
  return policy.batchAsyncMaxItems ?? 10_000
}

function buildUpsertPayloadFromPolicy(policy: ApiPolicy): UpsertApiPolicyPayload {
  return {
    allowedAdGroups: [...policy.allowedAdGroups],
    defaultRouteReleaseVersion: policy.defaultRouteReleaseVersion,
    outputFormats: [...policy.outputFormats],
    outputModes: [...policy.outputModes],
    batchEnabled: policy.batchEnabled,
    maxBatchSize: resolveBatchSyncMaxItems(policy),
    docxEncryptionEnabled: policy.docxEncryptionEnabled,
    pdfEncryptionEnabled: policy.pdfEncryptionEnabled,
  }
}

export function buildUpsertPayloadForDomain<D extends ApiPolicyDomain>(
  policy: ApiPolicy,
  domain: D,
  candidate: ApiPolicyDomainFormMap[D],
): UpsertApiPolicyPayload {
  const base = buildUpsertPayloadFromPolicy(policy)
  switch (domain) {
    case 'AD_GROUP_AUTHORIZATION':
      return { ...base, allowedAdGroups: [...(candidate as AdGroupsDomainForm).allowedAdGroups] }
    case 'OUTPUT_POLICY': {
      const output = candidate as OutputPolicyDomainForm
      return {
        ...base,
        outputFormats: [...output.outputFormats],
        outputModes: [...output.outputModes],
      }
    }
    case 'BATCH_LIMIT': {
      const batch = candidate as BatchLimitsDomainForm
      return {
        ...base,
        batchEnabled: batch.batchEnabled,
        maxBatchSize: batch.syncMaxItems,
      }
    }
    case 'ENCRYPTION_CAPABILITY': {
      const encryption = candidate as EncryptionDomainForm
      return {
        ...base,
        docxEncryptionEnabled: encryption.docxEncryptionEnabled,
        pdfEncryptionEnabled: encryption.pdfEncryptionEnabled,
      }
    }
    case 'DEFAULT_ROUTE_TARGET':
      return {
        ...base,
        defaultRouteReleaseVersion: (candidate as DefaultRouteDomainForm).defaultRouteReleaseVersion,
      }
    default:
      return base
  }
}

export function createDomainFormFromPolicy<D extends ApiPolicyDomain>(
  policy: ApiPolicy,
  domain: D,
): ApiPolicyDomainFormMap[D] {
  switch (domain) {
    case 'AD_GROUP_AUTHORIZATION':
      return { allowedAdGroups: [...policy.allowedAdGroups] } as ApiPolicyDomainFormMap[D]
    case 'OUTPUT_POLICY':
      return {
        outputFormats: [...policy.outputFormats],
        outputModes: [...policy.outputModes],
      } as ApiPolicyDomainFormMap[D]
    case 'BATCH_LIMIT':
      return {
        batchEnabled: policy.batchEnabled,
        syncMaxItems: resolveBatchSyncMaxItems(policy),
        asyncMaxItems: resolveBatchAsyncMaxItems(policy),
      } as ApiPolicyDomainFormMap[D]
    case 'ENCRYPTION_CAPABILITY':
      return {
        docxEncryptionEnabled: policy.docxEncryptionEnabled,
        pdfEncryptionEnabled: policy.pdfEncryptionEnabled,
      } as ApiPolicyDomainFormMap[D]
    case 'DEFAULT_ROUTE_TARGET':
      return {
        defaultRouteReleaseVersion: policy.defaultRouteReleaseVersion,
      } as ApiPolicyDomainFormMap[D]
    default:
      throw new Error(`Unsupported API policy domain: ${domain satisfies never}`)
  }
}

export function createRetentionFormFromPolicy(policy: ApiPolicy): InvocationRetentionDomainForm {
  return {
    saveGeneratedDocuments: policy.saveGeneratedDocuments,
    invocationRecordRetentionDays: policy.invocationRecordRetentionDays,
    documentRetentionDays: policy.documentRetentionDays,
  }
}
