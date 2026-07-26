import type { CallerContract } from '@/types/contract'
import type { TestDataSet } from '@/types/templatePreview'

/** Fixed placeholders — never embed real secrets (U12-C3 / FOS-W9-1). */
export const API_CREDENTIAL_ID_PLACEHOLDER = '<API_CREDENTIAL_ID>'
export const API_CREDENTIAL_SECRET_PLACEHOLDER = '<API_CREDENTIAL_SECRET>'
export const ACCESS_ACCOUNT_PLACEHOLDER = '<ACCESS_ACCOUNT>'
/** @deprecated FOS-W9-1 — Bearer is not accepted by runtime; kept for import stability in older tests. */
export const ACCESS_TOKEN_PLACEHOLDER = API_CREDENTIAL_ID_PLACEHOLDER
export const IDEMPOTENCY_KEY_PLACEHOLDER = '<IDEMPOTENCY_KEY>'
const REQUEST_ID_PLACEHOLDER = '<REQUEST_ID>'

export type GenerateExamplePayload = {
  output: { format: string; mode: string }
  variables: Record<string, unknown>
  requestId: string
  idempotencyKey: string
}

export type ContractCopyableExample = {
  exampleToken: string
  generateUrl: string
  payload: GenerateExamplePayload
  payloadJson: string
  curl: string
  hasTestDataSet: boolean
  /** FOS-W9-2: sync curl only when SYNC_STREAM is allowed; else async batch. */
  exampleKind: 'sync' | 'async'
}

function pickDefaultFormat(allowed: string[]): string {
  if (allowed.includes('DOCX')) {
    return 'DOCX'
  }
  return allowed[0] ?? 'DOCX'
}

/**
 * FOS-W9-2: sync generate only accepts SYNC_STREAM (ADR-0038). Prefer that when allowed;
 * otherwise fall back to ASYNC_TASK for a batch-generate example.
 */
export function pickDefaultMode(allowed: string[]): { mode: string; kind: 'sync' | 'async' } {
  const normalized = allowed.map((item) => item.toUpperCase())
  if (normalized.includes('SYNC_STREAM')) {
    return { mode: 'SYNC_STREAM', kind: 'sync' }
  }
  if (normalized.includes('ASYNC_TASK')) {
    return { mode: 'ASYNC_TASK', kind: 'async' }
  }
  return { mode: allowed[0] ?? 'SYNC_STREAM', kind: 'sync' }
}

/** Prefer default route URL; else first callable explicit version URL. */
export function resolveGenerateUrl(contract: CallerContract): string {
  const defaultUrl = contract.defaultRoute?.url?.trim()
  if (defaultUrl) {
    return defaultUrl
  }
  const first = contract.callableVersions[0]?.explicitVersionUrl?.trim()
  return first ?? ''
}

/** Map a sync generate path to the corresponding batch-generate path. */
export function toBatchGenerateUrl(generateUrl: string): string {
  if (!generateUrl) {
    return generateUrl
  }
  if (generateUrl.includes('/batch-generate')) {
    return generateUrl
  }
  return generateUrl.replace(/\/generate(\b|$)/, '/batch-generate$1')
}

/**
 * Absolute URL for curl when the contract path is root-relative.
 * Falls back to the path itself when origin is unavailable.
 */
export function absolutizeGenerateUrl(pathOrUrl: string, origin?: string): string {
  if (!pathOrUrl) {
    return pathOrUrl
  }
  if (/^https?:\/\//i.test(pathOrUrl)) {
    return pathOrUrl
  }
  const base = origin ?? (typeof window !== 'undefined' ? window.location.origin : '')
  if (!base) {
    return pathOrUrl
  }
  if (pathOrUrl.startsWith('/')) {
    return `${base}${pathOrUrl}`
  }
  return `${base}/${pathOrUrl}`
}

