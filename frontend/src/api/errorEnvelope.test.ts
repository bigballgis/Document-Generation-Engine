import { describe, expect, it } from 'vitest'
import {
  parseApiEnvelopeError,
  resolveApiError,
  resolveApiErrorMessageKey,
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
})
