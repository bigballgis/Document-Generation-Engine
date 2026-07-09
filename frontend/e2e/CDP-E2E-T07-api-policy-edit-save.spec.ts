import { expect, test, type APIRequestContext } from '@playwright/test'

import { isBackendReady } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  ensureDemoFullFlowPublished,
  fetchDemoFullFlowApiPolicy,
} from './helpers/content-modules-api'
import {
  editOutputPolicyCandidate,
  expandApiPolicyAdvancedSettings,
  saveApiOutputPolicyFromHubTab,
} from './helpers/lifecycle-ui'

async function resolveReachableFrontendBaseUrl(request: APIRequestContext): Promise<string | null> {
  const candidates = [
    process.env.E2E_BASE_URL,
    'http://127.0.0.1:4173',
    'http://127.0.0.1:5173',
  ].filter((value, index, array): value is string => Boolean(value) && array.indexOf(value) === index)

  for (const baseUrl of candidates) {
    try {
      if ((await request.get(baseUrl, { timeout: 5_000 })).ok()) {
        return baseUrl
      }
    } catch {
      // try next candidate
    }
  }
  return null
}

test.describe('CDP-E2E-T07 API policy edit save (BDD-CDP-APIPOL-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    const backendReady = await isBackendReady(request)
    const frontendBaseUrl = await resolveReachableFrontendBaseUrl(request)
    test.skip(
      !(backendReady && frontendBaseUrl),
      'Stack required (frontend + backend :8080). Start with .\\scripts\\docker-deploy.ps1 or pnpm dev.',
    )
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
  })

  test('BDD-CDP-APIPOL-001 — edit output policy, impact preview, save increments policyVersion', async ({
    page,
    request,
  }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    const policyBefore = await fetchDemoFullFlowApiPolicy(request, fixture.templateId)

    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByRole('tab', { name: /external access|对外接入/i })).toBeVisible()

    await expandApiPolicyAdvancedSettings(page)
    await editOutputPolicyCandidate(
      page,
      policyBefore.outputFormats,
      policyBefore.outputModes ?? [],
    )
    await saveApiOutputPolicyFromHubTab(page)

    const policyAfterSave = await fetchDemoFullFlowApiPolicy(request, fixture.templateId)
    expect(policyAfterSave.policyVersion).toBeGreaterThan(policyBefore.policyVersion)

    await page.reload()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(
      page.getByText(new RegExp(`settings version\\s+v${policyAfterSave.policyVersion}`, 'i')),
    ).toBeVisible()
    await expect(page.getByRole('button', { name: /^activity log$/i })).toBeVisible()
  })
})
