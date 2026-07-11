/**
 * LR-C5 — Catalog server-side pagination / filter (BDD-LRP-C5-001…015 subset).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C5-catalog-pagination.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080 with ≥500 LOAD-TPL-* seed
 * (stage 5 DEPLOY_OK). Perf evidence written to:
 *   frontend/e2e/evidence/LRP-C5-list-latency.json
 */
import { expect, test, type Page, type Response } from '@playwright/test'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { requireDockerStack } from './helpers/stack-readiness'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EVIDENCE_DIR = path.join(path.dirname(fileURLToPath(import.meta.url)), 'evidence')
const LATENCY_EVIDENCE_PATH = path.join(EVIDENCE_DIR, 'LRP-C5-list-latency.json')

/** Wall-clock timings (ms) for catalog list GETs observed during this file. */
const listLatenciesMs: Array<{ url: string; durationMs: number; totalElements?: number }> = []

function pathnameOf(url: string): string {
  try {
    return new URL(url).pathname.replace(/\/$/, '')
  } catch {
    return url
  }
}

function isCatalogListGet(url: string, resource: 'templates' | 'masters' | 'content-modules'): boolean {
  const pathname = pathnameOf(url)
  return pathname === `/api/management/v1/${resource}`
}

function queryParams(url: string): URLSearchParams {
  try {
    return new URL(url).searchParams
  } catch {
    return new URLSearchParams()
  }
}

function assertPagedListUrl(url: string, resource: 'templates' | 'masters' | 'content-modules') {
  expect(isCatalogListGet(url, resource), `expected ${resource} list URL, got ${url}`).toBe(true)
  const params = queryParams(url)
  expect(params.has('page'), `missing page= on ${url}`).toBe(true)
  expect(params.has('size'), `missing size= on ${url}`).toBe(true)
  const page = Number(params.get('page'))
  const size = Number(params.get('size'))
  expect(Number.isInteger(page) && page >= 0, `invalid page on ${url}`).toBe(true)
  expect(Number.isInteger(size) && size >= 1 && size <= 100, `invalid size on ${url}`).toBe(true)
}

async function waitForCatalogList(
  page: Page,
  resource: 'templates' | 'masters' | 'content-modules',
  predicate?: (url: string) => boolean,
): Promise<Response> {
  return page.waitForResponse(
    (response) => {
      if (response.request().method() !== 'GET') {
        return false
      }
      const url = response.url()
      if (!isCatalogListGet(url, resource)) {
        return false
      }
      if (predicate && !predicate(url)) {
        return false
      }
      return response.ok()
    },
    { timeout: 30_000 },
  )
}

async function recordListLatency(
  response: Response,
  wallClockStartMs?: number,
): Promise<{
  url: string
  durationMs: number
  totalElements?: number
  page?: number
  size?: number
  contentLength?: number
}> {
  const timing = response.request().timing()
  const playwrightMs =
    timing.responseEnd >= 0
      ? timing.responseEnd
      : timing.responseStart >= 0
        ? timing.responseStart
        : -1
  const wallMs =
    typeof wallClockStartMs === 'number' && wallClockStartMs > 0
      ? Math.max(0, Date.now() - wallClockStartMs)
      : -1
  // Prefer Playwright resource timing; fall back to wall clock from request start.
  const durationMs = playwrightMs >= 0 ? playwrightMs : wallMs >= 0 ? wallMs : 0
  let totalElements: number | undefined
  let page: number | undefined
  let size: number | undefined
  let contentLength: number | undefined
  try {
    const body = (await response.json()) as {
      result?: {
        totalElements?: number
        page?: number
        size?: number
        content?: unknown[]
      }
    }
    totalElements = body.result?.totalElements
    page = body.result?.page
    size = body.result?.size
    contentLength = body.result?.content?.length
  } catch {
    // Non-JSON or aborted — still record URL timing.
  }
  const entry = { url: response.url(), durationMs, totalElements }
  listLatenciesMs.push(entry)
  return { ...entry, page, size, contentLength }
}

async function waitForCatalogListTimed(
  page: Page,
  resource: 'templates' | 'masters' | 'content-modules',
  predicate?: (url: string) => boolean,
): Promise<{ response: Response; startedAt: number }> {
  let startedAt = 0
  const onRequest = (request: { method: () => string; url: () => string }) => {
    if (request.method() !== 'GET') {
      return
    }
    const url = request.url()
    if (!isCatalogListGet(url, resource)) {
      return
    }
    if (predicate && !predicate(url)) {
      return
    }
    startedAt = Date.now()
  }
  page.on('request', onRequest)
  try {
    const response = await waitForCatalogList(page, resource, predicate)
    return { response, startedAt }
  } finally {
    page.off('request', onRequest)
  }
}

