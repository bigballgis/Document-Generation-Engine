import type { APIRequestContext } from '@playwright/test'

import { E2E_GROUP_ADMIN, E2E_TEMPLATE_AUTHOR } from './auth'
import { E2E_API_BASE_URL } from './masters-api'
import {
  RUNTIME_API_BASE_URL,
  type RuntimeCredentialBundle,
} from './content-modules-api'

interface ApiEnvelope<T> {
  result: T
  error?: { code?: string }
}

export interface ManagementInvocationListFilters {
  status?: string
  invocationKind?: string
  requestId?: string
  resolvedReleaseVersion?: string
}

export interface ManagementInvocationSummaryRow {
  invocationId: string
  invocationKind: string
  status: string
  requestId: string
  resolvedReleaseVersion: string | null
  routeType: string | null
  createdAt: string
  accessAccountSummary: string
}

export interface ManagementInvocationDetailRow {
  invocationId: string
  requestId: string
  routeType: string | null
  resolvedReleaseVersion: string | null
  outcome: string | null
  durationMs: number | null
  accessAccountSummary: string
  credentialId: string | null
  batchId: string | null
  parentInvocationId: string | null
  createdAt: string
  documentPresent: boolean
  errorCode?: string | null
  errorCategory?: string | null
  errorMessageKey?: string | null
  errorRetryable?: boolean | null
  errorMessage?: string | null
}

export interface ManagementInvocationPage {
  content: ManagementInvocationSummaryRow[]
  totalElements: number
  page: number
  size: number
}

type LoginCredentials = typeof E2E_GROUP_ADMIN | typeof E2E_TEMPLATE_AUTHOR

