import type { APIRequestContext } from '@playwright/test'

import { DEMO_GROUP_CODE, DEMO_MASTER_NAME, E2E_TEMPLATE_AUTHOR } from './auth'
import { E2E_API_BASE_URL, ensureDemoRetailMasterApproved } from './masters-api'
import { listTemplateVersionLines } from './template-version-lines-api'

interface ApiEnvelope<T> {
  result: T
  error?: {
    code?: string
    category?: string
    messageKey?: string
    fieldErrors?: Array<{ field: string; reason: string; message?: string }>
  }
}

export interface CeG03PiiFixture {
  templateId: string
  externalId: string
  name: string
  piiKey: string
  nonPiiKey: string
}

async function apiLogin(
  request: APIRequestContext,
  credentials: { username: string; password: string } = E2E_TEMPLATE_AUTHOR,
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
  expectedStatus = 200,
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

async function authorizedPut(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  data: unknown,
): Promise<void> {
  const response = await request.put(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  })
  if (!response.ok()) {
    throw new Error(`PUT ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
}

function uniqueExternalId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}`.replace(/[^A-Z0-9_-]/g, '-')
}

/**
 * Draft template with one PERSONAL_NAME field + one NONE field for CE-G03 UI journeys.
 */
export async function prepareCeG03PiiSchemaFixture(
  request: APIRequestContext,
): Promise<CeG03PiiFixture> {
  const authorToken = await apiLogin(request)
  const master = await ensureDemoRetailMasterApproved(request)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found after ensure`)
  }

  const externalId = uniqueExternalId('E2E-CE-G03')
  const name = `E2E CE-G03 testdata PII ${externalId}`

  const created = await authorizedPost<{ id: string; externalId: string; lifecycleStatus: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'CE-G03 PII governance Playwright fixture',
      masterId: master.id,
    },
    201,
  )

  if (created.lifecycleStatus !== 'DRAFT') {
    throw new Error(`Expected DRAFT template, got ${created.lifecycleStatus}`)
  }

  const piiKey = 'customerName'
  const nonPiiKey = 'amount'

  await authorizedPut(request, authorToken, `/templates/${created.id}/variables/${piiKey}`, {
    variableKey: piiKey,
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Acme',
    description: 'Customer name (PII)',
    piiCategory: 'PERSONAL_NAME',
  })

  await authorizedPut(request, authorToken, `/templates/${created.id}/variables/${nonPiiKey}`, {
    variableKey: nonPiiKey,
    variableType: 'AMOUNT',
    required: false,
    description: 'Amount (non-PII)',
    piiCategory: 'NONE',
  })

  const lines = await listTemplateVersionLines(request, created.id)
  if (!lines.some((line) => line.lineKind === 'IN_FLIGHT')) {
    throw new Error(`Expected IN_FLIGHT version line for ${created.id}`)
  }

  return {
    templateId: created.id,
    externalId,
    name,
    piiKey,
    nonPiiKey,
  }
}

/** API fail-closed: create test data set touching PII without piiHandling (BDD-008 / 011). */
export async function createTestDataSetWithoutPiiHandling(
  request: APIRequestContext,
  templateId: string,
  payload: { name: string; variables: Record<string, unknown> },
): Promise<{ status: number; body: ApiEnvelope<unknown> }> {
  const token = await apiLogin(request)
  const response = await request.post(`${E2E_API_BASE_URL}/templates/${templateId}/test-data-sets`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      name: payload.name,
      required: false,
      variables: payload.variables,
    },
  })
  const body = (await response.json()) as ApiEnvelope<unknown>
  return { status: response.status(), body }
}
