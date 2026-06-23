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

export function isApiError(error: unknown): error is AxiosError<ApiEnvelope<unknown>> {
  return axios.isAxiosError(error)
}

export { TOKEN_STORAGE_KEY }
