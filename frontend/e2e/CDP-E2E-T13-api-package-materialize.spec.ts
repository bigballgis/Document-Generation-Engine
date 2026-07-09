import { expect, test, type APIRequestContext } from '@playwright/test'

import { isBackendReady } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  createIsolatedTemplatePendingRelease,
  fetchDemoFullFlowApiPolicy,
  fetchManagementCallerContract,
  fetchManagementRoutesSummary,
  publishSecondReleaseFromClone,
  publishTemplateRelease,
} from './helpers/content-modules-api'
import { saveDefaultRouteFromHubTab } from './helpers/lifecycle-ui'

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

test.describe('CDP-E2E-T13 API package materialize (BDD S1–S3)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let templateId = ''
  let externalId = ''

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

  test('BDD S1 — first publish materializes policy and dual generate paths', async ({ page, request }) => {
    const fixture = await createIsolatedTemplatePendingRelease(request)
    templateId = fixture.templateId
    externalId = fixture.externalId

    const policyBeforePublish = await fetchDemoFullFlowApiPolicy(request, templateId)
    expect(policyBeforePublish.defaultRouteReleaseVersion).toBeFalsy()

    await publishTemplateRelease(request, templateId, '1.0.0')

    const policy = await fetchDemoFullFlowApiPolicy(request, templateId)
    expect(policy.defaultRouteReleaseVersion).toBe('1.0.0')

    const routes = await fetchManagementRoutesSummary(request, templateId)
    expect(routes.templateExternalId).toBe(externalId)
    expect(routes.defaultGeneratePath).toMatch(/\/generate/i)
    expect(routes.explicitPaths.some((path) => path.releaseVersion === '1.0.0')).toBe(true)

    const contract = await fetchManagementCallerContract(request, templateId)
    expect(contract.paths.some((path) => /\/default\/generate/i.test(path))).toBe(true)
    expect(
      contract.callableVersions.some(
        (version) =>
          version.releaseVersion === '1.0.0' && /\/versions\/1\.0\.0/i.test(version.explicitVersionUrl),
      ),
    ).toBe(true)

    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const routeSummary = page.getByTestId('route-summary-panel')
    await expect(routeSummary).toBeVisible()
    await expect(routeSummary.locator('.path-value')).toContainText(/\/generate/i)
    await expect(page.getByText(/api not configured|未配置 api/i)).toHaveCount(0)
  })

  test('BDD S2 — second publish keeps default route on first release', async ({ request }) => {
    test.skip(!templateId, 'Depends on S1 fixture')

    await publishSecondReleaseFromClone(request, templateId, '1.0.0', '2.0.0')

    const policy = await fetchDemoFullFlowApiPolicy(request, templateId)
    expect(policy.defaultRouteReleaseVersion).toBe('1.0.0')

    const routes = await fetchManagementRoutesSummary(request, templateId)
    expect(routes.defaultRouteReleaseVersion).toBe('1.0.0')
    expect(routes.explicitPaths.some((path) => path.releaseVersion === '2.0.0')).toBe(true)

    const contract = await fetchManagementCallerContract(request, templateId)
    expect(
      contract.callableVersions.some((version) => version.releaseVersion === '2.0.0'),
    ).toBe(true)
  })

  test('BDD S3 — explicit default route change uses impact preview and bumps policyVersion', async ({
    page,
    request,
  }) => {
    test.skip(!templateId, 'Depends on S1 fixture')

    const policyBefore = await fetchDemoFullFlowApiPolicy(request, templateId)
    expect(policyBefore.defaultRouteReleaseVersion).toBe('1.0.0')

    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await saveDefaultRouteFromHubTab(page, '2.0.0')

    const policyAfter = await fetchDemoFullFlowApiPolicy(request, templateId)
    expect(policyAfter.defaultRouteReleaseVersion).toBe('2.0.0')
    expect(policyAfter.policyVersion).toBeGreaterThan(policyBefore.policyVersion)

    await page.reload()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.locator('#policy-domain-DEFAULT_ROUTE_TARGET')).toContainText('2.0.0')
  })
})
