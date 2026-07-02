/**
 * Removes Playwright fixture templates/modules left in the shared Docker catalog.
 * Skipped when E2E_SKIP_CATALOG_CLEANUP=true (e.g. debugging a single fixture).
 */
import { E2E_ADMIN, FOL_TEMPLATE_EXTERNAL_ID } from './helpers/auth'

const API_BASE =
  process.env.E2E_API_BASE_URL ?? `http://127.0.0.1:${process.env.BACKEND_PORT ?? '8080'}/api/management/v1`

const E2E_FIXTURE_PREFIX = 'E2E-'

interface ApiEnvelope<T> {
  result: T
}

interface TemplateSummary {
  id: string
  externalId: string
}

async function apiLogin(): Promise<string> {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: E2E_ADMIN.username, password: E2E_ADMIN.password }),
  })
  if (!response.ok) {
    throw new Error(`Teardown login failed (${response.status}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<{ accessToken: string }>
  return body.result.accessToken
}

async function authorizedGet<T>(token: string, pathSuffix: string): Promise<T> {
  const response = await fetch(`${API_BASE}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    throw new Error(`GET ${pathSuffix} failed (${response.status}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

async function deleteTemplate(token: string, templateId: string, externalId: string): Promise<void> {
  const response = await fetch(`${API_BASE}/templates/${templateId}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ reason: 'E2E global teardown', confirmed: true }),
  })
  if (!response.ok) {
    console.warn(`  Failed to delete template ${externalId}: ${response.status} ${await response.text()}`)
  }
}

export default async function globalTeardown(): Promise<void> {
  if (process.env.E2E_SKIP_CATALOG_CLEANUP === 'true') {
    console.log('E2E catalog cleanup skipped (E2E_SKIP_CATALOG_CLEANUP=true).')
    return
  }

  try {
    const token = await apiLogin()
    const templates = await authorizedGet<TemplateSummary[]>('/templates', token)
    const e2eTemplates = templates.filter(
      (template) =>
        template.externalId.startsWith(E2E_FIXTURE_PREFIX) &&
        template.externalId !== FOL_TEMPLATE_EXTERNAL_ID,
    )

    if (e2eTemplates.length > 0) {
      console.log(`E2E teardown: removing ${e2eTemplates.length} fixture template(s)...`)
      for (const template of e2eTemplates) {
        await deleteTemplate(token, template.id, template.externalId)
      }
    }
  } catch (error) {
    console.warn(`E2E catalog teardown skipped: ${error instanceof Error ? error.message : String(error)}`)
  }
}
