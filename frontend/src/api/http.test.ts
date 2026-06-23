import { describe, expect, it, vi } from 'vitest'
import { handleAuthHttpError } from '@/api/http'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

describe('handleAuthHttpError', () => {
  it('clears session and redirects to login with sessionExpired on 401', async () => {
    const clearSession = vi.fn()
    const recordDeny = vi.fn()
    const push = vi.fn().mockResolvedValue(undefined)

    await handleAuthHttpError(
      axiosEnvelopeError(
        401,
        'api.error.authentication.authenticationFailed',
        { code: 'AUTHENTICATION_FAILED', category: 'AUTHENTICATION', message: 'Authentication failed.' },
        { traceId: 'TRC-401' },
      ),
      {
        clearSession,
        recordDeny,
        getCurrentRouteName: () => 'master-list',
        getCurrentFullPath: () => '/masters?filter=pending',
        push,
      },
    )

    expect(clearSession).toHaveBeenCalledOnce()
    expect(push).toHaveBeenCalledWith({
      name: 'login',
      query: { sessionExpired: '1', redirect: '/masters?filter=pending' },
    })
    expect(recordDeny).not.toHaveBeenCalled()
  })

  it('does not redirect away from login on 401', async () => {
    const push = vi.fn().mockResolvedValue(undefined)

    await handleAuthHttpError(
      axiosEnvelopeError(
        401,
        'api.error.authentication.authenticationFailed',
        { code: 'AUTHENTICATION_FAILED', category: 'AUTHENTICATION', message: 'Authentication failed.' },
      ),
      {
        clearSession: vi.fn(),
        recordDeny: vi.fn(),
        getCurrentRouteName: () => 'login',
        getCurrentFullPath: () => '/login',
        push,
      },
    )

    expect(push).not.toHaveBeenCalled()
  })

  it('records traceId and redirects to forbidden on 403', async () => {
    const recordDeny = vi.fn()
    const push = vi.fn().mockResolvedValue(undefined)

    await handleAuthHttpError(
      axiosEnvelopeError(
        403,
        'api.error.authorization.accessDenied',
        { code: 'ACCESS_DENIED', category: 'AUTHORIZATION', message: 'Access denied.' },
        { traceId: 'TRC-403' },
      ),
      {
        clearSession: vi.fn(),
        recordDeny,
        getCurrentRouteName: () => 'template-list',
        getCurrentFullPath: () => '/templates',
        push,
      },
    )

    expect(recordDeny).toHaveBeenCalledWith('TRC-403')
    expect(push).toHaveBeenCalledWith({ name: 'forbidden', query: { traceId: 'TRC-403' } })
  })
})
