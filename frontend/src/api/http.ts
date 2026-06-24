import axios, { type AxiosError } from 'axios'
import type { RouteLocationRaw } from 'vue-router'
import type { ApiEnvelope } from '@/types/session'
import { resolveApiError } from './errorEnvelope'

const TOKEN_STORAGE_KEY = 'docgen.accessToken'

export const http = axios.create({
  baseURL: '/api/management/v1',
  headers: {
    Accept: 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export interface AuthHttpErrorHandlerDeps {
  clearSession: () => void
  recordDeny: (traceId: string | null) => void
  getCurrentRouteName: () => string | symbol | null | undefined
  getCurrentFullPath: () => string
  push: (location: RouteLocationRaw) => Promise<unknown>
}

export async function handleAuthHttpError(
  error: AxiosError<ApiEnvelope<unknown>>,
  deps: AuthHttpErrorHandlerDeps,
): Promise<void> {
  const status = error.response?.status
  if (status === 401) {
    deps.clearSession()
    if (deps.getCurrentRouteName() !== 'login') {
      await deps.push({
        name: 'login',
        query: {
          sessionExpired: '1',
          redirect: deps.getCurrentFullPath(),
        },
      })
    }
    return
  }
  if (status === 403) {
    const resolved = resolveApiError(error)
    const traceId = resolved?.metadata.traceId ?? null
    deps.recordDeny(traceId)
    if (deps.getCurrentRouteName() !== 'forbidden') {
      await deps.push({
        name: 'forbidden',
        query: traceId ? { traceId } : undefined,
      })
    }
  }
}

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      void (async () => {
        const [{ default: routerInstance }, { useSessionStore }] = await Promise.all([
          import('@/router'),
          import('@/stores/session'),
        ])
        const sessionStore = useSessionStore()
        await handleAuthHttpError(error, {
          clearSession: () => sessionStore.clearSession(),
          recordDeny: (traceId) => sessionStore.recordRouteDeny(traceId),
          getCurrentRouteName: () => routerInstance.currentRoute.value.name,
          getCurrentFullPath: () => routerInstance.currentRoute.value.fullPath,
          push: (location) => routerInstance.push(location),
        })
      })()
    }
    return Promise.reject(error)
  },
)

export {
  isApiError,
  parseApiEnvelopeError,
  resolveApiError,
  resolveApiErrorMessageKey,
  resolveStoreErrorMessageKey,
  isAuthHttpError,
  type ResolvedApiError,
} from './errorEnvelope'

export { TOKEN_STORAGE_KEY }
