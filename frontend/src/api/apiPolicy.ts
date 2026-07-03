import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type {
  ApiPolicyDomain,
  ApiPolicyDomainFormMap,
  AdGroupsDomainForm,
  BatchLimitsDomainForm,
  DefaultRouteDomainForm,
  EncryptionDomainForm,
  InvocationRetentionDomainForm,
  OutputPolicyDomainForm,
} from '@/types/apiPolicyDomain'
import type { ApiEnvelope } from '@/types/session'
import type {
  ApiCredentialCreated,
  ApiCredentialSummary,
  ApiPolicyImpactPreview,
  ApiPolicy,
  ManagementInvocationSummary,
  UpsertApiPolicyPayload,
} from '@/types/template'

export interface RotatedCredential {
  credentialId: string
  externalId: string
  secret: string
  rotatedAt: string
}

export async function getApiPolicy(templateId: string): Promise<ApiPolicy> {
  const response = await http.get<ApiEnvelope<ApiPolicy>>(`/templates/${templateId}/api/policy`)
  return unwrapEnvelope(response.data)
}

export async function upsertApiPolicy(
  templateId: string,
  payload: UpsertApiPolicyPayload,
): Promise<ApiPolicy> {
  const response = await http.put<ApiEnvelope<ApiPolicy>>(
    `/templates/${templateId}/api/policy`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function saveAdGroupsDomain(
  templateId: string,
  payload: AdGroupsDomainForm,
  confirmed = true,
): Promise<ApiPolicy> {
  const response = await http.put<ApiEnvelope<ApiPolicy>>(
    `/templates/${templateId}/api/policy/ad-groups`,
    { ...payload, confirmed },
  )
  return unwrapEnvelope(response.data)
}

export async function saveOutputDomain(
  templateId: string,
  payload: OutputPolicyDomainForm,
  confirmed = true,
): Promise<ApiPolicy> {
  const response = await http.put<ApiEnvelope<ApiPolicy>>(
    `/templates/${templateId}/api/policy/output`,
    { ...payload, confirmed },
  )
  return unwrapEnvelope(response.data)
}

export async function saveBatchLimitsDomain(
  templateId: string,
  payload: BatchLimitsDomainForm,
  confirmed = true,
): Promise<ApiPolicy> {
  const response = await http.put<ApiEnvelope<ApiPolicy>>(
    `/templates/${templateId}/api/policy/batch-limits`,
    { ...payload, confirmed },
  )
  return unwrapEnvelope(response.data)
}

export async function saveEncryptionDomain(
  templateId: string,
  payload: EncryptionDomainForm,
  confirmed = true,
): Promise<ApiPolicy> {
  const response = await http.put<ApiEnvelope<ApiPolicy>>(
    `/templates/${templateId}/api/policy/encryption`,
    { ...payload, confirmed },
  )
  return unwrapEnvelope(response.data)
}

export async function saveDefaultRouteDomain(
  templateId: string,
  payload: DefaultRouteDomainForm,
  confirmed = true,
): Promise<ApiPolicy> {
  const response = await http.put<ApiEnvelope<ApiPolicy>>(
    `/templates/${templateId}/api/policy/default-route`,
    { ...payload, confirmed },
  )
  return unwrapEnvelope(response.data)
}

export async function saveInvocationRetentionDomain(
  templateId: string,
  payload: InvocationRetentionDomainForm,
  confirmed = true,
): Promise<ApiPolicy> {
  const response = await http.put<ApiEnvelope<ApiPolicy>>(
    `/templates/${templateId}/api/policy/invocation-retention`,
    { ...payload, confirmed },
  )
  return unwrapEnvelope(response.data)
}

export async function saveApiPolicyDomain<D extends ApiPolicyDomain>(
  templateId: string,
  domain: D,
  payload: ApiPolicyDomainFormMap[D],
  confirmed = true,
): Promise<ApiPolicy> {
  switch (domain) {
    case 'AD_GROUP_AUTHORIZATION':
      return saveAdGroupsDomain(templateId, payload as AdGroupsDomainForm, confirmed)
    case 'OUTPUT_POLICY':
      return saveOutputDomain(templateId, payload as OutputPolicyDomainForm, confirmed)
    case 'BATCH_LIMIT':
      return saveBatchLimitsDomain(templateId, payload as BatchLimitsDomainForm, confirmed)
    case 'ENCRYPTION_CAPABILITY':
      return saveEncryptionDomain(templateId, payload as EncryptionDomainForm, confirmed)
    case 'DEFAULT_ROUTE_TARGET':
      return saveDefaultRouteDomain(templateId, payload as DefaultRouteDomainForm, confirmed)
    default:
      throw new Error(`Unsupported API policy domain: ${domain satisfies never}`)
  }
}

export async function listRecentInvocations(
  templateId: string,
  limit = 10,
): Promise<ManagementInvocationSummary[]> {
  const response = await http.get<ApiEnvelope<ManagementInvocationSummary[]>>(
    `/templates/${templateId}/api/invocations/recent`,
    { params: { limit } },
  )
  return unwrapEnvelope(response.data)
}

export async function fetchApiPolicyImpactPreview(
  templateId: string,
  payload: UpsertApiPolicyPayload,
): Promise<ApiPolicyImpactPreview> {
  const response = await http.post<ApiEnvelope<ApiPolicyImpactPreview>>(
    `/templates/${templateId}/api/policy/impact-preview`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function listCredentials(templateId: string): Promise<ApiCredentialSummary[]> {
  const response = await http.get<ApiEnvelope<ApiCredentialSummary[]>>(
    `/templates/${templateId}/api/credentials`,
  )
  return unwrapEnvelope(response.data)
}

export async function createCredential(templateId: string): Promise<ApiCredentialCreated> {
  const response = await http.post<ApiEnvelope<ApiCredentialCreated>>(
    `/templates/${templateId}/api/credentials`,
  )
  return unwrapEnvelope(response.data)
}

export async function rotateCredential(
  templateId: string,
  credentialId: string,
): Promise<RotatedCredential> {
  const response = await http.post<ApiEnvelope<RotatedCredential>>(
    `/templates/${templateId}/api/credentials/${credentialId}/rotate`,
  )
  return unwrapEnvelope(response.data)
}

export async function revokeCredential(
  templateId: string,
  credentialId: string,
): Promise<ApiCredentialSummary> {
  const response = await http.post<ApiEnvelope<ApiCredentialSummary>>(
    `/templates/${templateId}/api/credentials/${credentialId}/revoke`,
  )
  return unwrapEnvelope(response.data)
}
