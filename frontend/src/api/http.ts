import axios, { type AxiosError } from 'axios'
import type { ApiEnvelope } from '@/types/session'

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

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      void import('@/router').then(({ default: routerInstance }) => {
        const currentRoute = routerInstance.currentRoute.value
        if (currentRoute.name !== 'login') {
          void routerInstance.push({
            name: 'login',
            query: { sessionExpired: '1' },
          })
        }
      })
    }
    return Promise.reject(error)
  },
)

export function isApiError(error: unknown): error is AxiosError<ApiEnvelope<unknown>> {
  return axios.isAxiosError(error)
}

export function envelopeErrorMessageKey(error: unknown): string | null {
  if (isApiError(error) && error.response?.data.error?.messageKey) {
    return error.response.data.error.messageKey
  }
  return null
}

export { TOKEN_STORAGE_KEY }
