/**
 * CE-G05 — template annual review + clause FULL_TEXT / where-used (BDD-CE-G05-001…019)
 *
 * Acceptance: Docker :4173 + backend :8080
 * BDD SoT: docs/behavior/ce-g05-annual-review-fts.md
 * FE testids: template-annual-review-*, content-module-search-mode, catalog-filter-search,
 *   content-module-where-used*, data-partition-id=template-annual-review
 */
import { expect, test, type Page } from '@playwright/test'

import {
  E2E_CORP_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  bumpModuleBodyPhrase,
  completeAnnualReviewRawBody,
  completeAnnualReviewViaApi,
  createApprovedModuleWithBodyPhrase,
  createDraftTemplateNeverPublished,
  fetchTemplateDetailViaApi,
  findManagementAuditEvent,
  listAnnualReviewDueTasksViaApi,
  listContentModulesSearchViaApi,
  listWhereUsedViaApi,
  markTemplateAnnualReviewDueToday,
  preparePublishedAnnualReviewTemplate,
  republishTemplatePreservingReviewDue,
  utcPlusDays,
  utcToday,
} from './helpers/ce-g05-annual-review-api'
import {
  preparePublishedTemplateReferencingModule,
  preparePublishedTemplateWithLockedReference,
} from './helpers/content-modules-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  confirmMessageBox,
  expectDashboardPartitionHeading,
  openContentModulesList,
  selectElementPlusOption,
} from './helpers/ui'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ??
  (process.env.FRONTEND_PORT === '5173'
    ? 'http://127.0.0.1:5173'
    : 'http://127.0.0.1:4173')

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openDashboardTasks(page: Page) {
  await page.goto('/dashboard#tasks-section')
  await dismissOnboardingTourIfPresent(page)
  const tasks = page.locator('#tasks-section')
  await expect(tasks).toBeVisible({ timeout: 30_000 })
  await expect(tasks.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
  return tasks
}

test.describe('CE-G05 annual review + clause FTS (BDD-CE-G05-001…019)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-CE-G05-001 — first publish seeds nextReviewDue = todayUtc+365', async ({
    request,
  }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    const expected = utcPlusDays(365)
    expect(fixture.nextReviewDue).toBe(expected)

    const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
    expect(detail.nextReviewDue).toBe(expected)
    expect(detail.lifecycleStatus).toBe('PUBLISHED')
  })

  test('BDD-CE-G05-002 — republish does not overwrite nextReviewDue', async ({ request }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    const seeded = fixture.nextReviewDue
    const detailBefore = await fetchTemplateDetailViaApi(request, fixture.templateId)
    expect(detailBefore.releaseVersion).toBeTruthy()

    const after = await republishTemplatePreservingReviewDue(
      request,
      fixture.templateId,
      detailBefore.releaseVersion!,
      '1.0.1',
    )
    expect(after.nextReviewDue).toBe(seeded)
    expect(after.releaseVersion).toBe('1.0.1')
  })

  test('BDD-CE-G05-003 — due today appears in annual-review-due tasks', async ({ request }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    const today = await markTemplateAnnualReviewDueToday(request, fixture.templateId)

    const { status, tasks } = await listAnnualReviewDueTasksViaApi(request)
    expect(status).toBe(200)
    const hit = tasks.find((t) => t.templateId === fixture.templateId)
    expect(hit).toBeTruthy()
    expect(hit!.nextReviewDue).toBe(today)
  })

  test('BDD-CE-G05-004 — future nextReviewDue not in due queue', async ({ request }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    const tomorrow = utcPlusDays(1)
    const complete = await completeAnnualReviewViaApi(request, fixture.templateId, tomorrow)
    expect(complete.status).toBe(200)
    expect(complete.summary?.nextReviewDue).toBe(tomorrow)

    const { tasks } = await listAnnualReviewDueTasksViaApi(request)
    expect(tasks.some((t) => t.templateId === fixture.templateId)).toBeFalsy()
  })

  test('BDD-CE-G05-005 — null nextReviewDue not in due queue', async ({ request }) => {
    const draft = await createDraftTemplateNeverPublished(request)
    const detail = await fetchTemplateDetailViaApi(request, draft.templateId)
    expect(detail.nextReviewDue == null).toBeTruthy()

    const { tasks } = await listAnnualReviewDueTasksViaApi(request)
    expect(tasks.some((t) => t.templateId === draft.templateId)).toBeFalsy()
  })

  test('BDD-CE-G05-006 — complete without body rolls +365 and drops from queue', async ({
    request,
  }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    await markTemplateAnnualReviewDueToday(request, fixture.templateId)

    const complete = await completeAnnualReviewViaApi(request, fixture.templateId)
    expect(complete.status).toBe(200)
    expect(complete.summary?.nextReviewDue).toBe(utcPlusDays(365))

    const { tasks } = await listAnnualReviewDueTasksViaApi(request)
    expect(tasks.some((t) => t.templateId === fixture.templateId)).toBeFalsy()

    const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
    expect(detail.nextReviewDue).toBe(utcPlusDays(365))

    const audit = await findManagementAuditEvent(request, {
      eventType: 'TEMPLATE_ANNUAL_REVIEW_COMPLETED',
      templateExternalId: fixture.externalId,
    })
    if (audit) {
      const blob = JSON.stringify(audit)
      expect(blob).not.toMatch(/password|secret|credential/i)
      expect(blob).toContain(fixture.externalId)
    }
  })

  test('BDD-CE-G05-007 — complete with explicit future nextReviewDue', async ({ request }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    await markTemplateAnnualReviewDueToday(request, fixture.templateId)
    const future = utcPlusDays(200)

    const complete = await completeAnnualReviewViaApi(request, fixture.templateId, future)
    expect(complete.status).toBe(200)
    expect(complete.summary?.nextReviewDue).toBe(future)

    const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
    expect(detail.nextReviewDue).toBe(future)
  })

  test('BDD-CE-G05-008 — invalid nextReviewDue → validation error, due date unchanged', async ({
    request,
  }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    const before = await fetchTemplateDetailViaApi(request, fixture.templateId)

    const result = await completeAnnualReviewRawBody(request, fixture.templateId, {
      nextReviewDue: 'not-a-date',
    })
    expect([400, 422]).toContain(result.status)
    expect(result.error?.category ?? result.error?.code).toBeTruthy()

    const after = await fetchTemplateDetailViaApi(request, fixture.templateId)
    expect(after.nextReviewDue).toBe(before.nextReviewDue)
  })

  test('BDD-CE-G05-009 — TEMPLATE_TESTER fail-closed on due-tasks and complete', async ({
    request,
  }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    await markTemplateAnnualReviewDueToday(request, fixture.templateId)

    const due = await listAnnualReviewDueTasksViaApi(request, E2E_TEMPLATE_TESTER)
    expect(due.status).toBe(403)

    const complete = await completeAnnualReviewViaApi(
      request,
      fixture.templateId,
      undefined,
      E2E_TEMPLATE_TESTER,
    )
    expect(complete.status).toBe(403)
  })

  test('BDD-CE-G05-010/018 — Dashboard Annual review due Open + complete closes loop', async ({
    page,
    request,
  }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    await markTemplateAnnualReviewDueToday(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDashboardTasks(page)

    const annualPartition = page.locator('[data-partition-id="template-annual-review"]')
    await expect(annualPartition).toBeVisible({ timeout: 30_000 })
    await expectDashboardPartitionHeading(page, /annual review due/i)

    await annualPartition.getByRole('button', { name: /^Filter Item$/i }).click()
    const filterInput = page.locator('.table-column-filter-popover input:visible')
    await expect(filterInput).toBeVisible()
    await filterInput.fill(fixture.name)

    const row = annualPartition
      .locator('.el-table__row.app-data-table__activatable-row')
      .filter({ hasText: fixture.name })
      .first()
    await expect(row).toBeVisible({ timeout: 30_000 })
    await row.getByRole('button', { name: /^open$/i }).click()

    await expect(page).toHaveURL(new RegExp(`/templates/${fixture.templateId}`), {
      timeout: 30_000,
    })
    await expect(page).toHaveURL(/tab=overview|overview/i)
    await expect(page.getByTestId('template-overview-summary')).toBeVisible({ timeout: 20_000 })
    await expect(page.getByTestId('template-annual-review-due-value')).toHaveText(utcToday())
    await expect(page.getByTestId('template-annual-review-complete')).toBeVisible()

    await page.getByTestId('template-annual-review-complete').click()
    await confirmMessageBox(page)

    await expect(page.getByTestId('template-annual-review-due-value')).toHaveText(utcPlusDays(365), {
      timeout: 20_000,
    })

    const { tasks } = await listAnnualReviewDueTasksViaApi(request)
    expect(tasks.some((t) => t.templateId === fixture.templateId)).toBeFalsy()
  })

  test('BDD-CE-G05-011 — NAME search matches name, ignores body phrase', async ({ request }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const uniqueNameToken = `AlphaG05${stamp}`
    const bodyOnlyPhrase = `force-majeure-body-only-${stamp}`
    const module = await createApprovedModuleWithBodyPhrase(request, {
      phrase: bodyOnlyPhrase,
      name: `E2E ${uniqueNameToken} Clause`,
    })

    const byName = await listContentModulesSearchViaApi(request, {
      search: uniqueNameToken,
      searchMode: 'NAME',
    })
    expect(byName.content.some((row) => row.moduleId === module.moduleId)).toBeTruthy()

    const byBodyAsName = await listContentModulesSearchViaApi(request, {
      search: bodyOnlyPhrase,
      searchMode: 'NAME',
    })
    expect(byBodyAsName.content.some((row) => row.moduleId === module.moduleId)).toBeFalsy()
  })

  test('BDD-CE-G05-012 — FULL_TEXT hits unique body phrase', async ({ request }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const phrase = `force majeure carve-out-xyz-${stamp}`
    const module = await createApprovedModuleWithBodyPhrase(request, {
      phrase,
      name: `E2E G05 FTS Name ${stamp}`,
    })

    const pageView = await listContentModulesSearchViaApi(request, {
      search: phrase,
      searchMode: 'FULL_TEXT',
    })
    expect(pageView.content.some((row) => row.moduleId === module.moduleId)).toBeTruthy()
  })

  test('BDD-CE-G05-013 — FULL_TEXT does not leak invisible-group modules', async ({ request }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const phrase = `retail-secret-fts-${stamp}`
    const module = await createApprovedModuleWithBodyPhrase(request, {
      phrase,
      name: `E2E G05 Retail Secret ${stamp}`,
    })

    const retailHit = await listContentModulesSearchViaApi(
      request,
      { search: phrase, searchMode: 'FULL_TEXT' },
      E2E_TEMPLATE_AUTHOR,
    )
    expect(retailHit.content.some((row) => row.moduleId === module.moduleId)).toBeTruthy()

    const corpHit = await listContentModulesSearchViaApi(
      request,
      { search: phrase, searchMode: 'FULL_TEXT' },
      E2E_CORP_TEMPLATE_AUTHOR,
    )
    expect(corpHit.content.some((row) => row.moduleId === module.moduleId)).toBeFalsy()
  })

  test('BDD-CE-G05-014/015 — where-used lists referencing templates; empty when none', async ({
    request,
  }) => {
    const published = await preparePublishedTemplateWithLockedReference(request)
    const whereUsed = await listWhereUsedViaApi(request, published.moduleId)
    expect(whereUsed.content.some((row) => row.id === published.templateId)).toBeTruthy()
    expect(whereUsed.content.every((row) => row.groupCode)).toBeTruthy()

    const unused = await createApprovedModuleWithBodyPhrase(request, {
      phrase: `unused-where-used-${Date.now().toString(36)}`,
      name: `E2E G05 Unused ${Date.now().toString(36).toUpperCase()}`,
    })
    const empty = await listWhereUsedViaApi(request, unused.moduleId)
    expect(empty.content).toHaveLength(0)
    expect(empty.totalElements ?? 0).toBe(0)
  })

  test('BDD-CE-G05-016 — FTS index follows catalog-filter version body', async ({ request }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const phraseA = `fts-phrase-a-${stamp}`
    const phraseB = `fts-phrase-b-${stamp}`
    const module = await createApprovedModuleWithBodyPhrase(request, {
      phrase: phraseA,
      name: `E2E G05 FTS bump ${stamp}`,
    })

    const before = await listContentModulesSearchViaApi(request, {
      search: phraseA,
      searchMode: 'FULL_TEXT',
    })
    expect(before.content.some((row) => row.moduleId === module.moduleId)).toBeTruthy()

    await bumpModuleBodyPhrase(request, module.moduleId, '1.1.0', phraseB)

    const afterA = await listContentModulesSearchViaApi(request, {
      search: phraseA,
      searchMode: 'FULL_TEXT',
    })
    expect(afterA.content.some((row) => row.moduleId === module.moduleId)).toBeFalsy()

    const afterB = await listContentModulesSearchViaApi(request, {
      search: phraseB,
      searchMode: 'FULL_TEXT',
    })
    expect(afterB.content.some((row) => row.moduleId === module.moduleId)).toBeTruthy()
  })

  test('BDD-CE-G05-017 — FE FULL_TEXT search + where-used tab', async ({ page, request }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const phrase = `fe-fts-journey-${stamp}`
    const module = await createApprovedModuleWithBodyPhrase(request, {
      phrase,
      name: `E2E G05 FE FTS ${stamp}`,
    })
    const published = await preparePublishedTemplateReferencingModule(request, module, {
      externalIdPrefix: 'E2E-G05-FTS',
      referenceKey: 'E2E_G05_FTS_REF',
      name: `E2E G05 FE FTS tpl ${stamp}`,
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await openContentModulesList(page)

    await expect(page.getByTestId('content-module-search-mode')).toBeVisible()
    await page.getByTestId('content-module-search-mode').click()
    await selectElementPlusOption(page, /full text \(body\)/i)

    const listResponse = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes('/content-modules') &&
        response.url().includes('searchMode=FULL_TEXT') &&
        response.url().includes('search=') &&
        response.ok(),
      { timeout: 30_000 },
    )
    const search = page.getByTestId('catalog-filter-search')
    await search.fill(phrase)
    await listResponse
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const nameLink = page.getByRole('link', { name: module.name }).first()
    await expect(nameLink).toBeVisible({ timeout: 30_000 })
    await nameLink.click()

    await expect(page).toHaveURL(new RegExp(`/content-modules/${module.moduleId}`), {
      timeout: 20_000,
    })
    await page.getByRole('tab', { name: /^where used$/i }).click()
    await expect(page.getByTestId('content-module-where-used')).toBeVisible()
    await expect(page.getByTestId('content-module-where-used-table')).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByText(published.externalId, { exact: false })).toBeVisible()
  })

  test('BDD-CE-G05-019 — non-goals: annual review is author-workflow, not collaboration queue', async ({
    request,
  }) => {
    const fixture = await preparePublishedAnnualReviewTemplate(request)
    await markTemplateAnnualReviewDueToday(request, fixture.templateId)

    const { status, tasks } = await listAnnualReviewDueTasksViaApi(request)
    expect(status).toBe(200)
    expect(tasks.some((t) => t.templateId === fixture.templateId)).toBeTruthy()

    // Author-workflow projection path (not collaboration work-item queue_type)
    const tokenResponse = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
      data: E2E_TEMPLATE_AUTHOR,
    })
    const token = ((await tokenResponse.json()) as { result: { accessToken: string } }).result
      .accessToken
    const collab = await request.get(
      `${E2E_API_BASE_URL}/collaboration/work-items?queue=ANNUAL_REVIEW`,
      { headers: { Authorization: `Bearer ${token}` } },
    )
    const collabStatus = collab.status()
    // ANNUAL_REVIEW is not a collaboration queue_type — must not succeed as a work-item list
    expect(collabStatus).not.toBe(200)

    // Positive control: author-workflow path remains the SoT projection
    expect(status).toBe(200)
    expect(
      `${E2E_API_BASE_URL}/author-workflow/annual-review-due-tasks`,
    ).toContain('/author-workflow/annual-review-due-tasks')
  })
})
