import type { APIRequestContext } from '@playwright/test'
import { DEMO_GROUP_CODE, DEMO_MASTER_NAME, E2E_GROUP_ADMIN, E2E_TEMPLATE_AUTHOR } from './auth'
import { E2E_API_BASE_URL, findMasterByName } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

export interface StructuredAuthoringFixture {
  templateId: string
  externalId: string
  name: string
}

export const CLEAN_STRUCTURED_CONTENT_JSON = JSON.stringify({
  schemaVersion: '1.0',
  nodes: [
    {
      type: 'paragraph',
      children: [{ type: 'textRun', value: 'Clean binding content' }],
    },
  ],
})

export const IMAGE_SCALING_STRUCTURED_CONTENT_JSON = JSON.stringify({
  schemaVersion: '1.0',
  nodes: [{ type: 'imageRef', imageRef: 'IMG-1', applyScaling: true }],
})

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

function uniqueExternalId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}`.replace(/[^A-Z0-9_-]/g, '-')
}

async function createDraftTemplate(
  request: APIRequestContext,
  options?: { externalId?: string; name?: string },
): Promise<StructuredAuthoringFixture> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const master = await findMasterByName(request, groupAdminToken, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }

  const externalId = options?.externalId ?? uniqueExternalId('E2E-P18-T10')
  const name = options?.name ?? `E2E P18-T10 Structured Authoring ${externalId}`

  const created = await authorizedPost<{ id: string; externalId: string; lifecycleStatus: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'P18-T10 controlled structured authoring Playwright fixture',
      masterId: master.id,
    },
    201,
  )

  if (created.lifecycleStatus !== 'DRAFT') {
    throw new Error(`Expected DRAFT template, got ${created.lifecycleStatus}`)
  }

  return {
    templateId: created.id,
    externalId,
    name,
  }
}

async function configureCustomerVariable(request: APIRequestContext, templateId: string): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  await authorizedPut(request, authorToken, `/templates/${templateId}/variables/customerName`, {
    variableKey: 'customerName',
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Customer',
    description: 'Customer name',
  })
}

export async function upsertBindingViaApi(
  request: APIRequestContext,
  templateId: string,
  anchorId: string,
  structuredContentJson: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  await authorizedPut(request, authorToken, `/templates/${templateId}/bindings/${anchorId}`, {
    anchorId,
    declaredContentType: 'TEXT',
    structuredContentJson,
  })
}

export async function prepareDraftTemplateWithCleanBinding(
  request: APIRequestContext,
): Promise<StructuredAuthoringFixture> {
  const fixture = await createDraftTemplate(request, {
    name: `E2E P18-T10 Clean ${Date.now()}`,
  })
  await configureCustomerVariable(request, fixture.templateId)
  await upsertBindingViaApi(request, fixture.templateId, 'HEADER', CLEAN_STRUCTURED_CONTENT_JSON)
  await authorizedPost(
    request,
    await apiLogin(request, E2E_TEMPLATE_AUTHOR),
    `/templates/${fixture.templateId}/bindings/validate`,
    {},
  )
  return fixture
}

export async function prepareDraftTemplateWithImageScalingBinding(
  request: APIRequestContext,
): Promise<StructuredAuthoringFixture> {
  const fixture = await createDraftTemplate(request, {
    name: `E2E P18-T10 Image Scaling ${Date.now()}`,
  })
  await configureCustomerVariable(request, fixture.templateId)
  await upsertBindingViaApi(
    request,
    fixture.templateId,
    'HEADER',
    IMAGE_SCALING_STRUCTURED_CONTENT_JSON,
  )
  await authorizedPost(
    request,
    await apiLogin(request, E2E_TEMPLATE_AUTHOR),
    `/templates/${fixture.templateId}/bindings/validate`,
    {},
  )
  return fixture
}
