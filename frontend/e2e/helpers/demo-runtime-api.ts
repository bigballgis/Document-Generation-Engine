import { inflateRawSync } from 'node:zlib'

import AdmZip from 'adm-zip'
import type { APIRequestContext } from '@playwright/test'

import annualReviewDemoVariables from '../fixtures/demo/annual-review-demo-test-variables.json' with { type: 'json' }
import collectionDemoVariables from '../fixtures/demo/collection-demo-test-variables.json' with { type: 'json' }
import creditLimitDemoVariables from '../fixtures/demo/credit-limit-demo-test-variables.json' with { type: 'json' }
import mortgageDemoVariables from '../fixtures/demo/mortgage-demo-test-variables.json' with { type: 'json' }
import retailAccountDemoVariables from '../fixtures/demo/retail-account-demo-test-variables.json' with { type: 'json' }
import tradeLcDemoVariables from '../fixtures/demo/trade-lc-demo-test-variables.json' with { type: 'json' }
import wealthDemoVariables from '../fixtures/demo/wealth-demo-test-variables.json' with { type: 'json' }
import folDemoTestVariables from '../fixtures/fol-demo-test-variables.json' with { type: 'json' }
import { E2E_GROUP_ADMIN, E2E_TEMPLATE_AUTHOR, FOL_TEMPLATE_EXTERNAL_ID } from './auth'
import {
  createTemplateApiCredential,
  RUNTIME_API_BASE_URL,
  type RuntimeCredentialBundle,
} from './content-modules-api'
import { E2E_API_BASE_URL } from './masters-api'

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

export interface AssertDocxArtifactOptions {
  minBytes: number
  forbiddenPatterns?: string[]
  contentMarkers?: string[]
}

