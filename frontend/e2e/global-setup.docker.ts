/**
 * FOS-W12-3 — fail the smoke job when the acceptance stack is not reachable.
 * Does not start a second compose project; callers must provision via docker-deploy-queue.
 */
import {
  DEFAULT_BACKEND_HEALTH_URL,
  DEFAULT_FRONTEND_BASE_URL,
} from './helpers/stack-readiness'

async function probe(url: string, timeoutMs = 5_000): Promise<boolean> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    const response = await fetch(url, { signal: controller.signal })
    return response.ok
  } catch {
    return false
  } finally {
    clearTimeout(timer)
  }
}

export default async function globalSetup(): Promise<void> {
  const frontendBaseUrl = process.env.E2E_BASE_URL ?? DEFAULT_FRONTEND_BASE_URL
  const backendHealthUrl = process.env.E2E_BACKEND_HEALTH_URL ?? DEFAULT_BACKEND_HEALTH_URL
  const [backendReady, frontendReady] = await Promise.all([
    probe(backendHealthUrl),
    probe(frontendBaseUrl),
  ])
  if (backendReady && frontendReady) {
    return
  }
  throw new Error(
    [
      'Playwright docker smoke requires a pre-provisioned stack (FOS-W12-3).',
      `backend health (${backendHealthUrl}): ${backendReady ? 'ok' : 'UNREACHABLE'}`,
      `frontend (${frontendBaseUrl}): ${frontendReady ? 'ok' : 'UNREACHABLE'}`,
      'Provision with: pwsh ./scripts/docker-deploy-queue.ps1',
      'Do not treat skip-all as green.',
    ].join('\n'),
  )
}
