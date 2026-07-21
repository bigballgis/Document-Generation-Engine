import axios, { type AxiosError } from 'axios'
import type { ApiEnvelope, ApiErrorDetail, ApiFieldError, ApiMetadata } from '@/types/session'
import type { TemplateImportDependencyReport } from '@/types/template'

export interface ResolvedApiError {
  error: ApiErrorDetail
  metadata: ApiMetadata
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function parseFieldError(value: unknown): ApiFieldError | null {
  if (!isRecord(value)) {
    return null
  }
  const { field, reason, message } = value
  if (typeof field !== 'string' || typeof reason !== 'string' || typeof message !== 'string') {
    return null
  }
  return { field, reason, message }
}

function parseDependencyReport(value: unknown): TemplateImportDependencyReport | undefined {
  if (!isRecord(value)) {
    return undefined
  }
  if (
    !Array.isArray(value.items) ||
    typeof value.blockingCount !== 'number' ||
    typeof value.warningCount !== 'number' ||
    typeof value.infoCount !== 'number' ||
    typeof value.readyToCommit !== 'boolean'
  ) {
    return undefined
  }
  return value as TemplateImportDependencyReport
}

function parseErrorDetail(value: unknown): ApiErrorDetail | null {
  if (!isRecord(value)) {
    return null
  }
  const { code, category, message, messageKey, retryable } = value
  if (
    typeof code !== 'string' ||
    typeof category !== 'string' ||
    typeof message !== 'string' ||
    typeof messageKey !== 'string' ||
    typeof retryable !== 'boolean'
  ) {
    return null
  }
  const detail: ApiErrorDetail = { code, category, message, messageKey, retryable }
  if (Array.isArray(value.fieldErrors)) {
    detail.fieldErrors = value.fieldErrors
      .map(parseFieldError)
      .filter((item): item is ApiFieldError => item !== null)
  }
  const dependencyReport = parseDependencyReport(value.dependencyReport)
  if (dependencyReport) {
    detail.dependencyReport = dependencyReport
  }
  return detail
}

function parseMetadata(value: unknown): ApiMetadata {
  if (!isRecord(value)) {
    return {}
  }
  return {
    auditId: typeof value.auditId === 'string' ? value.auditId : undefined,
    traceId: typeof value.traceId === 'string' ? value.traceId : undefined,
  }
}

export function parseApiEnvelopeError(data: unknown): ResolvedApiError | null {
  if (!isRecord(data)) {
    return null
  }
  const error = parseErrorDetail(data.error)
  if (!error) {
    return null
  }
  return {
    error,
    metadata: parseMetadata(data.metadata),
  }
}

function isApiError(error: unknown): error is AxiosError<ApiEnvelope<unknown>> {
  return axios.isAxiosError(error)
}

export function resolveApiError(error: unknown): ResolvedApiError | null {
  if (!isApiError(error)) {
    return null
  }
  return parseApiEnvelopeError(error.response?.data)
}

/** CE-E01 commit gate: extract dependency report from 422 envelope when present. */
export function resolveApiDependencyReport(error: unknown): TemplateImportDependencyReport | null {
  return resolveApiError(error)?.error.dependencyReport ?? null
}

/**
 * Prefer envelope `messageKey`. For gateway/nginx 413 HTML (or empty body) with no
 * JSON envelope, map to a readable master upload size key so UI never surfaces raw HTML.
 */
export function resolveApiErrorMessageKey(error: unknown, fallbackKey: string): string {
  const fromEnvelope = resolveApiError(error)?.error.messageKey
  if (fromEnvelope) {
    return fromEnvelope
  }
  if (isApiError(error) && error.response?.status === 413) {
    return 'masters.upload.errorTooLarge'
  }
  return fallbackKey
}

function isAuthHttpError(error: unknown): boolean {
  if (!isApiError(error)) {
    return false
  }
  const status = error.response?.status
  return status === 401 || status === 403
}

/** Skip surfacing store errors for auth failures handled by the HTTP interceptor. */
export function resolveStoreErrorMessageKey(error: unknown, fallbackKey: string): string | null {
  if (isAuthHttpError(error)) {
    return null
  }
  return resolveApiErrorMessageKey(error, fallbackKey)
}