function percentile(sortedAsc: number[], p: number): number {
  if (sortedAsc.length === 0) {
    return Number.NaN
  }
  const idx = Math.min(sortedAsc.length - 1, Math.ceil((p / 100) * sortedAsc.length) - 1)
  return sortedAsc[Math.max(0, idx)]!
}

function writeLatencyEvidence() {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  let prior: typeof listLatenciesMs = []
  try {
    const raw = JSON.parse(readFileSync(LATENCY_EVIDENCE_PATH, 'utf8')) as {
      latenciesMs?: typeof listLatenciesMs
    }
    prior = raw.latenciesMs ?? []
  } catch {
    prior = []
  }
  // Merge prior file samples with this worker's in-memory samples (dedupe by url+duration).
  const merged = [...prior]
  for (const sample of listLatenciesMs) {
    const exists = merged.some(
      (p) => p.url === sample.url && Math.abs(p.durationMs - sample.durationMs) < 0.01,
    )
    if (!exists) {
      merged.push(sample)
    }
  }
  const durations = merged.map((e) => e.durationMs).sort((a, b) => a - b)
  const p95 = percentile(durations, 95)
  const payload = {
    slice: 'LR-C5',
    bdd: 'BDD-LRP-C5-001 / C5-C11',
    capturedAt: new Date().toISOString(),
    sampleCount: durations.length,
    latenciesMs: merged,
    summary: {
      minMs: durations[0] ?? null,
      maxMs: durations.at(-1) ?? null,
      avgMs:
        durations.length === 0
          ? null
          : Math.round((durations.reduce((a, b) => a + b, 0) / durations.length) * 10) / 10,
      p95Ms: Number.isFinite(p95) ? Math.round(p95 * 10) / 10 : null,
      gateMs: 1000,
      sampleCount: durations.length,
      pass: Number.isFinite(p95) && p95 < 1000,
    },
  }
  writeFileSync(LATENCY_EVIDENCE_PATH, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return payload
}

async function tableGroupCodes(page: Page): Promise<string[]> {
  const cells = page.locator('.el-table__body-wrapper tbody tr td:nth-child(1)')
  const count = await cells.count()
  const codes: string[] = []
  for (let i = 0; i < count; i += 1) {
    const text = (await cells.nth(i).innerText()).trim()
    if (text) {
      codes.push(text)
    }
  }
  return codes
}

async function tableRowIds(page: Page): Promise<string[]> {
  // Entity link href embeds the entity id for templates/masters/modules.
  const links = page.locator('.el-table__body-wrapper tbody tr a[href]')
  const count = await links.count()
  const ids: string[] = []
  for (let i = 0; i < count; i += 1) {
    const href = (await links.nth(i).getAttribute('href')) ?? ''
    const match = href.match(/\/(templates|masters|content-modules)\/([^/?#]+)/)
    if (match?.[2]) {
      ids.push(match[2])
    }
  }
  return ids
}

function assertGroupFirstOrder(groupCodes: string[]) {
  expect(groupCodes.length, 'expected visible catalog rows').toBeGreaterThan(0)
  for (let i = 1; i < groupCodes.length; i += 1) {
    expect(
      groupCodes[i]!.localeCompare(groupCodes[i - 1]!),
      `group-first broken at index ${i}: ${groupCodes.join(',')}`,
    ).toBeGreaterThanOrEqual(0)
  }
}

test.describe('LRP-C5 catalog server-side pagination/filter', () => {
  test.beforeEach(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL })
  })

  test.afterAll(() => {
    const evidence = writeLatencyEvidence()
    expect(
      evidence.sampleCount,
      'expected at least one catalog list latency sample',
    ).toBeGreaterThan(0)
    expect(
      evidence.summary.pass,
      `p95 list latency ${evidence.summary.p95Ms}ms must be < 1000ms (see ${LATENCY_EVIDENCE_PATH})`,
    ).toBe(true)
  })

  test('BDD-LRP-C5-001/004: templates first page is server PageView + group-first + paged network', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)

    const listPromise = waitForCatalogListTimed(page, 'templates', (url) => {
      const params = queryParams(url)
      return params.get('page') === '0'
    })
    await page.goto('/templates')
    const { response: listResponse, startedAt } = await listPromise
    const recorded = await recordListLatency(listResponse, startedAt)

    assertPagedListUrl(listResponse.url(), 'templates')
    const params = queryParams(listResponse.url())
    expect(params.get('page')).toBe('0')
    expect(Number(params.get('size'))).toBeGreaterThanOrEqual(1)
    expect(Number(params.get('size'))).toBeLessThanOrEqual(100)
    // Default sort is group-first (explicit or omitted → server default).
    const sort = params.get('sort')
    if (sort) {
      expect(sort).toBe('groupCodeAsc')
    }

    expect(recorded.totalElements ?? 0).toBeGreaterThanOrEqual(500)
    expect(recorded.contentLength ?? 0).toBeGreaterThan(0)
    expect(recorded.contentLength ?? 0).toBeLessThanOrEqual(recorded.size ?? 20)
    expect(recorded.durationMs).toBeLessThan(1000)

    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 20_000 })
    await expect(page.locator('.catalog-filter-toolbar')).toBeVisible()
    await expect(page.locator('.list-pagination, .el-pagination').first()).toBeVisible()

    const rowCount = await page.locator('.el-table__body-wrapper tbody tr').count()
    expect(rowCount).toBeGreaterThan(0)
    expect(rowCount).toBeLessThanOrEqual(recorded.size ?? 20)

    assertGroupFirstOrder(await tableGroupCodes(page))

    // Primary path must not issue an unpaged templates list GET.
    const unpaged = listLatenciesMs.filter(
      (e) => isCatalogListGet(e.url, 'templates') && !queryParams(e.url).has('page'),
    )
    expect(unpaged, 'unpaged templates list must not be the primary path').toHaveLength(0)
  })

  test('BDD-LRP-C5-002: templates next page issues page=N server request without id overlap', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)

    const firstPromise = waitForCatalogList(page, 'templates', (url) => queryParams(url).get('page') === '0')
    await page.goto('/templates')
    const first = await firstPromise
    await recordListLatency(first)
    assertPagedListUrl(first.url(), 'templates')

    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 20_000 })
    const page0Ids = await tableRowIds(page)
    expect(page0Ids.length).toBeGreaterThan(0)

    const nextPromise = waitForCatalogList(page, 'templates', (url) => {
      const pageParam = Number(queryParams(url).get('page'))
      return pageParam >= 1
    })
    await page.locator('.list-pagination button.btn-next, .el-pagination button.btn-next').first().click()
    const next = await nextPromise
    const recorded = await recordListLatency(next)
    assertPagedListUrl(next.url(), 'templates')
    expect(Number(queryParams(next.url()).get('page'))).toBeGreaterThanOrEqual(1)
    expect(recorded.contentLength ?? 0).toBeGreaterThan(0)

    await expect
      .poll(async () => (await tableRowIds(page)).join(','), { timeout: 15_000 })
      .not.toBe(page0Ids.join(','))

    const page1Ids = await tableRowIds(page)
    const overlap = page1Ids.filter((id) => page0Ids.includes(id))
    expect(overlap, `page overlap ids: ${overlap.join(',')}`).toHaveLength(0)
  })

  test('BDD-LRP-C5-003/005/010: search/group filter + page change stay server-paged; filter resets page', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)

    const firstPromise = waitForCatalogList(page, 'templates', (url) => queryParams(url).get('page') === '0')
    await page.goto('/templates')
    await recordListLatency(await firstPromise)
    await expect(page.locator('.catalog-filter-toolbar')).toBeVisible({ timeout: 20_000 })

    // Move off page 0 so filter reset can be observed.
    const page2Promise = waitForCatalogList(page, 'templates', (url) => Number(queryParams(url).get('page')) >= 1)
    await page.locator('.list-pagination button.btn-next, .el-pagination button.btn-next').first().click()
    await recordListLatency(await page2Promise)

    const groupFilterPromise = waitForCatalogList(page, 'templates', (url) => {
      const params = queryParams(url)
      return params.get('groupCode') === 'CORP' && params.get('page') === '0'
    })
    const groupInput = page.locator('.catalog-filter-toolbar__control input').first()
    await groupInput.fill('CORP')
    const filtered = await groupFilterPromise
    const filteredRecord = await recordListLatency(filtered)
    assertPagedListUrl(filtered.url(), 'templates')
    expect(queryParams(filtered.url()).get('groupCode')).toBe('CORP')
    expect(queryParams(filtered.url()).get('page')).toBe('0')
    expect(filteredRecord.totalElements ?? 0).toBeGreaterThan(0)
    expect(filteredRecord.totalElements ?? 0).toBeLessThan(515)

    const codes = await tableGroupCodes(page)
    expect(codes.every((c) => c === 'CORP')).toBe(true)

    // Search still server-paged.
    const searchPromise = waitForCatalogList(page, 'templates', (url) => {
      const params = queryParams(url)
      return (params.get('search') ?? '').includes('LOAD-TPL') && params.has('page') && params.has('size')
    })
    await page.locator('.catalog-filter-toolbar__search input').fill('LOAD-TPL')
    const searchResponse = await searchPromise
    await recordListLatency(searchResponse)
    assertPagedListUrl(searchResponse.url(), 'templates')
    expect(queryParams(searchResponse.url()).get('page')).toBe('0')

    // If filtered set still spans pages, next page keeps filter params.
    const total = filteredRecord.totalElements ?? 0
    const size = Number(queryParams(filtered.url()).get('size') || '20')
    if (total > size) {
      // Clear search so we can page within group filter alone (search may shrink to ≤1 page).
      const clearSearchPromise = waitForCatalogList(page, 'templates', (url) => {
        const params = queryParams(url)
        return params.get('groupCode') === 'CORP' && !params.get('search') && params.get('page') === '0'
      })
      await page.locator('.catalog-filter-toolbar__search input').fill('')
      await recordListLatency(await clearSearchPromise)

      const nextFilteredPromise = waitForCatalogList(page, 'templates', (url) => {
        const params = queryParams(url)
        return params.get('groupCode') === 'CORP' && Number(params.get('page')) >= 1
      })
      await page.locator('.list-pagination button.btn-next, .el-pagination button.btn-next').first().click()
      const nextFiltered = await nextFilteredPromise
      await recordListLatency(nextFiltered)
      assertPagedListUrl(nextFiltered.url(), 'templates')
      expect(queryParams(nextFiltered.url()).get('groupCode')).toBe('CORP')
      const nextCodes = await tableGroupCodes(page)
      expect(nextCodes.every((c) => c === 'CORP')).toBe(true)
    }
  })

  test('BDD-LRP-C5-006: awaitingApproval chip maps to lifecycleStatus+approvalSubState on paged request', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)

    const firstPromise = waitForCatalogList(page, 'templates')
    await page.goto('/templates')
    await recordListLatency(await firstPromise)
    await expect(page.locator('.workflow-filters')).toBeVisible({ timeout: 20_000 })

    const chipPromise = waitForCatalogList(page, 'templates', (url) => {
      const params = queryParams(url)
      return (
        params.get('lifecycleStatus') === 'APPROVAL' &&
        params.get('approvalSubState') === 'PENDING_DECISION' &&
        params.has('page') &&
        params.has('size')
      )
    })
    await page
      .locator('.workflow-filters .el-check-tag')
      .filter({ hasText: /awaiting my approval/i })
      .click()
    const chipResponse = await chipPromise
    await recordListLatency(chipResponse)
    assertPagedListUrl(chipResponse.url(), 'templates')
    expect(queryParams(chipResponse.url()).get('page')).toBe('0')
  })

  test('BDD-LRP-C5-007: masters catalog uses server PageView pagination', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)

    const listPromise = waitForCatalogList(page, 'masters', (url) => queryParams(url).get('page') === '0')
    await page.goto('/masters')
    const listResponse = await listPromise
    const recorded = await recordListLatency(listResponse)
    assertPagedListUrl(listResponse.url(), 'masters')

    expect(recorded.totalElements ?? 0).toBeGreaterThan(20)
    expect(recorded.contentLength ?? 0).toBeLessThanOrEqual(recorded.size ?? 20)

    await expect(page.getByRole('heading', { name: /^letterhead templates$/i })).toBeVisible()
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 20_000 })
    await expect(page.locator('.list-pagination, .el-pagination').first()).toBeVisible()

    const page0Ids = await tableRowIds(page)
    const nextPromise = waitForCatalogList(page, 'masters', (url) => Number(queryParams(url).get('page')) >= 1)
    await page.locator('.list-pagination button.btn-next, .el-pagination button.btn-next').first().click()
    const next = await nextPromise
    await recordListLatency(next)
    assertPagedListUrl(next.url(), 'masters')

    await expect
      .poll(async () => (await tableRowIds(page)).join(','), { timeout: 15_000 })
      .not.toBe(page0Ids.join(','))
  })

  test('BDD-LRP-C5-008: content-modules catalog uses server PageView pagination', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)

    const listPromise = waitForCatalogList(
      page,
      'content-modules',
      (url) => queryParams(url).get('page') === '0',
    )
    await page.goto('/content-modules')
    const listResponse = await listPromise
    const recorded = await recordListLatency(listResponse)
    assertPagedListUrl(listResponse.url(), 'content-modules')

    expect(recorded.totalElements ?? 0).toBeGreaterThan(20)
    expect(recorded.contentLength ?? 0).toBeLessThanOrEqual(recorded.size ?? 20)

    await expect(page.getByRole('heading', { name: /^standard clauses$/i })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 20_000 })
    await expect(page.locator('.list-pagination, .el-pagination').first()).toBeVisible()

    const nextPromise = waitForCatalogList(
      page,
      'content-modules',
      (url) => Number(queryParams(url).get('page')) >= 1,
    )
    await page.locator('.list-pagination button.btn-next, .el-pagination button.btn-next').first().click()
    const next = await nextPromise
    await recordListLatency(next)
    assertPagedListUrl(next.url(), 'content-modules')
  })
})
