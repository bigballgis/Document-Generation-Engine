/**
 * CE-U20 — clause create structured editor + catalog Status column/filter
 * BDD: docs/behavior/ce-u20-clause-create-structured.md (BDD-CE-U20-CCS-001…010)
 */
import { expect, test, type Page, type Request } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import {
  createApprovedContentModule,
  createDraftContentModule,
  createStoppedContentModule,
  getContentModuleDetailViaApi,
  listContentModulesViaApi,
} from './helpers/content-modules-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { openContentModulesList } from './helpers/ui'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const SHARE_TARGET_GROUP = 'CORP'

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openCreateDialog(page: Page) {
  await openContentModulesList(page)
  await page.getByRole('button', { name: /^new content module$/i }).first().click()
  const dialog = page.getByRole('dialog', { name: /create content module/i })
  await expect(dialog).toBeVisible()
  return dialog
}

async function selectOwnerGroup(
  dialog: ReturnType<Page['getByRole']>,
  page: Page,
  groupCode: string,
) {
  await dialog.getByRole('combobox', { name: /^\*?group$/i }).click()
  const dropdown = page.locator('.el-select-dropdown:visible')
  await expect(dropdown).toBeVisible()
  await dropdown.getByRole('option', { name: groupCode, exact: true }).click()
  await expect(dialog.locator('.el-form-item').first()).toContainText(groupCode)
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
}

async function selectSharedGroups(
  page: Page,
  root: ReturnType<Page['locator']> | ReturnType<Page['getByRole']>,
  codes: string[],
) {
  const select = root.getByTestId('content-module-shared-groups-select')
  for (const code of codes) {
    await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
    await select.click()
    const dropdown = page.locator('.el-select-dropdown:visible')
    await expect(dropdown).toBeVisible()
    await dropdown.getByRole('option', { name: code, exact: true }).click({ force: true })
  }
  await page.keyboard.press('Escape')
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
  for (const code of codes) {
    await expect(select).toContainText(code)
  }
}

function catalogStatusFilter(page: Page) {
  return page.locator('.catalog-filter-toolbar').getByRole('combobox', { name: /^status$/i })
}

async function setCatalogStatusFilter(page: Page, statusLabel: string | RegExp) {
  const select = catalogStatusFilter(page)
  await expect(select).toBeVisible()
  await select.click()
  const dropdown = page.locator('.el-select-dropdown:visible')
  await expect(dropdown).toBeVisible()
  await dropdown.getByRole('option', { name: statusLabel }).click()
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
}

function isContentModuleListGet(req: Request): boolean {
  if (req.method() !== 'GET') {
    return false
  }
  const url = req.url()
  return url.includes('/content-modules') && !url.includes('/content-modules/')
}

