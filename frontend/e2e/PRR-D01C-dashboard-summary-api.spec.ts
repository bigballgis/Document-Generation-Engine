/**
 * PRR-D01c / Task Master #136 — Dashboard summary API (bounded Overview first paint).
 *
 * BDD: docs/behavior/prod-dashboard-summary-api.md (BDD-PRR-D01C-*)
 *
 * Canonical run (Docker acceptance :4173 / :8080):
 *   pnpm -C frontend exec playwright test e2e/PRR-D01C-dashboard-summary-api.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Evidence: frontend/e2e/evidence/PRR-D01C-manifest.md
 *           frontend/e2e/evidence/PRR-D01C-network.json
 */
import { expect, test, type APIRequestContext, type Page, type Request } from '@playwright/test'
import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { E2E_ADMIN, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'

type DashboardSummaryBuckets = {
  catalogMasters: number
  catalogTemplates: number
  publishedVersions: number
  templateVersionsInWorkflow: number
  masterPendingReview: number
  masterVersionsInProgress: number
  stoppedVersions: number
}

async function fetchDashboardSummaryViaApi(
  request: APIRequestContext,
  credentials: { username: string; password: string },
): Promise<DashboardSummaryBuckets> {
  const loginResponse = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: credentials,
  })
  expect(loginResponse.ok(), `API login failed: ${loginResponse.status()}`).toBeTruthy()
  const loginBody = (await loginResponse.json()) as { result: { accessToken: string } }
  const token = loginBody.result.accessToken

  const summaryResponse = await request.get(`${E2E_API_BASE_URL}/dashboard/summary`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(summaryResponse.ok(), `summary API ${summaryResponse.status()}`).toBeTruthy()
  const body = (await summaryResponse.json()) as { result: DashboardSummaryBuckets }
  return body.result
}

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EVIDENCE_DIR = path.join(path.dirname(fileURLToPath(import.meta.url)), 'evidence')
const NETWORK_EVIDENCE_PATH = path.join(EVIDENCE_DIR, 'PRR-D01C-network.json')

type CapturedGet = { url: string; resource: 'summary' | 'masters' | 'templates' | 'other' }

function pathnameOf(url: string): string {
  try {
    return new URL(url).pathname.replace(/\/$/, '')
  } catch {
    return url
  }
}

function queryParams(url: string): URLSearchParams {
  try {
    return new URL(url).searchParams
  } catch {
    return new URLSearchParams()
  }
}

function isManagementListGet(url: string, resource: 'masters' | 'templates'): boolean {
  return pathnameOf(url) === `/api/management/v1/${resource}`
}

function isDashboardSummaryGet(url: string): boolean {
  return pathnameOf(url) === '/api/management/v1/dashboard/summary'
}

/**
 * Unbounded catalog fetch-all for Overview stats: list GET without a status /
 * lifecycleStatus filter (status-filtered multi-page collect is allowed for
 * master workflow todos — D01C-C6).
 */
function isUnboundedCatalogListGet(url: string): boolean {
  if (isManagementListGet(url, 'masters')) {
    return !queryParams(url).has('status')
  }
  if (isManagementListGet(url, 'templates')) {
    return !queryParams(url).has('lifecycleStatus')
  }
  return false
}

function classifyGet(url: string): CapturedGet['resource'] {
  if (isDashboardSummaryGet(url)) {
    return 'summary'
  }
  if (isManagementListGet(url, 'masters')) {
    return 'masters'
  }
  if (isManagementListGet(url, 'templates')) {
    return 'templates'
  }
  return 'other'
}

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

function attachGetCapture(page: Page, bucket: CapturedGet[]) {
  const onRequest = (request: Request) => {
    if (request.method() !== 'GET') {
      return
    }
    const url = request.url()
    if (!url.includes('/api/management/v1/')) {
      return
    }
    bucket.push({ url, resource: classifyGet(url) })
  }
  page.on('request', onRequest)
  return () => page.off('request', onRequest)
}

