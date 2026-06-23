import { AxiosError, AxiosHeaders } from 'axios'
import type { ApiEnvelope, ApiErrorDetail, ApiMetadata } from '@/types/session'

export function axiosEnvelopeError(
  status: number,
  messageKey: string,
  overrides: Partial<ApiErrorDetail> = {},
  metadata: ApiMetadata = {},
): AxiosError<ApiEnvelope<unknown>> {
  const error: ApiErrorDetail = {
    code: overrides.code ?? 'ACCESS_DENIED',
    category: overrides.category ?? 'AUTHORIZATION',
    message: overrides.message ?? 'Request failed.',
    messageKey,
    retryable: overrides.retryable ?? false,
    fieldErrors: overrides.fieldErrors,
  }
  return new AxiosError('Request failed', String(status), undefined, undefined, {
    status,
    statusText: 'Error',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: { metadata, error },
  })
}
