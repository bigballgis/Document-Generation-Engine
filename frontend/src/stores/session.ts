import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import { resolveApiErrorMessageKey, TOKEN_STORAGE_KEY } from '@/api/http'
import { pathForRouteKey } from '@/routing/routeKeys'
import type { ManagementSession } from '@/types/session'

export const useSessionStore = defineStore('session', () => {
  const accessToken = ref<string | null>(localStorage.getItem(TOKEN_STORAGE_KEY))
  const session = ref<ManagementSession | null>(null)
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

  function clearSession() {
    persistToken(null)
    session.value = null
  }

  function canAccessRoute(routeKey: string): boolean {
    return session.value?.visibleRoutes.includes(routeKey) ?? false
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
      persistToken(result.accessToken)
      session.value = result.session
    } finally {
      loading.value = false
    }
  }

  async function restoreSession(): Promise<boolean> {
    if (!accessToken.value) {
      return false
    }
    loading.value = true
    try {
      session.value = await authApi.fetchSession()
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
    loading,
    authenticated,
    lastDenyTraceId,
    clearSession,
    canAccessRoute,
    hasRole,
    defaultHomePath,
    login,
    restoreSession,
    logout,
    loginErrorMessageKey,
    recordRouteDeny: recordRouteDenial,
  }
})