function writeNetworkEvidence(payload: Record<string, unknown>) {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  writeFileSync(NETWORK_EVIDENCE_PATH, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

test.describe('PRR-D01c dashboard summary API (BDD-PRR-D01C)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-PRR-D01C-001/010 — Overview calls summary; no unbounded catalog fetch-all; stats render', async ({
    page,
    request,
  }) => {
    const expected = await fetchDashboardSummaryViaApi(request, E2E_ADMIN)
    expect(expected.catalogMasters).toBeGreaterThanOrEqual(0)
    expect(expected.catalogTemplates).toBeGreaterThanOrEqual(0)

    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')
    await dismissOnboardingTourIfPresent(page)

    const captured: CapturedGet[] = []
    const detach = attachGetCapture(page, captured)

    const summaryResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        isDashboardSummaryGet(response.url()) &&
        response.ok(),
      { timeout: 30_000 },
    )

    // Reload so network capture covers a full Overview first-paint after login.
    await page.reload({ waitUntil: 'domcontentloaded' })
    await dismissOnboardingTourIfPresent(page)
    const summaryResponse = await summaryResponsePromise
    expect(summaryResponse.ok()).toBeTruthy()

    await expect(page.getByRole('tab', { name: /^overview$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page.getByRole('heading', { name: /catalog & workflow snapshot/i })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.dashboard-stats .el-skeleton')).toHaveCount(0, { timeout: 60_000 })

    const statsSection = page.locator('.dashboard-stats')
    await expect(statsSection.getByRole('heading', { name: /letterheads in catalog/i })).toBeVisible()
    await expect(statsSection.getByRole('heading', { name: /templates in catalog/i })).toBeVisible()
    await expect(statsSection.locator('.stat-count').first()).toBeVisible()

    const catalogMasterCard = statsSection.locator('.stat-card').filter({
      has: page.getByRole('heading', { name: /letterheads in catalog/i }),
    })
    const catalogTemplateCard = statsSection.locator('.stat-card').filter({
      has: page.getByRole('heading', { name: /templates in catalog/i }),
    })
    await expect(catalogMasterCard.locator('.stat-count')).toHaveText(
      String(expected.catalogMasters),
    )
    await expect(catalogTemplateCard.locator('.stat-count')).toHaveText(
      String(expected.catalogTemplates),
    )

    await expect.poll(() => captured.filter((c) => c.resource === 'summary').length).toBeGreaterThan(0)

    const unbounded = captured.filter((c) => isUnboundedCatalogListGet(c.url))
    const summaryUrls = captured.filter((c) => c.resource === 'summary').map((c) => c.url)
    const mastersListUrls = captured.filter((c) => c.resource === 'masters').map((c) => c.url)
    const templatesListUrls = captured.filter((c) => c.resource === 'templates').map((c) => c.url)

    writeNetworkEvidence({
      slice: 'prod-dashboard-summary-api',
      task: 'PRR-D01c #136',
      bdd: ['BDD-PRR-D01C-001', 'BDD-PRR-D01C-010'],
      capturedAt: new Date().toISOString(),
      summaryUrls,
      mastersListUrls,
      templatesListUrls,
      unboundedCatalogListUrls: unbounded.map((u) => u.url),
      summaryBuckets: expected,
    })

    expect(
      unbounded,
      `Overview must not issue unfiltered masters/templates list GETs (fetch-all). Saw:\n${unbounded
        .map((u) => u.url)
        .join('\n')}`,
    ).toEqual([])

    // Status-filtered masters listAll for workflow todos (D01C-C6) may appear for GLOBAL_ADMIN;
    // templates catalog list must not appear on Overview first paint.
    expect(templatesListUrls, 'Overview must not list /templates catalog pages').toEqual([])

    detach()
  })

  test('BDD-PRR-D01C-006 smoke — Dashboard Tasks tab loads without Overview fetch-all regression', async ({
    page,
  }) => {
    const captured: CapturedGet[] = []
    const detach = attachGetCapture(page, captured)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await dismissOnboardingTourIfPresent(page)

    const tasks = page.locator('#tasks-section')
    await expect(tasks).toBeVisible({ timeout: 30_000 })
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /waiting on my testing/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(tasks.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
    await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()

    // Tasks path must not reintroduce unfiltered catalog fetch-all.
    const unbounded = captured.filter((c) => isUnboundedCatalogListGet(c.url))
    expect(
      unbounded,
      `Tasks entry must not issue unfiltered masters/templates list GETs. Saw:\n${unbounded
        .map((u) => u.url)
        .join('\n')}`,
    ).toEqual([])

    detach()
  })
})
