/** Client precheck aligned with backend `docgen.master.max-docx-upload-bytes` (50 MiB). */
export const MASTER_DOCX_MAX_UPLOAD_BYTES = 50 * 1024 * 1024

const DOCX_CONTENT_TYPES = new Set([
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/octet-stream',
])

export type MasterDocxUploadValidationResult =
  | { ok: true }
  | { ok: false; messageKey: 'masters.upload.errorTooLarge' | 'masters.upload.errorDocxOnly' }

/**
 * Shared create/replace client guard: size ≤ 50MB, `.docx` suffix, optional MIME whitelist.
 */
export function validateMasterDocxUploadFile(file: File): MasterDocxUploadValidationResult {
  if (file.size > MASTER_DOCX_MAX_UPLOAD_BYTES) {
    return { ok: false, messageKey: 'masters.upload.errorTooLarge' }
  }
  const lowerName = file.name.toLowerCase()
  if (!lowerName.endsWith('.docx')) {
    return { ok: false, messageKey: 'masters.upload.errorDocxOnly' }
  }
  if (file.type && !DOCX_CONTENT_TYPES.has(file.type)) {
    return { ok: false, messageKey: 'masters.upload.errorDocxOnly' }
  }
  return { ok: true }
}
