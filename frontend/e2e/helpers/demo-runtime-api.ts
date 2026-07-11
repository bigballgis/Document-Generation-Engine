import type { APIRequestContext } from '@playwright/test'

import { DEMO_RUNTIME_MIN_DOCX_BYTES } from '@/utils/demoRuntimeRegistry'

import annualReviewDemoVariables from '../fixtures/demo/annual-review-demo-test-variables.json' with { type: 'json' }
import collectionDemoVariables from '../fixtures/demo/collection-demo-test-variables.json' with { type: 'json' }
import creditLimitDemoVariables from '../fixtures/demo/credit-limit-demo-test-variables.json' with { type: 'json' }
import fullFlowDemoVariables from '../fixtures/demo/full-flow-demo-test-variables.json' with { type: 'json' }
import mortgageDemoVariables from '../fixtures/demo/mortgage-demo-test-variables.json' with { type: 'json' }
import retailAccountDemoVariables from '../fixtures/demo/retail-account-demo-test-variables.json' with { type: 'json' }
import tradeLcDemoVariables from '../fixtures/demo/trade-lc-demo-test-variables.json' with { type: 'json' }
import wealthDemoVariables from '../fixtures/demo/wealth-demo-test-variables.json' with { type: 'json' }
import folDemoTestVariables from '../fixtures/fol-demo-test-variables.json' with { type: 'json' }
import { DEMO_FULL_FLOW_EXTERNAL_ID, E2E_GROUP_ADMIN, E2E_TEMPLATE_AUTHOR, FOL_TEMPLATE_EXTERNAL_ID } from './auth'
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

export const DEMO_RUNTIME_CASES: DemoRuntimeCase[] = [
  {
    externalId: FOL_TEMPLATE_EXTERNAL_ID,
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES[FOL_TEMPLATE_EXTERNAL_ID],
    contentMarkers: ['Pacific Rim Holdings', 'Meridian Global Banking Corporation', 'Borrower'],
    loadVariables: () => folDemoTestVariables.variables as Record<string, unknown>,
  },
  {
    externalId: DEMO_FULL_FLOW_EXTERNAL_ID,
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES[DEMO_FULL_FLOW_EXTERNAL_ID],
    contentMarkers: ['Margaret Sinclair'],
    loadVariables: () => variablesFromFixture(fullFlowDemoVariables),
  },
  {
    externalId: 'DEMO-RETAIL-ACCOUNT-OPEN',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-RETAIL-ACCOUNT-OPEN'],
    contentMarkers: ['Eleanor Whitfield', 'Manchester Deansgate', 'Meridian Everyday Current Account'],
    loadVariables: () => variablesFromTestDataSet(retailAccountDemoVariables, 'retail-open-executive'),
  },
  {
    externalId: 'DEMO-RETAIL-ACCOUNT-BALANCE',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-RETAIL-ACCOUNT-BALANCE'],
    contentMarkers: ['James Porter', '24,567.89', 'Meridian Everyday Current Account'],
    loadVariables: () => variablesFromTestDataSet(retailAccountDemoVariables, 'retail-balance-executive'),
  },
  {
    externalId: 'DEMO-MORTGAGE-APPROVAL',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-MORTGAGE-APPROVAL'],
    contentMarkers: ['Oliver Hartley', 'Willow Close', 'Meridian Home Finance'],
    loadVariables: () => variablesFromFixture(mortgageDemoVariables),
  },
  {
    externalId: 'DEMO-CREDIT-LIMIT-CONFIRM',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-CREDIT-LIMIT-CONFIRM'],
    contentMarkers: ['Northgate Manufacturing', 'CORP-CL-2026', 'Revolving Credit Facility'],
    loadVariables: () => variablesFromFixture(creditLimitDemoVariables),
  },
  {
    externalId: 'DEMO-TRADE-LC-NOTICE',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-TRADE-LC-NOTICE'],
    contentMarkers: ['Shanghai Apex', 'LC-2026-MERI-44821', 'UCP 600'],
    loadVariables: () => variablesFromTestDataSet(tradeLcDemoVariables, 'trade-lc-executive'),
  },
  {
    externalId: 'DEMO-TRADE-GUARANTEE-NOTICE',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-TRADE-GUARANTEE-NOTICE'],
    contentMarkers: ['Gulf Infrastructure', 'BG-2026-MERI-99201', 'URDG 758'],
    loadVariables: () => variablesFromTestDataSet(tradeLcDemoVariables, 'trade-guarantee-executive'),
  },
  {
    externalId: 'DEMO-RATE-CHANGE-NOTICE',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-RATE-CHANGE-NOTICE'],
    contentMarkers: ['Priya Sharma', '3.95% AER', 'Meridian Retail Banking'],
    loadVariables: () => variablesFromTestDataSet(collectionDemoVariables, 'rate-change-executive'),
  },
  {
    externalId: 'DEMO-OVERDUE-COLLECTION',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-OVERDUE-COLLECTION'],
    contentMarkers: ['Daniel Reeves', '1,247.50', 'overdue balance'],
    loadVariables: () => variablesFromTestDataSet(collectionDemoVariables, 'overdue-collection-executive'),
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
    externalId: 'DEMO-WEALTH-STATEMENT',
    minDocxBytes: DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-WEALTH-STATEMENT'],
    contentMarkers: ['Ashford Family Trust', 'PWM-UK-2026', 'Meridian Private Wealth'],
    loadVariables: () => variablesFromFixture(wealthDemoVariables),
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