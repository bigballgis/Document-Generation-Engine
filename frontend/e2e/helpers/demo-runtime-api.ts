import type { APIRequestContext } from '@playwright/test'

import { DEMO_RUNTIME_MIN_DOCX_BYTES } from '@/utils/demoRuntimeRegistry'

import annualReviewDemoVariables from '../fixtures/demo/annual-review-demo-test-variables.json' with { type: 'json' }
import commitmentDemoVariables from '../fixtures/demo/commitment-demo-test-variables.json' with { type: 'json' }
import covenantWaiverDemoVariables from '../fixtures/demo/covenant-waiver-demo-test-variables.json' with { type: 'json' }
import creditLimitDemoVariables from '../fixtures/demo/credit-limit-demo-test-variables.json' with { type: 'json' }
import facilityAmendmentDemoVariables from '../fixtures/demo/facility-amendment-demo-test-variables.json' with { type: 'json' }
import formalDemandDemoVariables from '../fixtures/demo/formal-demand-demo-test-variables.json' with { type: 'json' }
import folDemoTestVariables from '../fixtures/fol-demo-test-variables.json' with { type: 'json' }
import { E2E_GROUP_ADMIN, E2E_TEMPLATE_AUTHOR, FOL_TEMPLATE_EXTERNAL_ID } from './auth'
import {
  E2E_CATALOG_PAGE_SIZE,
  buildCatalogQuery,
  findInCatalogPages,
  type CatalogPageView,
} from './catalog-query'
import {
  createTemplateApiCredential,
  RUNTIME_API_BASE_URL,
  type RuntimeCredentialBundle,
} from './content-modules-api'
import { E2E_API_BASE_URL } from './masters-api'

export { assertDocxArtifact, type AssertDocxArtifactOptions } from '@/utils/demoRuntimeArtifact'
export { DEMO_PUBLISH_EXTERNAL_IDS, DEMO_RUNTIME_MIN_DOCX_BYTES } from '@/utils/demoRuntimeRegistry'

interface ApiEnvelope<T> {
  result: T
}

interface TemplateSummary {
  id: string
  externalId: string
  lifecycleStatus: string
  groupCode: string
  releaseVersion?: string | null
}

interface TemplateApiPolicy {
  defaultRouteReleaseVersion: string
  allowedAdGroups: string[]
}

interface TestDataSetFixture {
  id: string
  variables: Record<string, unknown>
}

interface DemoVariablesFixture {
  variables?: Record<string, unknown>
  testDataSets?: TestDataSetFixture[]
}

export interface DemoRuntimeCase {
  externalId: string
  minDocxBytes: number
  contentMarkers: string[]
  loadVariables: () => Record<string, unknown>
}

export type EnsurePublishedDemoResult =
  | {
      ok: true
      templateId: string
      credential: RuntimeCredentialBundle
      lifecycleStatus: string
    }
  | { ok: false; reason: string }

function variablesFromFixture(fixture: DemoVariablesFixture): Record<string, unknown> {
  if (fixture.variables) {
    return fixture.variables
  }
  throw new Error('Fixture is missing a top-level variables object')
}

function variablesFromTestDataSet(fixture: DemoVariablesFixture, dataSetId: string): Record<string, unknown> {
  const dataSet = fixture.testDataSets?.find((entry) => entry.id === dataSetId)
  if (!dataSet) {
    throw new Error(`testDataSet "${dataSetId}" was not found in fixture`)
  }
  return dataSet.variables
}

/** Keep-set of 8 bank-letter Live templates (TM #164 / BDD-DEMO-KEEP-010). */
export const DEMO_RUNTIME_CASES: DemoRuntimeCase[] = [
  {
    externalId: FOL_TEMPLATE_EXTERNAL_ID,
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES[FOL_TEMPLATE_EXTERNAL_ID],
    contentMarkers: ['Pacific Rim Holdings', 'Meridian Global Banking Corporation', 'Borrower'],
    loadVariables: () => folDemoTestVariables.variables as Record<string, unknown>,
  },
  {
    externalId: 'DEMO-CREDIT-LIMIT-CONFIRM',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-CREDIT-LIMIT-CONFIRM'],
    contentMarkers: ['Northgate Manufacturing', 'CORP-CL-2026', 'Revolving Credit Facility'],
    loadVariables: () => variablesFromFixture(creditLimitDemoVariables),
  },
  {
    externalId: 'DEMO-ANNUAL-REVIEW',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-ANNUAL-REVIEW'],
    contentMarkers: ['Harbour Logistics', 'CORP-FAC-2024-77102', 'Covenant'],
    loadVariables: () => variablesFromTestDataSet(annualReviewDemoVariables, 'annual-review-executive'),
  },
  {
    externalId: 'DEMO-FACILITY-RENEWAL',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-FACILITY-RENEWAL'],
    contentMarkers: ['Harbour Logistics', 'CORP-FAC-2024-77102', 'Renewal Confirmation'],
    loadVariables: () => variablesFromTestDataSet(annualReviewDemoVariables, 'facility-renewal-executive'),
  },
  {
    externalId: 'DEMO-FACILITY-AMENDMENT',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-FACILITY-AMENDMENT'],
    contentMarkers: ['Pacific Rim Holdings', 'AMD-2026-FAC-77102', 'Variation'],
    loadVariables: () => variablesFromFixture(facilityAmendmentDemoVariables),
  },
  {
    externalId: 'DEMO-COMMITMENT-LETTER',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-COMMITMENT-LETTER'],
    contentMarkers: ['Aurora Industrial Partners', 'CORP-CML-2026-44107', 'Conditions Precedent'],
    loadVariables: () => variablesFromFixture(commitmentDemoVariables),
  },
  {
    externalId: 'DEMO-FORMAL-DEMAND',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-FORMAL-DEMAND'],
    contentMarkers: ['Harbour Logistics', 'Sums Demanded', 'CORP-FAC-2024-77102'],
    loadVariables: () => variablesFromFixture(formalDemandDemoVariables),
  },
  {
    externalId: 'DEMO-COVENANT-WAIVER',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-COVENANT-WAIVER'],
    contentMarkers: ['Northgate Manufacturing', 'Specified Covenant Breach', 'Waiver Period'],
    loadVariables: () => variablesFromFixture(covenantWaiverDemoVariables),
  },
]

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

