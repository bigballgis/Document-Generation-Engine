import { APIRequestContext, test } from '@playwright/test'

export const DEFAULT_FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
export const DEFAULT_BACKEND_HEALTH_URL = 'http://127.0.0.1:8080/healthz'
const DEFAULT_TIMEOUT_MS = 5_000

export type StackReadinessOptions = {
  frontendBaseUrl?: string
  backendHealthUrl?: string
  timeoutMs?: number
  skipMessage?: string
}

export async function isBackendReady(
  request: APIRequestContext,
  backendHealthUrl: string = DEFAULT_BACKEND_HEALTH_URL,
  timeoutMs: number = DEFAULT_TIMEOUT_MS,
): Promise<boolean> {
  try {
    const response = await request.get(backendHealthUrl, { timeout: timeoutMs })
    return response.ok()
  } catch {
    return false
  }
}

export async function requireBackendReady(
  request: APIRequestContext,
  options: Pick<StackReadinessOptions, 'backendHealthUrl' | 'timeoutMs' | 'skipMessage'> = {},
): Promise<void> {
  const ready = await isBackendReady(
    request,
    options.backendHealthUrl ?? DEFAULT_BACKEND_HEALTH_URL,
    options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
  )
  test.skip(!ready, options.skipMessage ?? 'Backend :8080 required. Start the Docker stack before running E2E.')
}

export async function isFrontendReady(
  request: APIRequestContext,
  frontendBaseUrl: string = DEFAULT_FRONTEND_BASE_URL,
  timeoutMs: number = DEFAULT_TIMEOUT_MS,
): Promise<boolean> {
  try {
    const response = await request.get(frontendBaseUrl, { timeout: timeoutMs })
    return response.ok()
  } catch {
    return false
  }
}

export async function isDockerStackReady(
  request: APIRequestContext,
  options: StackReadinessOptions = {},
): Promise<boolean> {
  const frontendBaseUrl = options.frontendBaseUrl ?? DEFAULT_FRONTEND_BASE_URL
  const backendHealthUrl = options.backendHealthUrl ?? DEFAULT_BACKEND_HEALTH_URL
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS
  const [backendReady, frontendReady] = await Promise.all([
    isBackendReady(request, backendHealthUrl, timeoutMs),
    isFrontendReady(request, frontendBaseUrl, timeoutMs),
  ])
  return backendReady && frontendReady
}

export function skipUnlessDockerStackReady(
  ready: boolean,
  frontendBaseUrl: string = DEFAULT_FRONTEND_BASE_URL,
  skipMessage?: string,
): void {
  test.skip(
    !ready,
    skipMessage
      ?? `Stack required (${frontendBaseUrl} + backend :8080). Start backend and frontend before running E2E.`,
  )
}

export async function requireDockerStack(
  request: APIRequestContext,
  options: StackReadinessOptions = {},
): Promise<void> {
  const frontendBaseUrl = options.frontendBaseUrl ?? DEFAULT_FRONTEND_BASE_URL
  const ready = await isDockerStackReady(request, options)
  skipUnlessDockerStackReady(ready, frontendBaseUrl, options.skipMessage)
}
