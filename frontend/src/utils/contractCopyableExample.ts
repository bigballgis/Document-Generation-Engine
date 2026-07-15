import type { CallerContract } from '@/types/contract'
import type { TestDataSet } from '@/types/templatePreview'

/** Fixed placeholders — never embed real secrets (U12-C3). */
export const ACCESS_TOKEN_PLACEHOLDER = '<ACCESS_TOKEN>'
export const IDEMPOTENCY_KEY_PLACEHOLDER = '<IDEMPOTENCY_KEY>'
export const REQUEST_ID_PLACEHOLDER = '<REQUEST_ID>'

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
}

const SYNC_MODE_PREFERENCE = ['SYNC_STREAM', 'SYNC_DOWNLOAD_URL'] as const

function pickDefaultFormat(allowed: string[]): string {
  if (allowed.includes('DOCX')) {
    return 'DOCX'
  }
  return allowed[0] ?? 'DOCX'
}

function pickDefaultMode(allowed: string[]): string {
  for (const preferred of SYNC_MODE_PREFERENCE) {
    if (allowed.includes(preferred)) {
      return preferred
    }
  }
  return allowed[0] ?? 'SYNC_STREAM'
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
  return {
    output: {
      format: pickDefaultFormat(allowedFormats),
      mode: pickDefaultMode(allowedModes),
    },
    variables: { ...(variables ?? {}) },
    requestId: REQUEST_ID_PLACEHOLDER,
    idempotencyKey: IDEMPOTENCY_KEY_PLACEHOLDER,
  }
}

export function formatPayloadJson(payload: GenerateExamplePayload): string {
  return `${JSON.stringify(payload, null, 2)}\n`
}

export function buildSyncGenerateCurl(generateUrl: string, payloadJson: string): string {
  const body = payloadJson.trimEnd()
  return [
    `curl -X POST '${generateUrl}' \\`,
    `  -H 'Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}' \\`,
    `  -H 'Idempotency-Key: ${IDEMPOTENCY_KEY_PLACEHOLDER}' \\`,
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
  const generateUrl = absolutizeGenerateUrl(relativeUrl, options?.origin)
  const hasTestDataSet = selectedDataSet != null
  const payload = buildGeneratePayload(contract, selectedDataSet?.variables)
  const payloadJson = formatPayloadJson(payload)
  const curl = buildSyncGenerateCurl(generateUrl, payloadJson)
  const exampleToken = contract.examples[0] ?? 'generate-sync'
  return {
    exampleToken,
    generateUrl,
    payload,
    payloadJson,
    curl,
    hasTestDataSet,
  }
}
