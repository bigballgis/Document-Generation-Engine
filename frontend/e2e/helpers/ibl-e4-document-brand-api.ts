/**
 * SYS-NORM Wave 6 / ADR-0071 — DocumentBrand / LegalEntity management APIs are retired.
 * Historical IBL-E4 create/get helpers are removed; use these probes for fail-closed evidence.
 *
 * BDD: docs/behavior/sys-norm-d1-brands.md (D1-009 / D1-010)
 */
import { expect, type APIRequestContext } from '@playwright/test'

import { E2E_GROUP_ADMIN } from './auth'
import { E2E_API_BASE_URL } from './masters-api'

interface ApiEnvelope<T> {
  result?: T
  error?: {
    code?: string
    messageKey?: string
  }
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
  return body.result!.accessToken
}

function assertRetiredSurfaceResponse(
  status: number,
  body: ApiEnvelope<unknown>,
  expectedCode: string,
  pathSuffix: string,
): void {
  expect([404, 410], `${pathSuffix} must fail-closed`).toContain(status)
  expect(body.error?.code, `${pathSuffix} stable retired code`).toBe(expectedCode)
  expect(body.result, `${pathSuffix} must not return a usable catalog payload`).toBeFalsy()
}

/** GET /document-brands → 404|410 DOCUMENT_BRAND_SURFACE_RETIRED (D1-009). */
export async function expectDocumentBrandListRetired(
  request: APIRequestContext,
): Promise<void> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const response = await request.get(`${E2E_API_BASE_URL}/document-brands`, {
    headers: { Authorization: `Bearer ${token}` },
    params: { groupCode: 'RETAIL', page: '0', size: '20' },
  })
  const body = (await response.json()) as ApiEnvelope<unknown>
  assertRetiredSurfaceResponse(
    response.status(),
    body,
    'DOCUMENT_BRAND_SURFACE_RETIRED',
    'GET /document-brands',
  )
}

/** GET /legal-entities → 404|410 LEGAL_ENTITY_SURFACE_RETIRED (D1-010). */
export async function expectLegalEntityListRetired(
  request: APIRequestContext,
): Promise<void> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const response = await request.get(`${E2E_API_BASE_URL}/legal-entities`, {
    headers: { Authorization: `Bearer ${token}` },
    params: { groupCode: 'RETAIL', page: '0', size: '20' },
  })
  const body = (await response.json()) as ApiEnvelope<unknown>
  assertRetiredSurfaceResponse(
    response.status(),
    body,
    'LEGAL_ENTITY_SURFACE_RETIRED',
    'GET /legal-entities',
  )
}
