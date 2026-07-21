import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { crc32, inflateRawSync } from 'node:zlib'
import AdmZip from 'adm-zip'
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

const E2E_ASSET_PNG_PATH = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
  'fixtures',
  'e2e-asset-1x1.png',
)

const ZIP_BUNDLE_ENTRY = 'template-export-bundle.json'
const ZIP_MASTER_ENTRY = 'artifacts/master.docx'
const ZIP_ASSET_DIR = 'artifacts/assets/'

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

  // IBL-E3: after test pass the template is PENDING_SUBMIT — must submit-for-approval first.
  await authorizedPost(request, authorToken, `/templates/${templateId}/lifecycle/submit-approval`, {
    commentSummary: 'E2E ready for approval decision',
  })

  await authorizedPost(request, approverToken, `/templates/${templateId}/lifecycle/approval-decision`, {
    decision: 'APPROVED',
    commentSummary: 'E2E approved',
    fidelityViewedConfirmed: true,
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
    fidelityViewedConfirmed: true,
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
      locale: 'en-US',
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

/** SYS-NORM Wave 7 — promotion pack ZIP (`dependencyClosure=PROMOTION`). */
export async function exportPromotionZipViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<Buffer> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const pathSuffix =
    `/templates/${templateId}/export?bundleVersion=2&format=zip&dependencyClosure=PROMOTION`
  const response = await request.get(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    throw new Error(`GET ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const contentType = response.headers()['content-type'] ?? ''
  if (!contentType.includes('application/zip') && !contentType.includes('application/octet-stream')) {
    throw new Error(`Expected ZIP content-type for promotion export, got "${contentType}"`)
  }
  return Buffer.from(await response.body())
}

function pathSafeAssetSegment(assetKey: string): string {
  return encodeURIComponent(assetKey.trim()).replace(/\+/g, '%20')
}

function readZipBundleJson(zip: AdmZip): Record<string, unknown> {
  const entry = zip.getEntry(ZIP_BUNDLE_ENTRY)
  if (!entry) {
    throw new Error(`Promotion ZIP missing ${ZIP_BUNDLE_ENTRY}`)
  }
  return JSON.parse(entry.getData().toString('utf8')) as Record<string, unknown>
}

/**
 * Build a ZIP with STORE compression, preserving entry order.
 * AdmZip reorders entries alphabetically on write, which breaks the FE import
 * summary parser (it only reads the first local ZIP entry as JSON).
 */
function buildOrderedStoreZip(entries: Array<{ name: string; data: Buffer }>): Buffer {
  const localParts: Buffer[] = []
  const centralParts: Buffer[] = []
  let offset = 0

  for (const entry of entries) {
    const nameBuf = Buffer.from(entry.name, 'utf8')
    const data = entry.data
    const checksum = crc32(data) >>> 0
    const localHeader = Buffer.alloc(30)
    localHeader.writeUInt32LE(0x04034b50, 0)
    localHeader.writeUInt16LE(20, 4) // version needed
    localHeader.writeUInt16LE(0, 6) // flags
    localHeader.writeUInt16LE(0, 8) // STORE
    localHeader.writeUInt16LE(0, 10) // time
    localHeader.writeUInt16LE(0, 12) // date
    localHeader.writeUInt32LE(checksum, 14)
    localHeader.writeUInt32LE(data.length, 18)
    localHeader.writeUInt32LE(data.length, 22)
    localHeader.writeUInt16LE(nameBuf.length, 26)
    localHeader.writeUInt16LE(0, 28)

    const central = Buffer.alloc(46)
    central.writeUInt32LE(0x02014b50, 0)
    central.writeUInt16LE(20, 4)
    central.writeUInt16LE(20, 6)
    central.writeUInt16LE(0, 8)
    central.writeUInt16LE(0, 10)
    central.writeUInt16LE(0, 12)
    central.writeUInt16LE(0, 14)
    central.writeUInt32LE(checksum, 16)
    central.writeUInt32LE(data.length, 20)
    central.writeUInt32LE(data.length, 24)
    central.writeUInt16LE(nameBuf.length, 28)
    central.writeUInt16LE(0, 30)
    central.writeUInt16LE(0, 32)
    central.writeUInt16LE(0, 34)
    central.writeUInt16LE(0, 36)
    central.writeUInt32LE(0, 38)
    central.writeUInt32LE(offset, 42)

    localParts.push(localHeader, nameBuf, data)
    centralParts.push(central, nameBuf)
    offset += localHeader.length + nameBuf.length + data.length
  }

  const centralDirectory = Buffer.concat(centralParts)
  const end = Buffer.alloc(22)
  end.writeUInt32LE(0x06054b50, 0)
  end.writeUInt16LE(0, 4)
  end.writeUInt16LE(0, 6)
  end.writeUInt16LE(entries.length, 8)
  end.writeUInt16LE(entries.length, 10)
  end.writeUInt32LE(centralDirectory.length, 12)
  end.writeUInt32LE(offset, 16)
  end.writeUInt16LE(0, 20)

  return Buffer.concat([...localParts, centralDirectory, end])
}

function rebuildPromotionZip(
  sourceZip: AdmZip,
  bundleJson: Record<string, unknown>,
  options?: {
    omitMasterDocx?: boolean
    extraAsset?: { assetKey: string; bytes: Buffer }
  },
): Buffer {
  const ordered: Array<{ name: string; data: Buffer }> = [
    // FE summary parser reads the first ZIP entry — keep JSON first.
    { name: ZIP_BUNDLE_ENTRY, data: Buffer.from(JSON.stringify(bundleJson), 'utf8') },
  ]

  for (const entry of sourceZip.getEntries()) {
    if (entry.isDirectory || entry.entryName === ZIP_BUNDLE_ENTRY) {
      continue
    }
    if (options?.omitMasterDocx && entry.entryName === ZIP_MASTER_ENTRY) {
      continue
    }
    ordered.push({ name: entry.entryName, data: entry.getData() })
  }

  if (options?.extraAsset) {
    const entryName = `${ZIP_ASSET_DIR}${pathSafeAssetSegment(options.extraAsset.assetKey)}`
    ordered.push({ name: entryName, data: options.extraAsset.bytes })
  }

  return buildOrderedStoreZip(ordered)
}

export interface StagingPromotionZip {
  zipBytes: Buffer
  externalId: string
  templateId: string
  name: string
  injectedAssetKey?: string
}

/**
 * Rewrites promotion ZIP metadata for a clean REJECT_IMPORT into the same stack,
 * optionally embeds a unique asset binary so dry-run surfaces ASSET_BINARY rows.
 */
export function mutatePromotionZipForStagingImport(
  zipBytes: Buffer,
  options?: { injectSyntheticAsset?: boolean },
): StagingPromotionZip {
  const source = new AdmZip(zipBytes)
  const bundle = readZipBundleJson(source)
  const metadata = (bundle.metadata ?? {}) as Record<string, unknown>
  const baseExternalId = String(metadata.externalId ?? 'E2E-PROMO')
  const stagingExternalId = `${baseExternalId}-STAGING`.replace(/[^A-Z0-9_-]/gi, '-').toUpperCase()
  const stagingTemplateId = crypto.randomUUID()
  const stagingName = `${String(metadata.name ?? 'E2E Promotion')} (staging import)`

  metadata.templateId = stagingTemplateId
  metadata.externalId = stagingExternalId
  metadata.name = stagingName
  bundle.metadata = metadata

  let injectedAssetKey: string | undefined
  let extraAsset: { assetKey: string; bytes: Buffer } | undefined
  if (options?.injectSyntheticAsset !== false) {
    injectedAssetKey = `E2E-PROMO-ASSET-${Date.now().toString(36).toUpperCase()}`
    const manifest = Array.isArray(bundle.assetKeyManifest)
      ? (bundle.assetKeyManifest as Array<Record<string, unknown>>)
      : []
    manifest.push({ referenceKey: injectedAssetKey, usage: 'IMAGE' })
    bundle.assetKeyManifest = manifest
    extraAsset = {
      assetKey: injectedAssetKey,
      bytes: fs.readFileSync(E2E_ASSET_PNG_PATH),
    }
  }

  return {
    zipBytes: rebuildPromotionZip(source, bundle, { extraAsset }),
    externalId: stagingExternalId,
    templateId: stagingTemplateId,
    name: stagingName,
    injectedAssetKey,
  }
}

/** Strip embedded master DOCX so dry-run reports blocking MASTER_DOCX_ABSENT. */
export function stripMasterDocxFromPromotionZip(zipBytes: Buffer): Buffer {
  const source = new AdmZip(zipBytes)
  const bundle = readZipBundleJson(source)
  return rebuildPromotionZip(source, bundle, { omitMasterDocx: true })
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