test.describe('CE-U20 clause create structured (BDD-CE-U20-CCS)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-CE-U20-CCS-001/002 — create dialog uses structured editor, not JSON textarea', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    const dialog = await openCreateDialog(page)

    const editor = dialog.getByTestId('controlled-structured-content-editor')
    await expect(editor).toBeVisible()
    await expect(editor.getByTestId('paragraph-input').first()).toBeVisible()

    // Structure field uses ControlledStructuredContentEditor — no legacy JSON textarea.
    await expect(dialog.getByText(/content structure \(json\)/i)).toHaveCount(0)
    await expect(dialog.locator('textarea').filter({ hasText: /"blocks"/i })).toHaveCount(0)
    // Default DEFAULT_STRUCTURED_CONTENT_JSON surface: empty paragraph node (not legacy blocks[]).
    await expect(editor.getByTestId('paragraph-input').first()).toHaveValue('')
  })

  test('BDD-CE-U20-CCS-003/010 — structured create → detail → list Status DRAFT + filter', async ({
    page,
    request,
  }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const moduleCode = `E2E-CCS-CRT-${stamp}`
    const name = `E2E CCS Create ${stamp}`
    const paragraphText = `E2E CCS structured paragraph ${stamp}`

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    const dialog = await openCreateDialog(page)

    await selectOwnerGroup(dialog, page, DEMO_GROUP_CODE)
    await dialog.getByTestId('module-code-input').fill(moduleCode)
    await dialog.getByTestId('module-name-input').fill(name)
    await dialog.getByTestId('controlled-structured-content-editor').getByTestId('paragraph-input').first().fill(paragraphText)

    const createRequestPromise = page.waitForRequest(
      (req) =>
        req.method() === 'POST' &&
        req.url().includes('/content-modules') &&
        !req.url().includes('/versions') &&
        !req.url().includes('/review'),
    )

    await dialog.getByRole('button', { name: /^create module$/i }).click()
    const createRequest = await createRequestPromise
    const payload = createRequest.postDataJSON() as { contentStructureJson?: string }
    expect(payload.contentStructureJson).toBeTruthy()
    expect(payload.contentStructureJson).toContain('schemaVersion')
    expect(payload.contentStructureJson).toContain('"nodes"')
    expect(payload.contentStructureJson).toContain(paragraphText)
    expect(payload.contentStructureJson).not.toMatch(/"blocks"\s*:\s*\[\s*\]/)

    await expect(page).toHaveURL(/\/content-modules\/[^/?]+/, { timeout: 30_000 })
    await expect(page.getByRole('heading', { level: 1, name })).toBeVisible({ timeout: 30_000 })
    await expect(page.getByText(/^draft$/i).first()).toBeVisible()

    const moduleId = page.url().match(/\/content-modules\/([^/?]+)/)?.[1]
    expect(moduleId).toBeTruthy()
    const detail = await getContentModuleDetailViaApi(request, moduleId!)
    expect(detail.versions[0]?.reviewState).toBe('DRAFT')

    await openContentModulesList(page)
    const searchBox = page.getByRole('textbox', { name: /search/i })
    await searchBox.fill(moduleCode)
    await expect(page.getByRole('columnheader', { name: /^status$/i })).toBeVisible()
    const row = page.locator('.el-table__body tr').filter({ hasText: moduleCode })
    await expect(row).toBeVisible({ timeout: 30_000 })
    await expect(row.getByText(/^draft$/i)).toBeVisible()

    const listWithDraft = page.waitForRequest(
      (req) => {
        if (!isContentModuleListGet(req)) {
          return false
        }
        const params = new URL(req.url()).searchParams
        return params.get('status') === 'DRAFT' && (params.get('search') ?? '').includes(moduleCode)
      },
    )
    await setCatalogStatusFilter(page, /^draft$/i)
    await listWithDraft
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.locator('.el-table__body tr').filter({ hasText: moduleCode })).toBeVisible({
      timeout: 30_000,
    })
  })

  test('BDD-CE-U20-CCS-004 — GROUP_ADMIN shared groups still posted with structured create', async ({
    page,
    request,
  }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const moduleCode = `E2E-CCS-SGC-${stamp}`
    const name = `E2E CCS Share ${stamp}`
    const paragraphText = `E2E CCS share paragraph ${stamp}`

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    const dialog = await openCreateDialog(page)

    await selectOwnerGroup(dialog, page, DEMO_GROUP_CODE)
    await expect(dialog.getByText(/share to groups/i)).toBeVisible()
    await selectSharedGroups(page, dialog, [SHARE_TARGET_GROUP])
    await expect(dialog.getByTestId('controlled-structured-content-editor')).toBeVisible()

    await dialog.getByTestId('module-code-input').fill(moduleCode)
    await dialog.getByTestId('module-name-input').fill(name)
    await dialog
      .getByTestId('controlled-structured-content-editor')
      .getByTestId('paragraph-input')
      .first()
      .fill(paragraphText)

    const createRequestPromise = page.waitForRequest(
      (req) =>
        req.method() === 'POST' &&
        req.url().includes('/content-modules') &&
        !req.url().includes('/versions'),
    )
    await dialog.getByRole('button', { name: /^create module$/i }).click()
    const createRequest = await createRequestPromise
    const payload = createRequest.postDataJSON() as {
      sharedGroupCodes?: string[]
      contentStructureJson?: string
    }
    expect(payload.sharedGroupCodes).toEqual([SHARE_TARGET_GROUP])
    expect(payload.contentStructureJson).toContain(paragraphText)

    await expect(page).toHaveURL(/\/content-modules\/[^/?]+/, { timeout: 30_000 })
    const moduleId = page.url().match(/\/content-modules\/([^/?]+)/)?.[1]
    expect(moduleId).toBeTruthy()
    const detail = await getContentModuleDetailViaApi(request, moduleId!)
    expect(detail.sharedGroupCodes ?? []).toEqual([SHARE_TARGET_GROUP])
  })

  test('BDD-CE-U20-CCS-005/006 — Status column + DRAFT filter requests status=DRAFT', async ({
    page,
    request,
  }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const draft = await createDraftContentModule(request, {
      moduleCode: `E2E-CCS-D-${stamp}`,
      name: `E2E CCS Draft ${stamp}`,
    })
    const approved = await createApprovedContentModule(request, {
      moduleCode: `E2E-CCS-A-${stamp}`,
      name: `E2E CCS Approved ${stamp}`,
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await openContentModulesList(page)

    await expect(page.getByRole('columnheader', { name: /^status$/i })).toBeVisible()
    await page.getByRole('textbox', { name: /search/i }).fill(stamp)
    await expect(page.locator('.el-table__body tr').filter({ hasText: draft.moduleCode })).toBeVisible({
      timeout: 30_000,
    })
    await expect(
      page.locator('.el-table__body tr').filter({ hasText: draft.moduleCode }).getByText(/^draft$/i),
    ).toBeVisible()
    await expect(
      page
        .locator('.el-table__body tr')
        .filter({ hasText: approved.moduleCode })
        .getByText(/^approved$/i),
    ).toBeVisible()

    const draftFilterRequest = page.waitForRequest(
      (req) => isContentModuleListGet(req) && new URL(req.url()).searchParams.get('status') === 'DRAFT',
    )
    await setCatalogStatusFilter(page, /^draft$/i)
    const matched = await draftFilterRequest
    expect(new URL(matched.url()).searchParams.get('page') ?? '0').toBe('0')

    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.locator('.el-table__body tr').filter({ hasText: draft.moduleCode })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.el-table__body tr').filter({ hasText: approved.moduleCode })).toHaveCount(
      0,
    )
  })

  test('BDD-CE-U20-CCS-007 — STOPPED filter matches lifecycle-priority badge', async ({
    page,
    request,
  }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const stopped = await createStoppedContentModule(request, {
      moduleCode: `E2E-CCS-S-${stamp}`,
      name: `E2E CCS Stopped ${stamp}`,
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await openContentModulesList(page)

    await page.getByRole('textbox', { name: /search/i }).fill(stopped.moduleCode)

    const stoppedFilterRequest = page.waitForRequest(
      (req) =>
        isContentModuleListGet(req) && new URL(req.url()).searchParams.get('status') === 'STOPPED',
    )
    await setCatalogStatusFilter(page, /^stopped$/i)
    await stoppedFilterRequest
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.locator('.el-table__body tr').filter({ hasText: stopped.moduleCode })).toBeVisible({
      timeout: 30_000,
    })
    await expect(
      page.locator('.el-table__body tr').filter({ hasText: stopped.moduleCode }).getByText(/^stopped$/i),
    ).toBeVisible()

    const draftFilterRequest = page.waitForRequest(
      (req) => isContentModuleListGet(req) && new URL(req.url()).searchParams.get('status') === 'DRAFT',
    )
    await setCatalogStatusFilter(page, /^draft$/i)
    await draftFilterRequest
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.locator('.el-table__body tr').filter({ hasText: stopped.moduleCode })).toHaveCount(0)
  })

  test('BDD-CE-U20-CCS-008 — unknown status returns successful empty page', async ({ request }) => {
    const page = await listContentModulesViaApi(request, {
      status: 'NOT_A_REAL_STATUS',
      page: 0,
      size: 20,
    })
    expect(page.content ?? []).toEqual([])
    expect(page.totalElements ?? 0).toBe(0)
  })

  test('BDD-CE-U20-CCS-009 — no author capability hides Create CTA', async ({ page, request }) => {
    await createDraftContentModule(request, {
      name: `E2E CCS FailClosed ${Date.now()}`,
    })

    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await dismissOnboardingTourIfPresent(page)
    await openContentModulesList(page)

    await expect(page.getByRole('button', { name: /^new content module$/i })).toHaveCount(0)
    await expect(page.getByRole('dialog', { name: /create content module/i })).toHaveCount(0)
    await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0)
  })
})
