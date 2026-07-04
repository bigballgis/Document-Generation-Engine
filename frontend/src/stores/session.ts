import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import { canAccessRouteWithCapability } from '@/auth/routeCapabilities'
import { resolveApiErrorMessageKey, TOKEN_STORAGE_KEY } from '@/api/http'
import { pathForRouteKey } from '@/routing/routeKeys'
import type { LoginResult, ManagementSession } from '@/types/session'

export const useSessionStore = defineStore('session', () => {
  const accessToken = ref<string | null>(localStorage.getItem(TOKEN_STORAGE_KEY))
  const session = ref<ManagementSession | null>(null)
  const accessTokenExpiresAt = ref<string | null>(null)
  const sessionAbsoluteDeadline = ref<string | null>(null)
  const loading = ref(false)
  const lastDenyTraceId = ref<string | null>(null)

  const authenticated = computed(() => Boolean(accessToken.value && session.value))

  function persistToken(token: string | null) {
    accessToken.value = token
    if (token) {
      localStorage.setItem(TOKEN_STORAGE_KEY, token)
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
    }
  }

  /**
   * Atomically applies a login/renew response: token, session view, and the
   * server-issued renewal timestamps (never derived from the JWT or the local
   * clock — spec B9).
   */
  function applyAuthResult(result: LoginResult) {
    persistToken(result.accessToken)
    session.value = result.session
    accessTokenExpiresAt.value = result.accessTokenExpiresAt ?? result.session.expiresAt ?? null
    sessionAbsoluteDeadline.value =
      result.sessionAbsoluteDeadline ?? result.session.absoluteSessionExpiresAt ?? null
  }

  function clearSession() {
    persistToken(null)
    session.value = null
    accessTokenExpiresAt.value = null
    sessionAbsoluteDeadline.value = null
  }

  function canAccessRoute(routeKey: string): boolean {
    return canAccessRouteWithCapability(routeKey, session.value)
  }

  function hasRole(role: string): boolean {
    return session.value?.roles.includes(role) ?? false
  }

  function defaultHomePath(): string {
    if (!session.value) {
      return '/login'
    }
    return pathForRouteKey(session.value.defaultRoute)
  }

  async function login(username: string, password: string): Promise<void> {
    loading.value = true
    try {
      const result = await authApi.login(username, password)
      applyAuthResult(result)
      const { useMastersStore } = await import('@/stores/masters')
      useMastersStore().clearListError()
    } finally {
      loading.value = false
    }
  }

  /**
   * Silent sliding renewal (LR-B6, SCEN-UX-01): swaps the token and renewal
   * timestamps in place without navigation, dialogs, or resetting other
   * stores. Failures propagate to the caller; the shared 401 interceptor owns
   * the clear-session + redirect flow (COR-F03).
   */
  async function renewSession(): Promise<void> {
    const result = await authApi.renewSession()
    if (!accessToken.value) {
      // The user signed out while the renew round-trip was in flight: logout
      // revoked the old jti, but the token in this late response was never
      // revoked. Persisting it would resurrect the session client-side, so
      // discard the result (spec D2: logout takes effect immediately).
      return
    }
    applyAuthResult(result)
  }

  async function restoreSession(): Promise<boolean> {
    if (!accessToken.value) {
      return false
    }
    loading.value = true
    try {
      const restored = await authApi.fetchSession()
      session.value = restored
      accessTokenExpiresAt.value = restored.expiresAt ?? null
      sessionAbsoluteDeadline.value = restored.absoluteSessionExpiresAt ?? null
      return true
    } catch {
      clearSession()
      return false
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    if (accessToken.value) {
      try {
        await authApi.logout()
      } catch {
        // Stateless logout: clear local session even if the server call fails.
      }
    }
    clearSession()
  }

  function loginErrorMessageKey(error: unknown): string {
    return resolveApiErrorMessageKey(error, 'login.errorGeneric')
  }

  function recordRouteDenial(traceId: string | null) {
    lastDenyTraceId.value = traceId
  }

  return {
    accessToken,
    session,
    accessTokenExpiresAt,
    sessionAbsoluteDeadline,
    loading,
    authenticated,
    lastDenyTraceId,
    clearSession,
    canAccessRoute,
    hasRole,
    defaultHomePath,
    login,
    renewSession,
    restoreSession,
    logout,
    loginErrorMessageKey,
    recordRouteDeny: recordRouteDenial,
  }
})
