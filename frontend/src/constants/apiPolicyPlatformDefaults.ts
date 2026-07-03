/** Mirrors backend `ApiPolicyPlatformDefaults` — single source for UI "platform defaults" labels. */
export const API_POLICY_PLATFORM_DEFAULTS = {
  outputFormats: ['DOCX', 'PDF'],
  outputModes: ['SYNC_STREAM', 'SYNC_DOWNLOAD_URL', 'ASYNC_TASK'],
  batchEnabled: true,
  syncMaxItems: 100,
  asyncMaxItems: 10_000,
  docxEncryptionEnabled: false,
  pdfEncryptionEnabled: false,
  saveGeneratedDocuments: true,
  invocationRecordRetentionDays: 90,
  documentRetentionDays: 30,
} as const

/** BDD invocation record retention presets (days). */
export const INVOCATION_RECORD_RETENTION_PRESETS = [7, 30, 90, 180, 365, 1095, 2555] as const

/** BDD document retention presets (days) — only when saveGeneratedDocuments is true. */
export const DOCUMENT_RETENTION_PRESETS = [7, 30, 90, 180, 365] as const

export function arraysEqual<T>(left: readonly T[], right: readonly T[]): boolean {
  if (left.length !== right.length) {
    return false
  }
  return left.every((value, index) => value === right[index])
}

export function isOutputPolicyPlatformDefault(
  outputFormats: readonly string[],
  outputModes: readonly string[],
): boolean {
  return (
    arraysEqual(outputFormats, API_POLICY_PLATFORM_DEFAULTS.outputFormats) &&
    arraysEqual(outputModes, API_POLICY_PLATFORM_DEFAULTS.outputModes)
  )
}

export function isBatchLimitsPlatformDefault(
  batchEnabled: boolean,
  syncMaxItems: number,
  asyncMaxItems: number,
): boolean {
  return (
    batchEnabled === API_POLICY_PLATFORM_DEFAULTS.batchEnabled &&
    syncMaxItems === API_POLICY_PLATFORM_DEFAULTS.syncMaxItems &&
    asyncMaxItems === API_POLICY_PLATFORM_DEFAULTS.asyncMaxItems
  )
}

export function isEncryptionPlatformDefault(
  docxEncryptionEnabled: boolean,
  pdfEncryptionEnabled: boolean,
): boolean {
  return (
    docxEncryptionEnabled === API_POLICY_PLATFORM_DEFAULTS.docxEncryptionEnabled &&
    pdfEncryptionEnabled === API_POLICY_PLATFORM_DEFAULTS.pdfEncryptionEnabled
  )
}
