import { http } from '@/api/http'
import type { ApiEnvelope, LoginResult, ManagementSession } from '@/types/session'

export async function login(username: string, password: string): Promise<LoginResult> {
  const response = await http.post<ApiEnvelope<LoginResult>>('/auth/login', {
    username,
    password,
  })
  if (!response.data.result) {
    throw new Error('Login response missing result')
  }
  return response.data.result
}

export async function fetchSession(): Promise<ManagementSession> {
  const response = await http.get<ApiEnvelope<ManagementSession>>('/auth/session')
  if (!response.data.result) {
    throw new Error('Session response missing result')
  }
  return response.data.result
}

export async function logout(): Promise<void> {
  await http.post('/auth/logout')
}
