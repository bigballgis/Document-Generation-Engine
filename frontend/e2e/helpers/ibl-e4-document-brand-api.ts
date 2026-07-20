/**
 * IBL-E4 (#131) — API fixtures for DocumentBrand / LegalEntity catalog journeys.
 * Codes must match ^[A-Z0-9][A-Z0-9_-]{0,63}$.
 */
import type { APIRequestContext } from '@playwright/test'

import { DEMO_GROUP_CODE, E2E_GROUP_ADMIN, E2E_TEMPLATE_AUTHOR } from './auth'
import { E2E_API_BASE_URL } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

export interface DocumentBrandFixture {
  groupCode: string
  documentBrandCode: string
  displayName: string
  status: 'ACTIVE' | 'INACTIVE'
  logoObjectRef: string
}

export interface LegalEntityFixture {
  groupCode: string
  legalEntityCode: string
  displayName: string
  status: 'ACTIVE' | 'INACTIVE'
  documentBrandCode: string
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

async function authorizedPost<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  data: unknown,
  expectedStatus = 201,
): Promise<T> {
  const response = await request.post(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  })
  if (response.status() !== expectedStatus) {
    throw new Error(`POST ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

async function authorizedPut<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  data: unknown,
): Promise<T> {
  const response = await request.put(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  })
  if (!response.ok()) {
    throw new Error(`PUT ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

async function authorizedGet<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  params?: Record<string, string>,
): Promise<T> {
  const response = await request.get(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    params,
  })
  if (!response.ok()) {
    throw new Error(`GET ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

export function uniqueE4Code(prefix: string): string {
  const stamp = Date.now().toString(36).toUpperCase()
  return `${prefix}-${stamp}`
}

export async function createDocumentBrandViaApi(
  request: APIRequestContext,
  options: {
    documentBrandCode: string
    displayName?: string
    groupCode?: string
    logoObjectRef?: string
    status?: 'ACTIVE' | 'INACTIVE'
  },
): Promise<DocumentBrandFixture> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const groupCode = options.groupCode ?? DEMO_GROUP_CODE
  const created = await authorizedPost<DocumentBrandFixture>(request, token, '/document-brands', {
    groupCode,
    documentBrandCode: options.documentBrandCode,
    displayName: options.displayName ?? `E2E Brand ${options.documentBrandCode}`,
    status: options.status ?? 'ACTIVE',
    logoObjectRef:
      options.logoObjectRef ?? `e2e/document-brands/${options.documentBrandCode}/logo`,
    letterheadLegalName: `E2E Letterhead ${options.documentBrandCode}`,
  })
  return created
}

export async function createLegalEntityViaApi(
  request: APIRequestContext,
  options: {
    legalEntityCode: string
    documentBrandCode: string
    displayName?: string
    groupCode?: string
    status?: 'ACTIVE' | 'INACTIVE'
  },
): Promise<LegalEntityFixture> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const groupCode = options.groupCode ?? DEMO_GROUP_CODE
  return authorizedPost<LegalEntityFixture>(request, token, '/legal-entities', {
    groupCode,
    legalEntityCode: options.legalEntityCode,
    displayName: options.displayName ?? `E2E Entity ${options.legalEntityCode}`,
    status: options.status ?? 'ACTIVE',
    documentBrandCode: options.documentBrandCode,
  })
}

export async function getLegalEntityViaApi(
  request: APIRequestContext,
  legalEntityCode: string,
  groupCode: string = DEMO_GROUP_CODE,
): Promise<LegalEntityFixture> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  return authorizedGet<LegalEntityFixture>(
    request,
    token,
    `/legal-entities/${encodeURIComponent(legalEntityCode)}`,
    { groupCode },
  )
}

export async function updateTemplateDocumentBrandAllowList(
  request: APIRequestContext,
  templateId: string,
  allowedDocumentBrandCodes: string[],
): Promise<{ allowedDocumentBrandCodes?: string[] | null }> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedPut(request, token, `/templates/${templateId}`, {
    allowedDocumentBrandCodes,
  })
}

export async function authorCreateDraftTemplate(
  request: APIRequestContext,
  options: {
    externalId: string
    name: string
    masterId: string
    groupCode?: string
  },
): Promise<{ id: string; externalId: string; groupCode: string }> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedPost(
    request,
    token,
    '/templates',
    {
      externalId: options.externalId,
      groupCode: options.groupCode ?? DEMO_GROUP_CODE,
      name: options.name,
      description: 'IBL-E4 allow-list Playwright fixture',
      masterId: options.masterId,
      locale: 'en-US',
    },
    201,
  )
}
