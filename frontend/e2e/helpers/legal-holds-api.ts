import type { APIRequestContext } from '@playwright/test'

import { DEMO_GROUP_CODE, E2E_ADMIN, E2E_TEMPLATE_AUTHOR } from './auth'
import { E2E_API_BASE_URL, ensureDemoRetailMasterApproved } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

export type LegalHoldScopeType = 'TEMPLATE_WINDOW' | 'INVOCATION_SET'
export type LegalHoldStatus = 'ACTIVE' | 'RELEASED'

export interface LegalHoldView {
  id: string
  holdExternalId: string
  scopeType: LegalHoldScopeType
  status: LegalHoldStatus
  reason: string | null
  templateId: string | null
  templateExternalId: string | null
  effectiveFrom: string | null
  effectiveTo: string | null
  invocationExternalIds: string[]
  invocationCount: number
  createdAt: string
  createdByUsername: string
  releasedAt: string | null
  releasedByUsername: string | null
}

export interface CreateLegalHoldPayload {
  scopeType: LegalHoldScopeType
  reason?: string | null
  templateId?: string
  templateExternalId?: string
  effectiveFrom?: string
  effectiveTo?: string | null
  invocationExternalIds?: string[]
}

interface PageView<T> {
  content: T[]
  totalElements: number
}

async function apiLogin(
  request: APIRequestContext,
  credentials: { username: string; password: string } = E2E_ADMIN,
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

export async function listLegalHoldsViaApi(
  request: APIRequestContext,
  options: {
    page?: number
    size?: number
    status?: LegalHoldStatus
    credentials?: { username: string; password: string }
  } = {},
): Promise<PageView<LegalHoldView>> {
  const token = await apiLogin(request, options.credentials ?? E2E_ADMIN)
  const params = new URLSearchParams({
    page: String(options.page ?? 0),
    size: String(options.size ?? 50),
  })
  if (options.status) {
    params.set('status', options.status)
  }
  const response = await request.get(`${E2E_API_BASE_URL}/legal-holds?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    throw new Error(`list legal-holds failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<PageView<LegalHoldView>>
  return body.result
}

export async function createLegalHoldViaApi(
  request: APIRequestContext,
  payload: CreateLegalHoldPayload,
  credentials: { username: string; password: string } = E2E_ADMIN,
): Promise<LegalHoldView> {
  const token = await apiLogin(request, credentials)
  const response = await request.post(`${E2E_API_BASE_URL}/legal-holds`, {
    headers: { Authorization: `Bearer ${token}` },
    data: payload,
  })
  if (!response.ok()) {
    throw new Error(`create legal-hold failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<LegalHoldView>
  return body.result
}

export async function releaseLegalHoldViaApi(
  request: APIRequestContext,
  holdId: string,
  credentials: { username: string; password: string } = E2E_ADMIN,
): Promise<LegalHoldView> {
  const token = await apiLogin(request, credentials)
  const response = await request.post(
    `${E2E_API_BASE_URL}/legal-holds/${encodeURIComponent(holdId)}/release`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  if (!response.ok()) {
    throw new Error(`release legal-hold failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<LegalHoldView>
  return body.result
}

export async function findLegalHoldByReason(
  request: APIRequestContext,
  reason: string,
): Promise<LegalHoldView | undefined> {
  const page = await listLegalHoldsViaApi(request, { size: 100 })
  return page.content.find((row) => row.reason === reason)
}

export interface LegalHoldTemplateFixture {
  templateId: string
  externalId: string
  name: string
}

/**
 * Stage-5 stacks may ship without demo catalog. Seed a minimal DRAFT template
 * (E2E- prefix) so TEMPLATE_WINDOW create journeys remain deterministic.
 */
export async function ensureLegalHoldTemplateFixture(
  request: APIRequestContext,
): Promise<LegalHoldTemplateFixture> {
  const master = await ensureDemoRetailMasterApproved(request)
  const authorLogin = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: E2E_TEMPLATE_AUTHOR,
  })
  if (!authorLogin.ok()) {
    throw new Error(`Author login failed (${authorLogin.status()}): ${await authorLogin.text()}`)
  }
  const authorToken = ((await authorLogin.json()) as ApiEnvelope<{ accessToken: string }>).result
    .accessToken

  const stamp = Date.now().toString(36).toUpperCase()
  const externalId = `E2E-LH-TPL-${stamp}`
  const name = `E2E Legal Hold Template ${stamp}`

  const createResponse = await request.post(`${E2E_API_BASE_URL}/templates`, {
    headers: { Authorization: `Bearer ${authorToken}` },
    data: {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'CE-G04 Legal Hold Playwright TEMPLATE_WINDOW fixture',
      masterId: master.id,
      locale: 'en-US',
    },
  })
  if (!createResponse.ok()) {
    throw new Error(
      `create template fixture failed (${createResponse.status()}): ${await createResponse.text()}`,
    )
  }
  const created = ((await createResponse.json()) as ApiEnvelope<{ id: string; externalId: string }>)
    .result

  return {
    templateId: created.id,
    externalId: created.externalId,
    name,
  }
}
