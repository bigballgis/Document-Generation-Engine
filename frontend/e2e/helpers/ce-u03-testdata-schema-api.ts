import type { APIRequestContext } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
} from './auth'
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

export interface CeU03SchemaFixture {
  templateId: string
  externalId: string
  name: string
  /** Controlled enterable keys for S1 / S4 / S8 assertions. */
  enterableKeys: string[]
  computeKey: string
}

export interface CeU03LargeSchemaFixture extends CeU03SchemaFixture {
  largeKeys: string[]
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

async function createDraftTemplate(
  request: APIRequestContext,
  options?: { externalId?: string; name?: string },
): Promise<{ templateId: string; externalId: string; name: string }> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await ensureDemoRetailMasterApproved(request)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found after ensure`)
  }

  const externalId = options?.externalId ?? uniqueExternalId('E2E-CE-U03')
  const name = options?.name ?? `E2E CE-U03 schema form ${externalId}`

  const created = await authorizedPost<{ id: string; externalId: string; lifecycleStatus: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'CE-U03 schema-driven test data form Playwright fixture',
      masterId: master.id,
    },
    201,
  )

  if (created.lifecycleStatus !== 'DRAFT') {
    throw new Error(`Expected DRAFT template, got ${created.lifecycleStatus}`)
  }

  return { templateId: created.id, externalId, name }
}

async function upsertVariable(
  request: APIRequestContext,
  token: string,
  templateId: string,
  payload: {
    variableKey: string
    variableType: string
    required?: boolean
    defaultValue?: string | null
    enumValues?: string | null
    description?: string | null
    computeExpression?: string | null
  },
): Promise<void> {
  await authorizedPut(request, token, `/templates/${templateId}/variables/${payload.variableKey}`, {
    variableKey: payload.variableKey,
    variableType: payload.variableType,
    required: payload.required ?? false,
    defaultValue: payload.defaultValue ?? null,
    enumValues: payload.enumValues ?? null,
    description: payload.description ?? null,
    computeExpression: payload.computeExpression ?? null,
  })
}

/** Compact schema for S1/S2/S3/S4/S5/S7/S8/S9 — TEXT+AMOUNT+ENUM+BOOLEAN+COMPUTED. */
export async function prepareCeU03CompactSchemaFixture(
  request: APIRequestContext,
): Promise<CeU03SchemaFixture> {
  const draft = await createDraftTemplate(request)
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)

  await upsertVariable(request, token, draft.templateId, {
    variableKey: 'customerName',
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Acme',
    description: 'Customer name',
  })
  await upsertVariable(request, token, draft.templateId, {
    variableKey: 'amount',
    variableType: 'AMOUNT',
    required: false,
    description: 'Amount',
  })
  await upsertVariable(request, token, draft.templateId, {
    variableKey: 'status',
    variableType: 'ENUM',
    required: false,
    enumValues: 'ACTIVE,CLOSED',
    description: 'Status',
  })
  await upsertVariable(request, token, draft.templateId, {
    variableKey: 'flag',
    variableType: 'BOOLEAN',
    required: false,
    description: 'Flag',
  })
  await upsertVariable(request, token, draft.templateId, {
    variableKey: 'principalCn',
    variableType: 'COMPUTED',
    required: true,
    computeExpression: '${principal}',
    description: 'Computed Chinese amount (skipped)',
  })

  // Ensure IN_FLIGHT version line exists for Testing tab navigation.
  const lines = await listTemplateVersionLines(request, draft.templateId)
  if (!lines.some((line) => line.lineKind === 'IN_FLIGHT')) {
    throw new Error(`Expected IN_FLIGHT version line for ${draft.templateId}`)
  }

  return {
    ...draft,
    enterableKeys: ['customerName', 'amount', 'status', 'flag'],
    computeKey: 'principalCn',
  }
}

/** ≥12 enterable variables so Advanced JSON defaults expanded (U03-C7 / S6). */
export async function prepareCeU03LargeSchemaFixture(
  request: APIRequestContext,
): Promise<CeU03LargeSchemaFixture> {
  const draft = await createDraftTemplate(request, {
    name: `E2E CE-U03 large schema ${Date.now().toString(36)}`,
  })
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)

  const largeKeys: string[] = []
  for (let i = 1; i <= 12; i += 1) {
    const key = `field${String(i).padStart(2, '0')}`
    largeKeys.push(key)
    await upsertVariable(request, token, draft.templateId, {
      variableKey: key,
      variableType: 'TEXT',
      required: false,
      defaultValue: `v${i}`,
      description: `Large schema field ${i}`,
    })
  }

  return {
    ...draft,
    enterableKeys: largeKeys,
    computeKey: '',
    largeKeys,
  }
}

/** Attempt create as TEMPLATE_TESTER — expect access denial (S10 / S17). */
export async function createTestDataSetAsTester(
  request: APIRequestContext,
  templateId: string,
  payload: { name: string; variables: Record<string, unknown> },
): Promise<{ status: number; body: ApiEnvelope<unknown> }> {
  const token = await apiLogin(request, E2E_TEMPLATE_TESTER)
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
