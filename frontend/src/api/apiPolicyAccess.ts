import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  ApiAccessAlert,
  ApiAccessReadinessSummary,
  ApiCredentialCreated,
  ApiCredentialSummary,
  RoutesSummary,
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

export async function fetchReadinessSummary(): Promise<ApiAccessReadinessSummary> {
  const response = await http.get<ApiEnvelope<ApiAccessReadinessSummary>>('/api-access/summary')
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
