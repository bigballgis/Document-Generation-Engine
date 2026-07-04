import type { APIRequestContext } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_TEMPLATE_AUTHOR,
} from './auth'
import { findTemplateByExternalId } from './content-modules-api'
import { E2E_API_BASE_URL, ensureDemoRetailMasterApproved } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

interface TemplateSummary {
  id: string
  externalId: string
  lifecycleStatus: string
}

interface TestDataSetSummary {
  testDataSetId: string
  name: string
}

export const CDP_MVP_GOLDEN_EXTERNAL_ID = 'CDP-MVP-GOLDEN'
export const CDP_MVP_GOLDEN_NAME = 'CDP MVP Golden Path Letter'
export const CDP_MVP_GOLDEN_RELEASE_VERSION = '1.0.0'
export const CDP_MVP_DATASET_NAME = 'CDP-MVP-DATASET-01'

export interface CdpMvpGoldenFixture {
  templateId: string
  externalId: string
  name: string
  releaseVersion: string
  lifecycleStatus: string
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

async function configurePublishableTemplate(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)

  await authorizedPut(request, authorToken, `/templates/${templateId}/variables/customerName`, {
    variableKey: 'customerName',
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Customer',
    description: 'CDP MVP golden path customer name',
  })

  await authorizedPut(request, authorToken, `/templates/${templateId}/bindings/HEADER`, {
    anchorId: 'HEADER',
    declaredContentType: 'TEXT',
    structuredContentJson:
      '{"nodes":[{"type":"paragraph","children":[{"type":"variable","key":"customerName"}]}]}',
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/bindings/validate`, {})
}

async function ensureGoldenTestDataSet(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const dataSets = await authorizedGet<TestDataSetSummary[]>(
    request,
    authorToken,
    `/templates/${templateId}/test-data-sets`,
  )
  const existing = dataSets.find((set) => set.name === CDP_MVP_DATASET_NAME)
  if (existing) {
    return
  }

  await authorizedPost<TestDataSetSummary>(
    request,
    authorToken,
    `/templates/${templateId}/test-data-sets`,
    {
      name: CDP_MVP_DATASET_NAME,
      required: true,
      variables: { customerName: 'CDP Golden Customer' },
    },
    201,
  )
}

async function fetchTemplateLifecycleStatus(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const detail = await authorizedGet<TemplateSummary>(request, authorToken, `/templates/${templateId}`)
  return detail.lifecycleStatus
}

/**
 * CD-E2E-T01b — idempotent DRAFT seed for browser MVP golden path.
 * Uses approved Demo Retail Letterhead (master approval skipped in UI).
 * API setup only; lifecycle transitions happen in the browser spec.
 */
export async function prepareCdpMvpGoldenDraft(
  request: APIRequestContext,
): Promise<CdpMvpGoldenFixture> {
  const existing = await findTemplateByExternalId(request, CDP_MVP_GOLDEN_EXTERNAL_ID)
  if (existing) {
    if (existing.lifecycleStatus === 'DRAFT') {
      await configurePublishableTemplate(request, existing.id)
      await ensureGoldenTestDataSet(request, existing.id)
    }
    return {
      templateId: existing.id,
      externalId: CDP_MVP_GOLDEN_EXTERNAL_ID,
      name: CDP_MVP_GOLDEN_NAME,
      releaseVersion: CDP_MVP_GOLDEN_RELEASE_VERSION,
      lifecycleStatus: existing.lifecycleStatus,
    }
  }

  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await ensureDemoRetailMasterApproved(request)

  const createdTemplate = await authorizedPost<{ id: string; externalId: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId: CDP_MVP_GOLDEN_EXTERNAL_ID,
      groupCode: DEMO_GROUP_CODE,
      name: CDP_MVP_GOLDEN_NAME,
      description: 'CDP browser MVP golden path template (setup seed only)',
      masterId: master.id,
    },
    201,
  )

  await configurePublishableTemplate(request, createdTemplate.id)
  await ensureGoldenTestDataSet(request, createdTemplate.id)

  return {
    templateId: createdTemplate.id,
    externalId: CDP_MVP_GOLDEN_EXTERNAL_ID,
    name: CDP_MVP_GOLDEN_NAME,
    releaseVersion: CDP_MVP_GOLDEN_RELEASE_VERSION,
    lifecycleStatus: await fetchTemplateLifecycleStatus(request, createdTemplate.id),
  }
}
