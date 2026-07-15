/**
 * CE-U10 — sharedGroupCodes create / settings / summary UI
 * BDD: docs/behavior/ce-u10-shared-group-codes-ui.md (BDD-CE-U10-SGC-001…007)
 */
import { expect, test, type Page } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import {
  createDraftContentModuleWithSharedGroups,
  getContentModuleDetailViaApi,
} from './helpers/content-modules-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { confirmMessageBox, openContentModulesList } from './helpers/ui'

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

async function selectOwnerGroup(dialog: ReturnType<Page['getByRole']>, page: Page, groupCode: string) {
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

/**
 * CE-U10 sharedGroupCodes journeys (create / summary / settings / fail-closed).
 */
test.describe('CE-U10 sharedGroupCodes UI (BDD-CE-U10-SGC)', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-CE-U10-SGC-001 — GROUP_ADMIN create writes sharedGroupCodes', async ({
    page,
    request,
  }) => {
    const stamp = Date.now().toString(36).toUpperCase()
    const moduleCode = `E2E-SGC-CRT-${stamp}`
    const name = `E2E SGC Create ${stamp}`

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    const dialog = await openCreateDialog(page)

    await selectOwnerGroup(dialog, page, DEMO_GROUP_CODE)
    await expect(dialog.getByText(/share to groups/i)).toBeVisible()
    await selectSharedGroups(page, dialog, [SHARE_TARGET_GROUP])

    await dialog.getByTestId('module-code-input').fill(moduleCode)
    await dialog.getByTestId('module-name-input').fill(name)
    await dialog.getByRole('button', { name: /^create module$/i }).click()

    await expect(page).toHaveURL(/\/content-modules\/[^/?]+/, { timeout: 30_000 })
    await expect(page.getByRole('heading', { level: 1, name })).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('.page-header')).toContainText(`Owner: ${DEMO_GROUP_CODE}`)
    await expect(page.locator('.page-header')).toContainText(`Shared with: ${SHARE_TARGET_GROUP}`)

    const moduleId = page.url().match(/\/content-modules\/([^/?]+)/)?.[1]
    expect(moduleId).toBeTruthy()
    const detail = await getContentModuleDetailViaApi(request, moduleId!)
    expect(detail.sharedGroupCodes ?? []).toEqual([SHARE_TARGET_GROUP])
    expect(detail.groupCode).toBe(DEMO_GROUP_CODE)
  })

  test('BDD-CE-U10-SGC-002 — TEMPLATE_AUTHOR create hides Share to groups', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    const dialog = await openCreateDialog(page)

    await expect(dialog.getByText(/share to groups/i)).toHaveCount(0)
    await expect(dialog.getByTestId('content-module-shared-groups-select')).toHaveCount(0)
  })

  test('BDD-CE-U10-SGC-003 — detail summary shows owner, shared, and empty state', async ({
    page,
    request,
  }) => {
    const shared = await createDraftContentModuleWithSharedGroups(request, {
      name: `E2E SGC Summary Shared ${Date.now()}`,
      sharedGroupCodes: [SHARE_TARGET_GROUP],
    })
    const empty = await createDraftContentModuleWithSharedGroups(request, {
      name: `E2E SGC Summary Empty ${Date.now()}`,
      sharedGroupCodes: [],
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)

    await page.goto(`/content-modules/${shared.moduleId}`)
    await expect(page.getByRole('heading', { level: 1, name: shared.name })).toBeVisible({
      timeout: 30_000,
    })
    const sharedHeader = page.locator('.page-header')
    await expect(sharedHeader).toContainText(`Owner: ${DEMO_GROUP_CODE}`)
    await expect(sharedHeader).toContainText(`Shared with: ${SHARE_TARGET_GROUP}`)

    await page.goto(`/content-modules/${empty.moduleId}`)
    await expect(page.getByRole('heading', { level: 1, name: empty.name })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.page-header')).toContainText('Not shared outside owner group')
  })

  test('BDD-CE-U10-SGC-004/005 — Settings update with confirm cancel then save', async ({
    page,
    request,
  }) => {
    const fixture = await createDraftContentModuleWithSharedGroups(request, {
      name: `E2E SGC Settings ${Date.now()}`,
      sharedGroupCodes: [],
    })

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await page.goto(`/content-modules/${fixture.moduleId}`)
    await expect(page.getByRole('heading', { level: 1, name: fixture.name })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.page-header')).toContainText('Not shared outside owner group')

    await page.getByTestId('content-module-settings-open').click()
    const settings = page.getByRole('dialog', { name: /module settings/i })
    await expect(settings).toBeVisible()
    await expect(settings).toContainText(`Owner: ${DEMO_GROUP_CODE}`)
    await selectSharedGroups(page, settings, [SHARE_TARGET_GROUP])

    let putCount = 0
    page.on('request', (req) => {
      if (req.method() === 'PUT' && req.url().includes('/shared-group-codes')) {
        putCount += 1
      }
    })

    await settings.getByTestId('content-module-shared-groups-save').click()
    const confirmBox = page.locator('.el-message-box')
    await expect(confirmBox).toBeVisible()
    await expect(confirmBox).toContainText(/confirm shared group changes/i)
    await confirmBox.getByRole('button', { name: /^cancel$/i }).click()
    await expect(confirmBox).toHaveCount(0)
    expect(putCount).toBe(0)

    await settings.getByTestId('content-module-shared-groups-save').click()
    await confirmMessageBox(page)

    await expect(settings).toHaveCount(0, { timeout: 15_000 })
    await expect(page.getByText(/shared groups updated/i)).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('.page-header')).toContainText(`Shared with: ${SHARE_TARGET_GROUP}`, {
      timeout: 15_000,
    })
    expect(putCount).toBe(1)

    const detail = await getContentModuleDetailViaApi(request, fixture.moduleId)
    expect(detail.sharedGroupCodes ?? []).toEqual([SHARE_TARGET_GROUP])
  })

  test('BDD-CE-U10-SGC-006 — TEMPLATE_AUTHOR sees summary but not Settings', async ({
    page,
    request,
  }) => {
    const fixture = await createDraftContentModuleWithSharedGroups(request, {
      name: `E2E SGC No Settings ${Date.now()}`,
      sharedGroupCodes: [SHARE_TARGET_GROUP],
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await page.goto(`/content-modules/${fixture.moduleId}`)
    await expect(page.getByRole('heading', { level: 1, name: fixture.name })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.page-header')).toContainText(`Shared with: ${SHARE_TARGET_GROUP}`)
    await expect(page.getByTestId('content-module-settings-open')).toHaveCount(0)
  })

  test('BDD-CE-U10-SGC-007 — share options exclude owning groupCode', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    const dialog = await openCreateDialog(page)

    await selectOwnerGroup(dialog, page, DEMO_GROUP_CODE)
    const select = dialog.getByTestId('content-module-shared-groups-select')
    await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
    await select.click()
    const dropdown = page.locator('.el-select-dropdown:visible')
    await expect(dropdown.getByRole('option', { name: SHARE_TARGET_GROUP, exact: true })).toBeVisible()
    await expect(dropdown.getByRole('option', { name: DEMO_GROUP_CODE, exact: true })).toHaveCount(0)
    await page.keyboard.press('Escape')
  })
})
