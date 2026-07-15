import { execFileSync } from 'node:child_process'

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
  }
}

export interface CeU13Fixture {
  templateId: string
  externalId: string
  name: string
  unlockedTestDataSetId: string
  lockedTestDataSetId: string
  oldKey: string
  newKey: string
  otherKey: string
  lonelyKey: string
  customerNameKey: string
  computeKey: string
  autocompleteKeys: { borrowerLegalName: string; showNotice: string }
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

function uniqueExternalId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}`.replace(/[^A-Z0-9_-]/g, '-')
}

export const CE_U13_BINDING_WITH_CUSTOMER_REFS = JSON.stringify({
  schemaVersion: '1.0',
  nodes: [
    {
      type: 'conditionBlock',
      conditionExpression: '${customer} == true',
      children: [
        {
          type: 'paragraph',
          children: [{ type: 'textRun', value: 'Greeting' }],
        },
      ],
    },
    {
      type: 'paragraph',
      children: [
        { type: 'variable', key: 'customer' },
        { type: 'variable', key: 'customerName' },
      ],
    },
  ],
})

function lockTestDataSetViaPostgres(testDataSetExternalId: string): void {
  const sql = `UPDATE template_test_data_set SET locked = true WHERE external_id = '${testDataSetExternalId.replace(/'/g, "''")}';`
  execFileSync(
    'docker',
    [
      'exec',
      '-e',
      'PGPASSWORD=docgen_local_pwd',
      'docgen-postgres',
      'psql',
      '-U',
      'docgen',
      '-d',
      'docgen',
      '-v',
      'ON_ERROR_STOP=1',
      '-c',
      sql,
    ],
    { encoding: 'utf8' },
  )
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
    description?: string | null
    computeExpression?: string | null
  },
): Promise<void> {
  await authorizedPut(request, token, `/templates/${templateId}/variables/${payload.variableKey}`, {
    variableKey: payload.variableKey,
    variableType: payload.variableType,
    required: payload.required ?? false,
    defaultValue: payload.defaultValue ?? null,
    enumValues: null,
    description: payload.description ?? null,
    computeExpression: payload.computeExpression ?? null,
  })
}