function allowedAdGroupsForGroupCode(groupCode: string): string[] {
  return groupCode === 'CORP' ? ['CORP_API'] : ['RETAIL_API']
}

function runtimeCredentialHeaders(credential: RuntimeCredentialBundle): Record<string, string> {
  return {
    'X-Api-Credential-Id': credential.externalId,
    'X-Api-Credential-Secret': credential.secret,
    'X-Access-Account': 'e2e-runtime-caller',
  }
}

export async function findTemplateByExternalId(
  request: APIRequestContext,
  externalId: string,
): Promise<TemplateSummary | undefined> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return findInCatalogPages<TemplateSummary>(
    (page, size) =>
      authorizedGet<CatalogPageView<TemplateSummary> | TemplateSummary[]>(
        request,
        token,
        `/templates${buildCatalogQuery({ search: externalId, page, size })}`,
      ),
    (template) => template.externalId === externalId,
    { pageSize: E2E_CATALOG_PAGE_SIZE },
  )
}

async function fetchTemplateApiPolicy(
  request: APIRequestContext,
  templateId: string,
): Promise<TemplateApiPolicy | null> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const response = await request.get(`${E2E_API_BASE_URL}/templates/${templateId}/api/policy`, {
    headers: { Authorization: `Bearer ${groupAdminToken}` },
  })
  if (response.status() === 404) {
    return null
  }
  if (!response.ok()) {
    throw new Error(
      `GET /templates/${templateId}/api/policy failed (${response.status()}): ${await response.text()}`,
    )
  }
  const body = (await response.json()) as ApiEnvelope<TemplateApiPolicy>
  return body.result
}

async function ensureTemplateApiPolicy(
  request: APIRequestContext,
  template: TemplateSummary,
): Promise<void> {
  const existing = await fetchTemplateApiPolicy(request, template.id)
  if (existing && existing.allowedAdGroups.length > 0) {
    return
  }

  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const releaseVersion = template.releaseVersion ?? existing?.defaultRouteReleaseVersion ?? '1.0.0'
  await authorizedPut(request, groupAdminToken, `/templates/${template.id}/api/policy`, {
    allowedAdGroups: allowedAdGroupsForGroupCode(template.groupCode),
    defaultRouteReleaseVersion: releaseVersion,
    outputFormats: ['DOCX'],
    outputModes: ['SYNC_STREAM'],
    batchEnabled: false,
    maxBatchSize: 10,
    docxEncryptionEnabled: false,
    pdfEncryptionEnabled: false,
  })
}

export async function ensurePublishedDemoWithCredential(
  request: APIRequestContext,
  externalId: string,
): Promise<EnsurePublishedDemoResult> {
  const template = await findTemplateByExternalId(request, externalId)
  if (!template) {
    return {
      ok: false,
      reason: `Template "${externalId}" was not found. Run deploy/import-all-demos.ps1 and publish demos first.`,
    }
  }
  if (template.lifecycleStatus !== 'PUBLISHED') {
    return {
      ok: false,
      reason: `Template "${externalId}" is ${template.lifecycleStatus}, not PUBLISHED.`,
    }
  }

  await ensureTemplateApiPolicy(request, template)
  const credential = await createTemplateApiCredential(request, template.id)

  return {
    ok: true,
    templateId: template.id,
    credential,
    lifecycleStatus: template.lifecycleStatus,
  }
}

export async function runtimeGenerateDocx(
  request: APIRequestContext,
  externalId: string,
  credential: RuntimeCredentialBundle,
  variables: Record<string, unknown>,
  idempotencyKey: string,
): Promise<{ status: number; body: Buffer; documentId: string | null }> {
  const response = await request.post(
    `${RUNTIME_API_BASE_URL}/templates/${externalId}/default/generate`,
    {
      headers: {
        ...runtimeCredentialHeaders(credential),
        'Content-Type': 'application/json',
      },
      data: {
        output: { format: 'DOCX', mode: 'SYNC_STREAM' },
        variables,
        requestId: `req-${idempotencyKey}`,
        idempotencyKey,
      },
    },
  )

  const documentId =
    response.headers()['documentid'] ??
    response.headers()['documentId'] ??
    response.headers()['Document-Id'] ??
    null

  return {
    status: response.status(),
    body: Buffer.from(await response.body()),
    documentId,
  }
}
