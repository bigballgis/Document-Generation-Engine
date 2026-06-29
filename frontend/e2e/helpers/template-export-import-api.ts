import { inflateRawSync } from 'node:zlib'
import type { APIRequestContext } from '@playwright/test'
import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
} from './auth'
import { E2E_API_BASE_URL, findMasterByName } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

export interface PublishedTemplateFixture {
  templateId: string
  externalId: string
  name: string
  groupCode: string
}

export interface TemplateExportApiResult {
  format: string
  bundle: {
    format: string
    metadata: {
      templateId: string
      externalId: string
      groupCode: string
      name: string
      description?: string
      masterId?: string
    }
    variables: unknown[]
    bindings: unknown[]
    rules: unknown[]
    contentModuleReferences: unknown[]
    policySnapshot?: unknown
  }
}

const FORBIDDEN_SECRET_MARKERS = [
  'credentialid',
  'clientsecret',
  'apikey',
  'passwordhash',
  'rawsecret',
] as const

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

function uniqueExternalId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}`.replace(/[^A-Z0-9_-]/g, '-')
}

async function configurePublishableTemplate(request: APIRequestContext, templateId: string): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)

  await authorizedPut(request, authorToken, `/templates/${templateId}/variables/customerName`, {
    variableKey: 'customerName',
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Customer',
    description: 'Customer name',
  })

  await authorizedPut(request, authorToken, `/templates/${templateId}/bindings/HEADER`, {
    anchorId: 'HEADER',
    declaredContentType: 'TEXT',
    structuredContentJson:
      '{"nodes":[{"type":"paragraph","children":[{"type":"variable","key":"customerName"}]}]}',
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/bindings/validate`, {})
}

async function publishTemplateThroughLifecycle(
  request: APIRequestContext,
  templateId: string,
  releaseVersion: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const testerToken = await apiLogin(request, E2E_TEMPLATE_TESTER)
  const approverToken = await apiLogin(request, E2E_TEMPLATE_APPROVER)
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)

  const testDataSet = await authorizedPost<{ testDataSetId: string }>(
    request,
    authorToken,
    `/templates/${templateId}/test-data-sets`,
    {
      name: 'E2E export/import sample',
      required: true,
      variables: { customerName: 'Alice' },
    },
    201,
  )

  await authorizedPost(request, authorToken, `/templates/${templateId}/previews/test-generate`, {
    variables: { customerName: 'Alice' },
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/previews/batch-test`, {
    testDataSetIds: [testDataSet.testDataSetId],
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/lifecycle/submit-test`, {
    commentSummary: 'E2E ready for export/import',
  })

  await authorizedPost(request, testerToken, `/templates/${templateId}/lifecycle/test-decision`, {
    decision: 'PASSED',
    commentSummary: 'E2E test passed',
    fidelityViewedConfirmed: true,
    coverageViewedConfirmed: true,
    previewViewedConfirmed: true,
  })

  await authorizedPost(request, approverToken, `/templates/${templateId}/lifecycle/approval-decision`, {
    decision: 'APPROVED',
    commentSummary: 'E2E approved',
    keyEvidenceConfirmed: true,
  })

  await authorizedPut(request, groupAdminToken, `/templates/${templateId}/api/policy`, {
    allowedAdGroups: ['RETAIL_API'],
    defaultRouteReleaseVersion: releaseVersion,
    outputFormats: ['DOCX'],
    outputModes: ['SYNC_STREAM'],
    batchEnabled: false,
    maxBatchSize: 10,
    docxEncryptionEnabled: false,
    pdfEncryptionEnabled: false,
  })

  await authorizedPost(request, groupAdminToken, `/templates/${templateId}/lifecycle/publish`, {
    releaseVersion,
  })
}

