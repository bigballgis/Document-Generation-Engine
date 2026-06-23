import axios, { type AxiosError } from 'axios'
import type { ApiEnvelope, ApiErrorDetail, ApiFieldError, ApiMetadata } from '@/types/session'

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

export function isApiError(error: unknown): error is AxiosError<ApiEnvelope<unknown>> {
  return axios.isAxiosError(error)
}

export function resolveApiError(error: unknown): ResolvedApiError | null {
  if (!isApiError(error)) {
    return null
  }
  return parseApiEnvelopeError(error.response?.data)
}

export function resolveApiErrorMessageKey(error: unknown, fallbackKey: string): string {
  return resolveApiError(error)?.error.messageKey ?? fallbackKey
}
