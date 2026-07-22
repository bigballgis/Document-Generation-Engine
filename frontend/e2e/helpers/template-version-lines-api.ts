import type { APIRequestContext } from '@playwright/test'

import { E2E_CORP_TEMPLATE_AUTHOR, E2E_TEMPLATE_AUTHOR } from './auth'
import { E2E_API_BASE_URL } from './masters-api'

interface ApiEnvelope<T> {
  result: T
  error?: {
    code: string
    messageKey: string
  }
}

interface ApiErrorEnvelope {
  error: {
    code: string
    messageKey: string
  }
}

export interface TemplateVersionLineSummary {
  devVersionId: string
  devVersionNumber: number
  releaseVersion: string | null
  lifecycleStatus: string
  lineKind: 'IN_FLIGHT' | 'PUBLISHED'
  cloneable: boolean
}

interface PageView<T> {
  content: T[]
  totalElements: number
}

async function apiLogin(
  request: APIRequestContext,
  credentials: { username: string; password: string },
): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: credentials,
  })
  if (!response.ok()) {
    throw new Error(`API login failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<{ accessToken: string }>
  return body.result.accessToken
}

async function authorizedGet<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
): Promise<T> {
  const response = await request.get(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    throw new Error(`GET ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

async function authorizedGetWithStatus(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
): Promise<{ status: number; body: ApiErrorEnvelope & ApiEnvelope<unknown> }> {
  const response = await request.get(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const body = (await response.json()) as ApiErrorEnvelope & ApiEnvelope<unknown>
  return { status: response.status(), body }
}

async function authorizedPostWithStatus(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
): Promise<{ status: number; body: ApiErrorEnvelope & ApiEnvelope<unknown> }> {
  const response = await request.post(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const body = (await response.json()) as ApiErrorEnvelope & ApiEnvelope<unknown>
  return { status: response.status(), body }
}

function assertAccessDenied(status: number, body: ApiErrorEnvelope, label: string): void {
  if (status !== 403) {
    throw new Error(`Expected 403 for ${label}, got ${status}: ${JSON.stringify(body)}`)
  }
  if (body.error?.code !== 'ACCESS_DENIED') {
    throw new Error(`Expected ACCESS_DENIED for ${label}, got ${body.error?.code}`)
  }
}

export async function assertCrossGroupVersionLineAccessDenied(
  request: APIRequestContext,
  templateId: string,
  devVersionId: string,
  releaseVersion: string,
): Promise<void> {
  const token = await apiLogin(request, E2E_CORP_TEMPLATE_AUTHOR)

  const list = await authorizedGetWithStatus(
    request,
    token,
    `/templates/${templateId}/version-lines?page=0&size=20`,
  )
  assertAccessDenied(list.status, list.body, 'version-lines list')

  const dev = await authorizedGetWithStatus(
    request,
    token,
    `/templates/${templateId}/dev/${devVersionId}`,
  )
  assertAccessDenied(dev.status, dev.body, 'dev detail')

  const release = await authorizedGetWithStatus(
    request,
    token,
    `/templates/${templateId}/releases/${encodeURIComponent(releaseVersion)}`,
  )
  assertAccessDenied(release.status, release.body, 'release detail')

  const clone = await authorizedPostWithStatus(
    request,
    token,
    `/templates/${templateId}/release-versions/${encodeURIComponent(releaseVersion)}/clone`,
  )
  assertAccessDenied(clone.status, clone.body, 'release clone')
}

export async function listTemplateVersionLines(
  request: APIRequestContext,
  templateId: string,
  token?: string,
): Promise<TemplateVersionLineSummary[]> {
  const accessToken = token ?? (await apiLogin(request, E2E_TEMPLATE_AUTHOR))
  const page = await authorizedGet<PageView<TemplateVersionLineSummary>>(
    request,
    accessToken,
    `/templates/${templateId}/version-lines?page=0&size=20`,
  )
  return page.content
}

/** Resolve the IN_FLIGHT line's `devVersionId` for a template (throws if missing). */
export async function resolveInFlightDevVersionId(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight?.devVersionId) {
    throw new Error(`IN_FLIGHT devVersion for ${templateId} not found`)
  }
  return inFlight.devVersionId
}

export async function cloneReleaseVersion(
  request: APIRequestContext,
  templateId: string,
  releaseVersion: string,
  expectedStatus = 201,
): Promise<{ devVersionId: string; devVersionNumber: number } | ApiErrorEnvelope> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const response = await request.post(
    `${E2E_API_BASE_URL}/templates/${templateId}/release-versions/${encodeURIComponent(releaseVersion)}/clone`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  const body = (await response.json()) as ApiEnvelope<{ devVersionId: string; devVersionNumber: number }> &
    ApiErrorEnvelope
  if (response.status() !== expectedStatus) {
    throw new Error(
      `POST clone failed (expected ${expectedStatus}, got ${response.status()}): ${JSON.stringify(body)}`,
    )
  }
  if (expectedStatus === 201) {
    return body.result
  }
  return body as ApiErrorEnvelope
}

export async function assertCloneBlockedWhenInFlight(
  request: APIRequestContext,
  templateId: string,
  releaseVersion: string,
): Promise<void> {
  const body = (await cloneReleaseVersion(request, templateId, releaseVersion, 409)) as ApiErrorEnvelope
  if (body.error.code !== 'TEMPLATE_DEV_LINE_IN_FLIGHT') {
    throw new Error(`Expected TEMPLATE_DEV_LINE_IN_FLIGHT, got ${body.error.code}`)
  }
}

export async function assertPublishedVersionImmutable(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const response = await request.put(`${E2E_API_BASE_URL}/templates/${templateId}/variables/immutableProbe`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      variableKey: 'immutableProbe',
      variableType: 'TEXT',
      required: false,
      defaultValue: 'blocked',
      description: 'S5 immutability probe',
    },
  })
  const body = (await response.json()) as ApiErrorEnvelope
  if (response.status() !== 403) {
    throw new Error(`Expected 403 immutable, got ${response.status()}: ${JSON.stringify(body)}`)
  }
  if (body.error.code !== 'TEMPLATE_VERSION_IMMUTABLE') {
    throw new Error(`Expected TEMPLATE_VERSION_IMMUTABLE, got ${body.error.code}`)
  }
}
