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
import type { PageView } from '@/types/identity'
import type {
  ApiAccessAlert,
  ApiCredentialCreated,
  ApiCredentialSummary,
  ApiPolicyImpactPreview,
  ApiPolicy,
  ManagementInvocationDetail,
  ManagementInvocationFilters,
  ManagementInvocationSummary,
  RoutesSummary,
  UpsertApiPolicyPayload,
} from '@/types/template'

export interface RotatedCredential {
  credentialId: string
  externalId: string
  secret: string
  rotatedAt: string
}

interface RoutesSummaryApiView {
  templateExternalId: string
  defaultRouteReleaseVersion: string | null
  defaultGeneratePath: string
  explicitPaths: Array<{ releaseVersion: string; generatePath: string }>
}

interface ApiAccessAlertApiView {
  alertType: ApiAccessAlert['alertKind']
  templateId: string
  templateName: string
  templateExternalId: string
  groupCode?: string | null
  credentialExternalId?: string | null
  expiresAt?: string | null
}

function mapRoutesSummary(templateId: string, raw: RoutesSummaryApiView): RoutesSummary {
  return {
    templateId,
    externalId: raw.templateExternalId,
    defaultPath: raw.defaultGeneratePath,
    defaultRouteReleaseVersion: raw.defaultRouteReleaseVersion ?? '',
    explicitPaths: (raw.explicitPaths ?? []).map((path) => ({
      releaseVersion: path.releaseVersion,
      explicitVersionUrl: path.generatePath,
    })),
  }
}

function mapApiAccessAlert(raw: ApiAccessAlertApiView): ApiAccessAlert {
  return {
    alertKind: raw.alertType,
    templateId: raw.templateId,
    templateName: raw.templateName,
    templateExternalId: raw.templateExternalId,
    groupCode: raw.groupCode ?? null,
    credentialExternalId: raw.credentialExternalId ?? null,
    credentialExpiresAt: raw.expiresAt ?? null,
  }
}

export async function fetchRoutesSummary(templateId: string): Promise<RoutesSummary> {
  const response = await http.get<ApiEnvelope<RoutesSummaryApiView>>(
    `/templates/${templateId}/api/routes-summary`,
  )
  return mapRoutesSummary(templateId, unwrapEnvelope(response.data))
}

export async function fetchAlerts(): Promise<ApiAccessAlert[]> {
  const response = await http.get<ApiEnvelope<ApiAccessAlertApiView[]>>('/api-access/alerts')
  return unwrapEnvelope(response.data).map(mapApiAccessAlert)
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


function buildInvocationListParams(
  page: number,
  size: number,
  filters: ManagementInvocationFilters = {},
): Record<string, string | number> {
  const params: Record<string, string | number> = { page, size }
  if (filters.status?.trim()) {
    params.status = filters.status.trim()
  }
  if (filters.invocationKind?.trim()) {
    params.invocationKind = filters.invocationKind.trim()
  }
  if (filters.requestId?.trim()) {
    params.requestId = filters.requestId.trim()
  }
  if (filters.createdAfter?.trim()) {
    params.createdAfter = filters.createdAfter.trim()
  }
  if (filters.createdBefore?.trim()) {
    params.createdBefore = filters.createdBefore.trim()
  }
  if (filters.credentialId?.trim()) {
    params.credentialId = filters.credentialId.trim()
  }
  return params
}

export async function listInvocations(
  templateId: string,
  page: number,
  size: number,
  filters: ManagementInvocationFilters = {},
): Promise<PageView<ManagementInvocationSummary>> {
  const response = await http.get<ApiEnvelope<PageView<ManagementInvocationSummary>>>(
    `/templates/${templateId}/api/invocations`,
    { params: buildInvocationListParams(page, size, filters) },
  )
  return unwrapEnvelope(response.data)
}

export async function getInvocationDetail(
  templateId: string,
  invocationId: string,
): Promise<ManagementInvocationDetail> {
  const response = await http.get<ApiEnvelope<ManagementInvocationDetail>>(
    `/templates/${templateId}/api/invocations/${invocationId}`,
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
