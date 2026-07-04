import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/auth'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'
import type { ManagementSession } from '@/types/session'

vi.mock('@/api/auth')

const MINUTE_MS = 60_000

function buildSession(overrides: Partial<ManagementSession> = {}): ManagementSession {
  return {
    username: '10000003',
    displayName: 'Template Author',
    email: 'author@example.com',
    authSource: 'LOCAL',
    roles: ['TEMPLATE_AUTHOR'],
    authorizedGroupCodes: ['RETAIL'],
    defaultRoute: ROUTE_KEYS.dashboardHome,
    visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement],
    expiresAt: new Date(Date.now() + 30 * MINUTE_MS).toISOString(),
    ...overrides,
  }
}

describe('session store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(authApi.logout).mockReset().mockResolvedValue(undefined)
    vi.mocked(authApi.login).mockReset()
    vi.mocked(authApi.renewSession).mockReset()
    vi.mocked(authApi.fetchSession).mockReset()
  })

  it('evaluates route visibility from session summary', () => {
    const store = useSessionStore()
    store.$patch({
      accessToken: 'token',
      session: buildSession(),
    })

    expect(store.canAccessRoute(ROUTE_KEYS.dashboardHome)).toBe(true)
    expect(store.canAccessRoute(ROUTE_KEYS.templateManagement)).toBe(true)
    expect(store.canAccessRoute(ROUTE_KEYS.globalGovernanceHome)).toBe(false)
    expect(store.canAccessRoute(ROUTE_KEYS.masterManagement)).toBe(false)
    expect(store.defaultHomePath()).toBe('/dashboard')
  })

  it('clears persisted token on logout', async () => {
    localStorage.setItem('docgen.accessToken', 'token')
    const store = useSessionStore()
    store.$patch({
      accessToken: 'token',
      session: buildSession({
        username: '10000001',
        displayName: 'Global Admin',
        email: 'global.admin@example.com',
        roles: ['GLOBAL_ADMIN'],
        authorizedGroupCodes: ['*'],
        defaultRoute: ROUTE_KEYS.globalGovernanceHome,
        visibleRoutes: [ROUTE_KEYS.globalGovernanceHome],
      }),
    })

    await store.logout()

    expect(store.authenticated).toBe(false)
    expect(localStorage.getItem('docgen.accessToken')).toBeNull()
  })

  it('clears local session state even when logout fails with 503 (revocation store down)', async () => {
    localStorage.setItem('docgen.accessToken', 'token')
    vi.mocked(authApi.logout).mockRejectedValue(
      axiosEnvelopeError(503, 'api.error.generation.serviceUnavailable', {
        code: 'SERVICE_UNAVAILABLE',
        category: 'GENERATION',
        retryable: true,
      }),
    )
    const store = useSessionStore()
    store.$patch({
      accessToken: 'token',
      session: buildSession(),
      accessTokenExpiresAt: new Date(Date.now() + 20 * MINUTE_MS).toISOString(),
      sessionAbsoluteDeadline: new Date(Date.now() + 7 * 60 * MINUTE_MS).toISOString(),
    })

    await store.logout()

    expect(store.authenticated).toBe(false)
    expect(localStorage.getItem('docgen.accessToken')).toBeNull()
    expect(store.accessTokenExpiresAt).toBeNull()
    expect(store.sessionAbsoluteDeadline).toBeNull()
  })

  it('stores renewal timestamps from the login response', async () => {
    const expiresAt = new Date(Date.now() + 30 * MINUTE_MS).toISOString()
    const deadline = new Date(Date.now() + 8 * 60 * MINUTE_MS).toISOString()
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'login-token',
      tokenType: 'Bearer',
      session: buildSession(),
      accessTokenExpiresAt: expiresAt,
      sessionAbsoluteDeadline: deadline,
    })
    const store = useSessionStore()

    await store.login('10000003', 'password')

    expect(store.accessToken).toBe('login-token')
    expect(localStorage.getItem('docgen.accessToken')).toBe('login-token')
    expect(store.accessTokenExpiresAt).toBe(expiresAt)
    expect(store.sessionAbsoluteDeadline).toBe(deadline)
  })

  it('renewSession atomically replaces token and timestamps without touching other stores', async () => {
    const store = useSessionStore()
    const masters = useMastersStore()
    masters.$patch({ lastErrorMessageKey: 'masters.error.loadList' })

    const oldExpiresAt = new Date(Date.now() + 4 * MINUTE_MS).toISOString()
    const deadline = new Date(Date.now() + 7 * 60 * MINUTE_MS).toISOString()
    localStorage.setItem('docgen.accessToken', 'old-token')
    store.$patch({
      accessToken: 'old-token',
      session: buildSession(),
      accessTokenExpiresAt: oldExpiresAt,
      sessionAbsoluteDeadline: deadline,
    })

    const newExpiresAt = new Date(Date.now() + 30 * MINUTE_MS).toISOString()
    vi.mocked(authApi.renewSession).mockResolvedValue({
      accessToken: 'new-token',
      tokenType: 'Bearer',
      session: buildSession({ expiresAt: newExpiresAt }),
      accessTokenExpiresAt: newExpiresAt,
      sessionAbsoluteDeadline: deadline,
    })

    await store.renewSession()

    expect(store.accessToken).toBe('new-token')
    expect(localStorage.getItem('docgen.accessToken')).toBe('new-token')
    expect(store.accessTokenExpiresAt).toBe(newExpiresAt)
    expect(store.sessionAbsoluteDeadline).toBe(deadline)
    expect(store.session?.username).toBe('10000003')
    expect(store.authenticated).toBe(true)
    // SCEN-UX-01: renewal must not reset other stores (form/editor state preserved).
    expect(masters.lastErrorMessageKey).toBe('masters.error.loadList')
  })

  it('discards a late renew response after logout instead of resurrecting the session', async () => {
    const store = useSessionStore()
    localStorage.setItem('docgen.accessToken', 'old-token')
    store.$patch({
      accessToken: 'old-token',
      session: buildSession(),
      accessTokenExpiresAt: new Date(Date.now() + 4 * MINUTE_MS).toISOString(),
      sessionAbsoluteDeadline: new Date(Date.now() + 7 * 60 * MINUTE_MS).toISOString(),
    })

    let resolveRenew!: (result: Awaited<ReturnType<typeof authApi.renewSession>>) => void
    vi.mocked(authApi.renewSession).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRenew = resolve
        }),
    )

    const renewPromise = store.renewSession()
    // The user signs out while the renew round-trip is still in flight; the
    // server revoked the old jti, but the response token is never revoked.
    await store.logout()

    resolveRenew({
      accessToken: 'late-unrevoked-token',
      tokenType: 'Bearer',
      session: buildSession(),
      accessTokenExpiresAt: new Date(Date.now() + 30 * MINUTE_MS).toISOString(),
      sessionAbsoluteDeadline: new Date(Date.now() + 7 * 60 * MINUTE_MS).toISOString(),
    })
    await renewPromise

    expect(store.accessToken).toBeNull()
    expect(localStorage.getItem('docgen.accessToken')).toBeNull()
    expect(store.session).toBeNull()
    expect(store.accessTokenExpiresAt).toBeNull()
    expect(store.sessionAbsoluteDeadline).toBeNull()
    expect(store.authenticated).toBe(false)
  })

  it('renewSession failure keeps the current session state and rethrows', async () => {
    const store = useSessionStore()
    const oldExpiresAt = new Date(Date.now() + 4 * MINUTE_MS).toISOString()
    const deadline = new Date(Date.now() + 5 * MINUTE_MS).toISOString()
    localStorage.setItem('docgen.accessToken', 'old-token')
    store.$patch({
      accessToken: 'old-token',
      session: buildSession(),
      accessTokenExpiresAt: oldExpiresAt,
      sessionAbsoluteDeadline: deadline,
    })

    vi.mocked(authApi.renewSession).mockRejectedValue(
      axiosEnvelopeError(401, 'api.error.authentication.sessionAbsoluteLimitReached', {
        code: 'SESSION_ABSOLUTE_LIMIT_REACHED',
        category: 'AUTHENTICATION',
        message: 'Your sign-in session has reached its maximum duration. Please sign in again.',
      }),
    )

    await expect(store.renewSession()).rejects.toBeTruthy()

    // The renew action itself never clears state; the shared 401 interceptor
    // owns the clear-session + redirect flow (COR-F03).
    expect(store.accessToken).toBe('old-token')
    expect(localStorage.getItem('docgen.accessToken')).toBe('old-token')
    expect(store.accessTokenExpiresAt).toBe(oldExpiresAt)
    expect(store.sessionAbsoluteDeadline).toBe(deadline)
  })

  it('restoreSession maps renewal timestamps from the session view', async () => {
    const expiresAt = new Date(Date.now() + 25 * MINUTE_MS).toISOString()
    const deadline = new Date(Date.now() + 6 * 60 * MINUTE_MS).toISOString()
    vi.mocked(authApi.fetchSession).mockResolvedValue(
      buildSession({ expiresAt, absoluteSessionExpiresAt: deadline }),
    )
    localStorage.setItem('docgen.accessToken', 'token')
    const store = useSessionStore()

    await expect(store.restoreSession()).resolves.toBe(true)

    expect(store.accessTokenExpiresAt).toBe(expiresAt)
    expect(store.sessionAbsoluteDeadline).toBe(deadline)
  })

  it('maps login failures from structured api envelope errors', () => {
    const store = useSessionStore()
    const error = axiosEnvelopeError(
      401,
      'api.error.authentication.authenticationFailed',
      { code: 'AUTHENTICATION_FAILED', category: 'AUTHENTICATION', message: 'Authentication failed.' },
      { traceId: 'TRC-LOGIN' },
    )

    expect(store.loginErrorMessageKey(error)).toBe('api.error.authentication.authenticationFailed')
  })
})
