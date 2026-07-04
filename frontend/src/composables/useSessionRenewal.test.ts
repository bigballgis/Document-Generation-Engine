import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/auth'
import {
  useSessionRenewal,
  type SessionRenewalController,
} from '@/composables/useSessionRenewal'
import { useSessionStore } from '@/stores/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'
import type { ManagementSession } from '@/types/session'

vi.mock('@/api/auth')

const MINUTE_MS = 60_000
const HOUR_MS = 60 * MINUTE_MS

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

function iso(offsetMs: number): string {
  return new Date(Date.now() + offsetMs).toISOString()
}

function patchAuthenticated(
  overrides: { accessTokenExpiresAt?: string; sessionAbsoluteDeadline?: string } = {},
) {
  const store = useSessionStore()
  localStorage.setItem('docgen.accessToken', 'old-token')
  store.$patch({
    accessToken: 'old-token',
    session: buildSession(),
    accessTokenExpiresAt: overrides.accessTokenExpiresAt ?? iso(30 * MINUTE_MS),
    sessionAbsoluteDeadline: overrides.sessionAbsoluteDeadline ?? iso(7 * HOUR_MS),
  })
  return store
}

function mockRenewSuccess() {
  vi.mocked(authApi.renewSession).mockImplementation(async () => ({
    accessToken: 'renewed-token',
    tokenType: 'Bearer',
    session: buildSession({ expiresAt: iso(30 * MINUTE_MS) }),
    accessTokenExpiresAt: iso(30 * MINUTE_MS),
    sessionAbsoluteDeadline: iso(7 * HOUR_MS),
  }))
}

describe('useSessionRenewal', () => {
  let controller: SessionRenewalController | null = null

  function createController(): SessionRenewalController {
    controller = useSessionRenewal()
    return controller
  }

  beforeEach(() => {
    vi.useFakeTimers()
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(authApi.renewSession).mockReset()
  })

  afterEach(() => {
    controller?.stop()
    controller = null
    vi.useRealTimers()
  })

  it('silently renews when the token nears expiry and the user is active', async () => {
    const store = patchAuthenticated({ accessTokenExpiresAt: iso(6 * MINUTE_MS) })
    mockRenewSuccess()

    createController()
    await vi.advanceTimersByTimeAsync(MINUTE_MS)
    // Remaining lifetime is exactly 5 minutes: still outside the strict window.
    expect(authApi.renewSession).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(MINUTE_MS)
    expect(authApi.renewSession).toHaveBeenCalledTimes(1)
    expect(store.accessToken).toBe('renewed-token')
    expect(localStorage.getItem('docgen.accessToken')).toBe('renewed-token')

    // Fresh token is far from expiry: no further renewals on later ticks.
    await vi.advanceTimersByTimeAsync(3 * MINUTE_MS)
    expect(authApi.renewSession).toHaveBeenCalledTimes(1)
  })

  it('does not renew for an idle user and resumes after new activity', async () => {
    patchAuthenticated({ accessTokenExpiresAt: iso(12 * MINUTE_MS) })
    mockRenewSuccess()

    createController()
    // 8 minutes of idle ticks: token enters the renewal window at minute 8
    // but the last activity (mount) is older than the activity window.
    await vi.advanceTimersByTimeAsync(8 * MINUTE_MS)
    expect(authApi.renewSession).not.toHaveBeenCalled()

    window.dispatchEvent(new Event('keydown'))
    await vi.advanceTimersByTimeAsync(MINUTE_MS)
    expect(authApi.renewSession).toHaveBeenCalledTimes(1)
  })

  it('shows the reminder and suspends renewal inside the absolute-limit window', async () => {
    patchAuthenticated({
      accessTokenExpiresAt: iso(4 * MINUTE_MS),
      sessionAbsoluteDeadline: iso(9 * MINUTE_MS),
    })
    mockRenewSuccess()

    const { reminderVisible } = createController()
    await vi.advanceTimersByTimeAsync(0)

    expect(reminderVisible.value).toBe(true)
    expect(authApi.renewSession).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(2 * MINUTE_MS)
    expect(authApi.renewSession).not.toHaveBeenCalled()
  })

  it('raises the reminder once the deadline crosses into the reminder window', async () => {
    patchAuthenticated({
      accessTokenExpiresAt: iso(25 * MINUTE_MS),
      sessionAbsoluteDeadline: iso(15 * MINUTE_MS),
    })

    const { reminderVisible } = createController()
    await vi.advanceTimersByTimeAsync(0)
    expect(reminderVisible.value).toBe(false)

    await vi.advanceTimersByTimeAsync(6 * MINUTE_MS)
    expect(reminderVisible.value).toBe(true)
    expect(authApi.renewSession).not.toHaveBeenCalled()
  })

  it('stops renewing after a SESSION_ABSOLUTE_LIMIT_REACHED 401', async () => {
    patchAuthenticated({ accessTokenExpiresAt: iso(4 * MINUTE_MS) })
    vi.mocked(authApi.renewSession).mockRejectedValue(
      axiosEnvelopeError(401, 'api.error.authentication.sessionAbsoluteLimitReached', {
        code: 'SESSION_ABSOLUTE_LIMIT_REACHED',
        category: 'AUTHENTICATION',
        message: 'Your sign-in session has reached its maximum duration. Please sign in again.',
      }),
    )

    createController()
    await vi.advanceTimersByTimeAsync(0)
    expect(authApi.renewSession).toHaveBeenCalledTimes(1)

    window.dispatchEvent(new Event('pointerdown'))
    await vi.advanceTimersByTimeAsync(3 * MINUTE_MS)
    expect(authApi.renewSession).toHaveBeenCalledTimes(1)
  })

  it('retries on the next tick after a transient renewal failure', async () => {
    const store = patchAuthenticated({ accessTokenExpiresAt: iso(4 * MINUTE_MS) })
    vi.mocked(authApi.renewSession).mockRejectedValueOnce(new Error('Network Error'))
    mockRenewSuccess()

    createController()
    await vi.advanceTimersByTimeAsync(0)
    expect(authApi.renewSession).toHaveBeenCalledTimes(1)
    expect(store.accessToken).toBe('old-token')

    window.dispatchEvent(new Event('keydown'))
    await vi.advanceTimersByTimeAsync(MINUTE_MS)
    expect(authApi.renewSession).toHaveBeenCalledTimes(2)
    expect(store.accessToken).toBe('renewed-token')
  })

  it('clears the reminder when the session is gone', async () => {
    const store = patchAuthenticated({
      accessTokenExpiresAt: iso(4 * MINUTE_MS),
      sessionAbsoluteDeadline: iso(5 * MINUTE_MS),
    })

    const { reminderVisible } = createController()
    await vi.advanceTimersByTimeAsync(0)
    expect(reminderVisible.value).toBe(true)

    store.clearSession()
    await vi.advanceTimersByTimeAsync(MINUTE_MS)
    expect(reminderVisible.value).toBe(false)
    expect(authApi.renewSession).not.toHaveBeenCalled()
  })
})
