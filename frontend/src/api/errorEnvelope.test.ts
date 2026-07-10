import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it } from 'vitest'
import {
  parseApiEnvelopeError,
  resolveApiError,
  resolveApiErrorMessageKey,
  resolveStoreErrorMessageKey,
} from '@/api/errorEnvelope'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

describe('errorEnvelope', () => {
  it('parses contract-aligned error detail with metadata', () => {
    const parsed = parseApiEnvelopeError({
      metadata: { auditId: 'AUD-1', traceId: 'TRC-1' },
      error: {
        code: 'ACCESS_DENIED',
        category: 'AUTHORIZATION',
        message: 'Access denied.',
        messageKey: 'api.error.authorization.accessDenied',
        retryable: false,
      },
    })

    expect(parsed).toEqual({
      metadata: { auditId: 'AUD-1', traceId: 'TRC-1' },
      error: {
        code: 'ACCESS_DENIED',
        category: 'AUTHORIZATION',
        message: 'Access denied.',
        messageKey: 'api.error.authorization.accessDenied',
        retryable: false,
      },
    })
  })

  it('rejects envelopes missing required error fields', () => {
    expect(
      parseApiEnvelopeError({
        metadata: {},
        error: {
          code: 'ACCESS_DENIED',
          message: 'Access denied.',
          messageKey: 'api.error.authorization.accessDenied',
        },
      }),
    ).toBeNull()
  })

  it('resolves axios errors into structured api errors', () => {
    const axiosError = axiosEnvelopeError(
      403,
      'api.error.authorization.accessDenied',
      { code: 'ACCESS_DENIED', category: 'AUTHORIZATION', message: 'Access denied.' },
      { traceId: 'TRC-403' },
    )

    expect(resolveApiError(axiosError)).toEqual({
      metadata: { traceId: 'TRC-403' },
      error: {
        code: 'ACCESS_DENIED',
        category: 'AUTHORIZATION',
        message: 'Access denied.',
        messageKey: 'api.error.authorization.accessDenied',
        retryable: false,
      },
    })
  })

  it('falls back to caller key when envelope is absent', () => {
    expect(resolveApiErrorMessageKey(new Error('network'), 'templates.error.loadList')).toBe(
      'templates.error.loadList',
    )
  })

  it('maps nginx/HTML 413 payload-too-large to readable master upload key', () => {
    const axiosError = new AxiosError('Request failed', '413', undefined, undefined, {
      status: 413,
      statusText: 'Payload Too Large',
      headers: { 'content-type': 'text/html' },
      config: { headers: new AxiosHeaders() },
      data: '<html><head><title>413 Request Entity Too Large</title></head><body>nginx</body></html>',
    })

    expect(resolveApiErrorMessageKey(axiosError, 'masters.error.upload')).toBe(
      'masters.upload.errorTooLarge',
    )
    expect(resolveApiErrorMessageKey(axiosError, 'masters.error.replaceFile')).toBe(
      'masters.upload.errorTooLarge',
    )
  })

  it('prefers envelope messageKey for Spring 413 docxTooLarge', () => {
    const axiosError = axiosEnvelopeError(413, 'api.error.master.docxTooLarge', {
      code: 'MASTER_VALIDATION_FAILED',
      category: 'VALIDATION',
      message: 'The uploaded DOCX exceeds the maximum allowed size.',
    })

    expect(resolveApiErrorMessageKey(axiosError, 'masters.error.upload')).toBe(
      'api.error.master.docxTooLarge',
    )
  })

  it('skips store error keys for auth failures', () => {
    const axiosError = axiosEnvelopeError(
      401,
      'api.error.authentication.sessionExpired',
      {
        code: 'SESSION_EXPIRED',
        category: 'AUTHENTICATION',
        message: 'Session expired.',
      },
    )
    expect(resolveStoreErrorMessageKey(axiosError, 'masters.error.loadList')).toBeNull()
  })
})