/** Short demo letters typically land 4.5–8 KB; FOL stays on a higher bar. */
const DEFAULT_MIN_DOCX_BYTES = 4_096
const FOL_MIN_DOCX_BYTES = 20_480
const DEFAULT_FORBIDDEN_PATTERNS = ['LOREM', '{{', 'PLACEHOLDER'] as const

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
    minDocxBytes: FOL_MIN_DOCX_BYTES,
    contentMarkers: ['Pacific Rim Holdings', 'Meridian Global Banking Corporation', 'Borrower'],
    loadVariables: () => folDemoTestVariables.variables as Record<string, unknown>,
  },
  {
    externalId: 'DEMO-CREDIT-LIMIT-CONFIRM',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Northgate Manufacturing', 'CORP-CL-2026'],
    loadVariables: () => variablesFromFixture(creditLimitDemoVariables),
  },
  {
    externalId: 'DEMO-MORTGAGE-APPROVAL',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Hartley', 'Willow Close'],
    loadVariables: () => variablesFromFixture(mortgageDemoVariables),
  },
  {
    externalId: 'DEMO-TRADE-LC-NOTICE',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Shanghai Apex', 'LC-2026-MERI-44821'],
    loadVariables: () => variablesFromTestDataSet(tradeLcDemoVariables, 'trade-lc-executive'),
  },
  {
    externalId: 'DEMO-TRADE-GUARANTEE-NOTICE',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Gulf Infrastructure', 'BG-2026-MERI-99201'],
    loadVariables: () => variablesFromTestDataSet(tradeLcDemoVariables, 'trade-guarantee-executive'),
  },
  {
    externalId: 'DEMO-RATE-CHANGE-NOTICE',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Priya Sharma', '3.95%'],
    loadVariables: () => variablesFromTestDataSet(collectionDemoVariables, 'rate-change-executive'),
  },
  {
    externalId: 'DEMO-OVERDUE-COLLECTION',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Daniel Reeves', '1,247.50'],
    loadVariables: () => variablesFromTestDataSet(collectionDemoVariables, 'overdue-collection-executive'),
  },
  {
    externalId: 'DEMO-ANNUAL-REVIEW',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Harbour Logistics', 'CORP-FAC-2024-77102'],
    loadVariables: () => variablesFromTestDataSet(annualReviewDemoVariables, 'annual-review-executive'),
  },
  {
    externalId: 'DEMO-FACILITY-RENEWAL',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Harbour Logistics', 'CORP-FAC-2024-77102', 'Meridian Global Banking'],
    loadVariables: () => variablesFromTestDataSet(annualReviewDemoVariables, 'facility-renewal-executive'),
  },
  {
    externalId: 'DEMO-WEALTH-STATEMENT',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Ashford Family Trust', 'PWM-UK-2026'],
    loadVariables: () => variablesFromFixture(wealthDemoVariables),
  },
  {
    externalId: 'DEMO-RETAIL-ACCOUNT-OPEN',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['Eleanor Whitfield', 'Manchester Deansgate'],
    loadVariables: () => variablesFromTestDataSet(retailAccountDemoVariables, 'retail-open-executive'),
  },
  {
    externalId: 'DEMO-RETAIL-ACCOUNT-BALANCE',
    minDocxBytes: DEFAULT_MIN_DOCX_BYTES,
    contentMarkers: ['James Porter', '24,567.89'],
    loadVariables: () => variablesFromTestDataSet(retailAccountDemoVariables, 'retail-balance-executive'),
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

interface TemplateListPage {
  content: TemplateSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

async function listTemplates(request: APIRequestContext, token: string): Promise<TemplateSummary[]> {
  const page = await authorizedGet<TemplateListPage>(request, token, '/templates?size=200')
  if (Array.isArray(page)) {
    return page
  }
  return page.content ?? []
}

export async function findTemplateByExternalId(
  request: APIRequestContext,
  externalId: string,
): Promise<TemplateSummary | undefined> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const templates = await listTemplates(request, token)
  return templates.find((template) => template.externalId === externalId)
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

function readUInt16LE(buffer: Buffer, offset: number): number {
  return buffer.readUInt16LE(offset)
}

function readUInt32LE(buffer: Buffer, offset: number): number {
  return buffer.readUInt32LE(offset)
}

function extractZipEntry(buffer: Buffer, entryName: string): Buffer | null {
  const target = entryName.replace(/\\/g, '/')
  let offset = 0

  while (offset + 30 <= buffer.length) {
    const signature = readUInt32LE(buffer, offset)
    if (signature !== 0x04034b50) {
      break
    }

    const compressionMethod = readUInt16LE(buffer, offset + 8)
    const compressedSize = readUInt32LE(buffer, offset + 18)
    const fileNameLength = readUInt16LE(buffer, offset + 26)
    const extraFieldLength = readUInt16LE(buffer, offset + 28)
    const nameStart = offset + 30
    const nameEnd = nameStart + fileNameLength
    const fileName = buffer.toString('utf8', nameStart, nameEnd)
    const dataStart = nameEnd + extraFieldLength
    const dataEnd = dataStart + compressedSize

    if (dataEnd > buffer.length) {
      break
    }

    if (fileName === target) {
      const compressed = buffer.subarray(dataStart, dataEnd)
      if (compressionMethod === 0) {
        return Buffer.from(compressed)
      }
      if (compressionMethod === 8) {
        return Buffer.from(inflateRawSync(compressed))
      }
      throw new Error(`Unsupported ZIP compression method ${compressionMethod} for ${entryName}`)
    }

    offset = dataEnd
  }

  return null
}

function extractDocumentXmlText(docx: Buffer): string {
  try {
    const zip = new AdmZip(docx)
    const entry = zip.getEntry('word/document.xml')
    if (entry) {
      return entry.getData().toString('utf8')
    }
  } catch {
    // Fall through to manual ZIP parsing below.
  }

  const documentXml = extractZipEntry(docx, 'word/document.xml')
  if (documentXml) {
    return documentXml.toString('utf8')
  }
  return docx.toString('latin1')
}

export function assertDocxArtifact(
  body: Buffer,
  options: AssertDocxArtifactOptions,
): void {
  if (body.length < 4 || body[0] !== 0x50 || body[1] !== 0x4b || body[2] !== 0x03 || body[3] !== 0x04) {
    throw new Error('Response body is not a valid DOCX (missing PK\\x03\\x04 magic bytes)')
  }

  if (body.length < options.minBytes) {
    throw new Error(`DOCX too small: ${body.length} bytes (minimum ${options.minBytes})`)
  }

  const documentText = extractDocumentXmlText(body)
  const haystack = `${documentText}\n${body.toString('latin1')}`
  const forbiddenPatterns = options.forbiddenPatterns ?? [...DEFAULT_FORBIDDEN_PATTERNS]

  for (const pattern of forbiddenPatterns) {
    if (haystack.includes(pattern)) {
      throw new Error(`DOCX contains forbidden placeholder marker: ${pattern}`)
    }
  }

  for (const marker of options.contentMarkers ?? []) {
    if (!haystack.includes(marker)) {
      throw new Error(`DOCX is missing expected content marker: ${marker}`)
    }
  }
}