export async function preparePublishedTemplate(
  request: APIRequestContext,
  options?: { externalId?: string; name?: string },
): Promise<PublishedTemplateFixture> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await findMasterByName(request, groupAdminToken, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }

  const externalId = options?.externalId ?? uniqueExternalId('E2E-EXPORT')
  const name = options?.name ?? `E2E Export Template ${externalId}`

  const createdTemplate = await authorizedPost<{ id: string; externalId: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'P14-T03 template export/import Playwright fixture',
      masterId: master.id,
    },
    201,
  )

  await configurePublishableTemplate(request, createdTemplate.id)
  await publishTemplateThroughLifecycle(request, createdTemplate.id, '1.0.0')

  const detail = await authorizedGet<{ lifecycleStatus: string }>(
    request,
    authorToken,
    `/templates/${createdTemplate.id}`,
  )
  if (detail.lifecycleStatus !== 'PUBLISHED') {
    throw new Error(
      `Expected PUBLISHED template for export E2E, got ${detail.lifecycleStatus} (${createdTemplate.id})`,
    )
  }

  return {
    templateId: createdTemplate.id,
    externalId,
    name,
    groupCode: DEMO_GROUP_CODE,
  }
}

export async function exportTemplateJsonViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<TemplateExportApiResult> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  return authorizedGet<TemplateExportApiResult>(request, token, `/templates/${templateId}/export`)
}

export function assertNoSecretsInSerializedBundle(serialized: string): void {
  const lower = serialized.toLowerCase()
  for (const marker of FORBIDDEN_SECRET_MARKERS) {
    if (lower.includes(marker)) {
      throw new Error(`Export bundle must not contain secret marker "${marker}"`)
    }
  }
}

function findCentralDirectoryOffset(buffer: Buffer): number {
  for (let index = buffer.length - 22; index >= 0; index -= 1) {
    if (
      buffer[index] === 0x50 &&
      buffer[index + 1] === 0x4b &&
      buffer[index + 2] === 0x01 &&
      buffer[index + 3] === 0x02
    ) {
      return index
    }
  }
  throw new Error('Downloaded ZIP central directory was not found')
}

export function extractSingleFileZipJson(buffer: Buffer): string {
  const view = new DataView(buffer.buffer, buffer.byteOffset, buffer.byteLength)
  if (view.byteLength < 30 || view.getUint32(0, true) !== 0x04034b50) {
    throw new Error('Downloaded ZIP is not a valid archive')
  }

  const centralDirectoryOffset = findCentralDirectoryOffset(buffer)
  const compressionMethod = view.getUint16(centralDirectoryOffset + 10, true)
  let compressedSize = view.getUint32(centralDirectoryOffset + 20, true)
  const localHeaderOffset = view.getUint32(centralDirectoryOffset + 42, true)
  const localFilenameLength = view.getUint16(localHeaderOffset + 26, true)
  const localExtraLength = view.getUint16(localHeaderOffset + 28, true)
  const dataOffset = localHeaderOffset + 30 + localFilenameLength + localExtraLength

  if (compressedSize === 0) {
    compressedSize = view.getUint32(localHeaderOffset + 18, true)
  }

  if (compressedSize === 0) {
    throw new Error('Downloaded ZIP entry size is missing from archive metadata')
  }

  if (dataOffset + compressedSize > view.byteLength) {
    throw new Error('Downloaded ZIP entry is truncated')
  }

  const compressed = buffer.subarray(dataOffset, dataOffset + compressedSize)
  if (compressionMethod === 0) {
    return compressed.toString('utf8')
  }
  if (compressionMethod === 8) {
    return inflateRawSync(compressed).toString('utf8')
  }
  throw new Error(`Unsupported ZIP compression method ${compressionMethod}`)
}

export function mutateBundleForStagingImport(
  exportResult: TemplateExportApiResult,
): TemplateExportApiResult {
  const stagingExternalId = `${exportResult.bundle.metadata.externalId}-STAGING`
  const stagingTemplateId = crypto.randomUUID()

  return {
    format: exportResult.format,
    bundle: {
      ...exportResult.bundle,
      metadata: {
        ...exportResult.bundle.metadata,
        templateId: stagingTemplateId,
        externalId: stagingExternalId,
        name: `${exportResult.bundle.metadata.name} (staging import)`,
      },
    },
  }
}

export function buildImportJsonFileContent(exportResult: TemplateExportApiResult): string {
  return JSON.stringify(exportResult, null, 2)
}