export function buildGeneratePayload(
  contract: CallerContract,
  variables: Record<string, unknown> | null | undefined,
): GenerateExamplePayload {
  const allowedFormats = contract.apiPolicy?.allowedOutputFormats ?? []
  const allowedModes = contract.apiPolicy?.allowedOutputModes ?? []
  const picked = pickDefaultMode(allowedModes)
  return {
    output: {
      format: pickDefaultFormat(allowedFormats),
      mode: picked.mode,
    },
    variables: { ...(variables ?? {}) },
    requestId: REQUEST_ID_PLACEHOLDER,
    idempotencyKey: IDEMPOTENCY_KEY_PLACEHOLDER,
  }
}

function formatPayloadJson(payload: GenerateExamplePayload): string {
  return `${JSON.stringify(payload, null, 2)}\n`
}

function credentialHeaders(): string[] {
  return [
    `  -H 'X-Api-Credential-Id: ${API_CREDENTIAL_ID_PLACEHOLDER}' \\`,
    `  -H 'X-Api-Credential-Secret: ${API_CREDENTIAL_SECRET_PLACEHOLDER}' \\`,
    `  -H 'X-Access-Account: ${ACCESS_ACCOUNT_PLACEHOLDER}' \\`,
  ]
}

function buildSyncGenerateCurl(generateUrl: string, payloadJson: string): string {
  const body = payloadJson.trimEnd()
  return [
    `curl -X POST '${generateUrl}' \\`,
    ...credentialHeaders(),
    `  -H 'Content-Type: application/json' \\`,
    `  -d '${body.replace(/'/g, `'\\''`)}'`,
  ].join('\n')
}

function buildAsyncBatchCurl(batchUrl: string, payloadJson: string): string {
  const syncPayload = JSON.parse(payloadJson) as GenerateExamplePayload
  const batchBody = {
    output: syncPayload.output,
    items: [
      {
        itemId: 'item-1',
        variables: syncPayload.variables,
      },
    ],
    requestId: syncPayload.requestId,
    idempotencyKey: syncPayload.idempotencyKey,
  }
  const body = JSON.stringify(batchBody, null, 2)
  return [
    `curl -X POST '${batchUrl}' \\`,
    ...credentialHeaders(),
    `  -H 'Content-Type: application/json' \\`,
    `  -d '${body.replace(/'/g, `'\\''`)}'`,
  ].join('\n')
}

/** Prefer first unlocked set; else first available (U12-C4). */
export function pickDefaultTestDataSet(dataSets: TestDataSet[]): TestDataSet | null {
  if (dataSets.length === 0) {
    return null
  }
  const unlocked = dataSets.find((row) => !row.locked)
  return unlocked ?? dataSets[0] ?? null
}

export function buildContractCopyableExample(
  contract: CallerContract,
  selectedDataSet: TestDataSet | null,
  options?: { origin?: string },
): ContractCopyableExample {
  const relativeUrl = resolveGenerateUrl(contract)
  const allowedModes = contract.apiPolicy?.allowedOutputModes ?? []
  const picked = pickDefaultMode(allowedModes)
  const pathForKind =
    picked.kind === 'async' ? toBatchGenerateUrl(relativeUrl) : relativeUrl
  const generateUrl = absolutizeGenerateUrl(pathForKind, options?.origin)
  const hasTestDataSet = selectedDataSet != null
  const payload = buildGeneratePayload(contract, selectedDataSet?.variables)
  const payloadJson = formatPayloadJson(payload)
  const curl =
    picked.kind === 'async'
      ? buildAsyncBatchCurl(generateUrl, payloadJson)
      : buildSyncGenerateCurl(generateUrl, payloadJson)
  // FOS-W9-5: never surface opaque backend example tokens as the primary label.
  const exampleToken = picked.kind === 'async' ? 'batch-generate-async' : 'generate-sync'
  return {
    exampleToken,
    generateUrl,
    payload,
    payloadJson,
    curl,
    hasTestDataSet,
    exampleKind: picked.kind,
  }
}