async function apiLogin(request: APIRequestContext, credentials: LoginCredentials): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: credentials,
  })
  if (!response.ok()) {
    throw new Error(`API login failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<{ accessToken: string }>
  return body.result.accessToken
}

function runtimeCredentialHeaders(credential: RuntimeCredentialBundle): Record<string, string> {
  return {
    'X-Api-Credential-Id': credential.externalId,
    'X-Api-Credential-Secret': credential.secret,
    'X-Access-Account': 'e2e-runtime-caller',
  }
}

function buildFilterQuery(filters: ManagementInvocationListFilters = {}): string {
  const params = new URLSearchParams()
  if (filters.status?.trim()) {
    params.set('status', filters.status.trim())
  }
  if (filters.invocationKind?.trim()) {
    params.set('invocationKind', filters.invocationKind.trim())
  }
  if (filters.requestId?.trim()) {
    params.set('requestId', filters.requestId.trim())
  }
  if (filters.resolvedReleaseVersion?.trim()) {
    params.set('resolvedReleaseVersion', filters.resolvedReleaseVersion.trim())
  }
  return params.toString()
}

export async function listManagementInvocations(
  request: APIRequestContext,
  templateId: string,
  page = 0,
  size = 50,
  filters: ManagementInvocationListFilters = {},
  credentials: LoginCredentials = E2E_GROUP_ADMIN,
): Promise<ManagementInvocationPage> {
  const token = await apiLogin(request, credentials)
  const filterQuery = buildFilterQuery(filters)
  const response = await request.get(
    `${E2E_API_BASE_URL}/templates/${templateId}/api/invocations?page=${page}&size=${size}${filterQuery ? `&${filterQuery}` : ''}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  if (!response.ok()) {
    throw new Error(
      `GET management invocations failed (${response.status()}): ${await response.text()}`,
    )
  }
  const body = (await response.json()) as ApiEnvelope<ManagementInvocationPage>
  return body.result
}

export async function listManagementInvocationsStatus(
  request: APIRequestContext,
  templateId: string,
  filters: ManagementInvocationListFilters = {},
  credentials: LoginCredentials = E2E_TEMPLATE_AUTHOR,
): Promise<{ status: number; body: unknown }> {
  const token = await apiLogin(request, credentials)
  const filterQuery = buildFilterQuery(filters)
  const response = await request.get(
    `${E2E_API_BASE_URL}/templates/${templateId}/api/invocations?page=0&size=20${filterQuery ? `&${filterQuery}` : ''}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  let body: unknown = null
  try {
    body = await response.json()
  } catch {
    body = await response.text()
  }
  return { status: response.status(), body }
}

export async function getManagementInvocationDetail(
  request: APIRequestContext,
  templateId: string,
  invocationId: string,
  credentials: LoginCredentials = E2E_GROUP_ADMIN,
): Promise<ManagementInvocationDetailRow> {
  const token = await apiLogin(request, credentials)
  const response = await request.get(
    `${E2E_API_BASE_URL}/templates/${templateId}/api/invocations/${invocationId}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  if (!response.ok()) {
    throw new Error(
      `GET management invocation detail failed (${response.status()}): ${await response.text()}`,
    )
  }
  const body = (await response.json()) as ApiEnvelope<ManagementInvocationDetailRow>
  return body.result
}

export async function exportManagementInvocationsCsv(
  request: APIRequestContext,
  templateId: string,
  filters: ManagementInvocationListFilters = {},
  credentials: LoginCredentials = E2E_GROUP_ADMIN,
): Promise<{ status: number; text: string; truncated: boolean }> {
  const token = await apiLogin(request, credentials)
  const filterQuery = buildFilterQuery(filters)
  const response = await request.get(
    `${E2E_API_BASE_URL}/templates/${templateId}/api/invocations/export${filterQuery ? `?${filterQuery}` : ''}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  const truncatedHeader = response.headers()['x-export-truncated']
  return {
    status: response.status(),
    text: await response.text(),
    truncated: String(truncatedHeader ?? '').toLowerCase() === 'true',
  }
}

export async function runtimeGenerateByVersion(
  request: APIRequestContext,
  templateExternalId: string,
  credential: RuntimeCredentialBundle,
  releaseVersion: string,
  idempotencyKey: string,
): Promise<{ status: number; requestId: string }> {
  const requestId = `req-${idempotencyKey}`
  const response = await request.post(
    `${RUNTIME_API_BASE_URL}/templates/${templateExternalId}/versions/${encodeURIComponent(releaseVersion)}/generate`,
    {
      headers: {
        ...runtimeCredentialHeaders(credential),
        'Content-Type': 'application/json',
      },
      data: {
        output: { format: 'DOCX', mode: 'SYNC_STREAM' },
        variables: { customerName: 'Bob' },
        requestId,
        idempotencyKey,
      },
    },
  )
  return { status: response.status(), requestId }
}

/**
 * Triggers REQUEST_BODY_INVALID via blank output.mode (persists failed invocation + error envelope).
 */
export async function runtimeGenerateContractInvalid(
  request: APIRequestContext,
  templateExternalId: string,
  credential: RuntimeCredentialBundle,
  releaseVersion: string | null,
  idempotencyKey: string,
): Promise<{ status: number; requestId: string; bodyText: string }> {
  const requestId = `req-${idempotencyKey}`
  const path =
    releaseVersion == null
      ? `${RUNTIME_API_BASE_URL}/templates/${templateExternalId}/default/generate`
      : `${RUNTIME_API_BASE_URL}/templates/${templateExternalId}/versions/${encodeURIComponent(releaseVersion)}/generate`
  const response = await request.post(path, {
    headers: {
      ...runtimeCredentialHeaders(credential),
      'Content-Type': 'application/json',
    },
    data: {
      output: { format: 'DOCX', mode: '' },
      variables: { customerName: 'Bob' },
      requestId,
      idempotencyKey,
    },
  })
  return { status: response.status(), requestId, bodyText: await response.text() }
}

export async function waitForManagementInvocationByRequestId(
  request: APIRequestContext,
  templateId: string,
  requestId: string,
  timeoutMs = 60_000,
): Promise<ManagementInvocationSummaryRow> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const page = await listManagementInvocations(request, templateId, 0, 50, { requestId })
    const match = page.content.find((row) => row.requestId === requestId)
    if (match) {
      return match
    }
    await new Promise((resolve) => setTimeout(resolve, 1_000))
  }
  throw new Error(`Timed out waiting for management invocation requestId=${requestId}`)
}