/** Draft template with bindings, rules, unlocked + locked test sets, and compute refs for CE-U13. */
export async function prepareCeU13CascadeFixture(
  request: APIRequestContext,
): Promise<CeU13Fixture> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await ensureDemoRetailMasterApproved(request)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found after ensure`)
  }

  const externalId = uniqueExternalId('E2E-CE-U13')
  const name = `E2E CE-U13 variable rename ${externalId}`
  const created = await authorizedPost<{ id: string; externalId: string; lifecycleStatus: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'CE-U13 variable rename cascade Playwright fixture',
      masterId: master.id,
    },
    201,
  )
  if (created.lifecycleStatus !== 'DRAFT') {
    throw new Error(`Expected DRAFT template, got ${created.lifecycleStatus}`)
  }

  const templateId = created.id
  const oldKey = 'customer'
  const newKey = 'party'
  const otherKey = 'otherKey'
  const lonelyKey = 'lonelyKey'
  const customerNameKey = 'customerName'
  const computeKey = 'computedLabel'
  const borrowerLegalName = 'borrowerLegalName'
  const showNotice = 'showNotice'

  await upsertVariable(request, authorToken, templateId, {
    variableKey: oldKey,
    variableType: 'TEXT',
    // Keep optional so Docker images without the cascade delete-before-test-set
    // reorder still accept unlocked-set updates during rename (schema still has oldKey).
    required: false,
    defaultValue: 'Acme',
    description: 'Customer (rename target)',
  })
  await upsertVariable(request, authorToken, templateId, {
    variableKey: customerNameKey,
    variableType: 'TEXT',
    required: false,
    defaultValue: 'Keep',
    description: 'Must not be substring-renamed',
  })
  await upsertVariable(request, authorToken, templateId, {
    variableKey: otherKey,
    variableType: 'TEXT',
    required: false,
    description: 'Conflict target',
  })
  await upsertVariable(request, authorToken, templateId, {
    variableKey: lonelyKey,
    variableType: 'TEXT',
    required: false,
    description: 'Zero-ref rename target',
  })
  await upsertVariable(request, authorToken, templateId, {
    variableKey: borrowerLegalName,
    variableType: 'TEXT',
    required: false,
    description: 'Autocomplete candidate',
  })
  await upsertVariable(request, authorToken, templateId, {
    variableKey: showNotice,
    variableType: 'BOOLEAN',
    required: false,
    description: 'Visibility autocomplete candidate',
  })
  await upsertVariable(request, authorToken, templateId, {
    variableKey: computeKey,
    variableType: 'COMPUTED',
    required: false,
    computeExpression: 'COALESCE(${customer}, "")',
    description: 'Compute ref to customer',
  })

  await authorizedPut(request, authorToken, `/templates/${templateId}/bindings/HEADER`, {
    anchorId: 'HEADER',
    declaredContentType: 'TEXT',
    structuredContentJson: CE_U13_BINDING_WITH_CUSTOMER_REFS,
  })

  await authorizedPut(request, authorToken, `/templates/${templateId}/rules`, {
    rules: [
      {
        ruleId: 'E2E-U13-RULE-CUSTOMER',
        conditionExpression: '${customer} == true',
        targetAnchorId: 'HEADER',
      },
    ],
  })

  const unlocked = await authorizedPost<{ testDataSetId: string }>(
    request,
    authorToken,
    `/templates/${templateId}/test-data-sets`,
    {
      name: `E2E CE-U13 unlocked ${Date.now()}`,
      required: false,
      variables: { customer: 'UnlockedValue', customerName: 'KeepName' },
    },
    201,
  )

  const locked = await authorizedPost<{ testDataSetId: string }>(
    request,
    authorToken,
    `/templates/${templateId}/test-data-sets`,
    {
      name: `E2E CE-U13 locked ${Date.now()}`,
      required: false,
      variables: { customer: 'LockedValue', customerName: 'KeepName' },
    },
    201,
  )
  lockTestDataSetViaPostgres(locked.testDataSetId)

  const lines = await listTemplateVersionLines(request, templateId)
  if (!lines.some((line) => line.lineKind === 'IN_FLIGHT')) {
    throw new Error(`Expected IN_FLIGHT version line for ${templateId}`)
  }

  return {
    templateId,
    externalId,
    name,
    unlockedTestDataSetId: unlocked.testDataSetId,
    lockedTestDataSetId: locked.testDataSetId,
    oldKey,
    newKey,
    otherKey,
    lonelyKey,
    customerNameKey,
    computeKey,
    autocompleteKeys: { borrowerLegalName, showNotice },
  }
}

export async function fetchTemplateDetailViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<{
  variables: Array<{ variableKey: string; computeExpression?: string | null }>
  bindings: Array<{ anchorId: string; structuredContentJson?: string | null }>
  rules: Array<{ ruleId: string; conditionExpression: string }>
}> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedGet(request, token, `/templates/${templateId}`)
}

export async function fetchTestDataSetsViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<
  Array<{
    testDataSetId: string
    name: string
    locked: boolean
    variables: Record<string, unknown>
    required?: boolean
    description?: string | null
    scenarioName?: string | null
    coverageTags?: string[]
  }>
> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedGet(request, token, `/templates/${templateId}/test-data-sets`)
}

/** Fail-closed write probe for sessions without authorTemplates. */
export async function attemptVariableWriteAsTester(
  request: APIRequestContext,
  templateId: string,
  variableKey: string,
): Promise<{ status: number; body: ApiEnvelope<unknown> }> {
  const token = await apiLogin(request, E2E_TEMPLATE_TESTER)
  const response = await request.put(
    `${E2E_API_BASE_URL}/templates/${templateId}/variables/${encodeURIComponent(variableKey)}`,
    {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        variableKey: `${variableKey}Renamed`,
        variableType: 'TEXT',
        required: false,
        defaultValue: null,
        enumValues: null,
        description: 'Should be denied',
        computeExpression: null,
      },
    },
  )
  const body = (await response.json()) as ApiEnvelope<unknown>
  return { status: response.status(), body }
}
