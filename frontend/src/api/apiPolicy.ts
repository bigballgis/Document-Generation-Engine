import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  ApiCredentialCreated,
  ApiCredentialSummary,
  ApiPolicyImpactPreview,
  ApiPolicy,
  UpsertApiPolicyPayload,
} from '@/types/template'

export interface RotatedCredential {
  credentialId: string
  externalId: string
  secret: string
  rotatedAt: string
}

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function getApiPolicy(templateId: string): Promise<ApiPolicy> {
  const response = await http.get<ApiEnvelope<ApiPolicy>>(`/templates/${templateId}/api/policy`)
  return unwrap(response.data)
}

export async function upsertApiPolicy(
  templateId: string,
  payload: UpsertApiPolicyPayload,
): Promise<ApiPolicy> {
  const response = await http.put<ApiEnvelope<ApiPolicy>>(
    `/templates/${templateId}/api/policy`,
    payload,
  )
  return unwrap(response.data)
}

export async function fetchApiPolicyImpactPreview(
  templateId: string,
  payload: UpsertApiPolicyPayload,
): Promise<ApiPolicyImpactPreview> {
  const response = await http.post<ApiEnvelope<ApiPolicyImpactPreview>>(
    `/templates/${templateId}/api/policy/impact-preview`,
    payload,
  )
  return unwrap(response.data)
}

export async function listCredentials(templateId: string): Promise<ApiCredentialSummary[]> {
  const response = await http.get<ApiEnvelope<ApiCredentialSummary[]>>(
    `/templates/${templateId}/api/credentials`,
  )
  return unwrap(response.data)
}

export async function createCredential(templateId: string): Promise<ApiCredentialCreated> {
  const response = await http.post<ApiEnvelope<ApiCredentialCreated>>(
    `/templates/${templateId}/api/credentials`,
  )
  return unwrap(response.data)
}

export async function rotateCredential(
  templateId: string,
  credentialId: string,
): Promise<RotatedCredential> {
  const response = await http.post<ApiEnvelope<RotatedCredential>>(
    `/templates/${templateId}/api/credentials/${credentialId}/rotate`,
  )
  return unwrap(response.data)
}

export async function revokeCredential(
  templateId: string,
  credentialId: string,
): Promise<ApiCredentialSummary> {
  const response = await http.post<ApiEnvelope<ApiCredentialSummary>>(
    `/templates/${templateId}/api/credentials/${credentialId}/revoke`,
  )
  return unwrap(response.data)
}
