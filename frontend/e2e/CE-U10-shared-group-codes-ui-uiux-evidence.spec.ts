/**
 * CE-U10 UIUX evidence — sharedGroupCodes create / settings / summary
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: SGC-001…007 surfaces (create multiselect, detail summary, settings dialog)
 *
 * Note: close el-dialog before switchBrand — modal overlay intercepts header brand switcher.
 */
import { expect, test, type Page } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import { createDraftContentModuleWithSharedGroups } from './helpers/content-modules-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { openContentModulesList } from './helpers/ui'
import {
  captureCeU10LocatorScreenshot,
  captureCeU10Screenshot,
  CE_U10_VIEWPORT,
  ensureCeU10EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

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

async function openCreateDialogPrepared(page: Page) {
  await openContentModulesList(page)
  await page.getByRole('button', { name: /^new content module$/i }).first().click()
  const dialog = page.getByRole('dialog', { name: /create content module/i })
  await expect(dialog).toBeVisible()
  return dialog
}

async function prepareAdminCreateWithShare(page: Page) {
  const dialog = await openCreateDialogPrepared(page)
  await dialog.getByRole('combobox', { name: /^\*?group$/i }).click()
  const ownerDropdown = page.locator('.el-select-dropdown:visible')
  await expect(ownerDropdown).toBeVisible()
  await ownerDropdown.getByRole('option', { name: DEMO_GROUP_CODE, exact: true }).click()
  await expect(dialog.locator('.el-form-item').first()).toContainText(DEMO_GROUP_CODE)
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
  await expect(dialog.getByText(/share to groups/i)).toBeVisible()
  return dialog
}

async function closeDialog(dialog: ReturnType<Page['getByRole']>) {
  await dialog.getByRole('button', { name: /^cancel$/i }).click()
  await expect(dialog).toHaveCount(0)
}

test.describe('CE-U10 sharedGroupCodes UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeU10EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
  })

  test('01–04 dual-brand: create Share to groups + author hide', async ({ page }) => {
    await page.setViewportSize(CE_U10_VIEWPORT)

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    let createDialog = await prepareAdminCreateWithShare(page)
    await captureCeU10Screenshot(page, '01-create-share-to-groups-redbc-1920x1080.png')
    await captureCeU10LocatorScreenshot(
      createDialog,
      '01b-create-dialog-crop-redbc-1920x1080.png',
    )
    await closeDialog(createDialog)

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    createDialog = await prepareAdminCreateWithShare(page)
    await captureCeU10Screenshot(page, '02-create-share-to-groups-greenbc-1920x1080.png')
    await closeDialog(createDialog)
    await captureCeU10LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02b-brand-header-greenbc-crop.png',
    )

    await page.locator('header .user-menu-trigger').click()
    await page.getByRole('menuitem', { name: /sign out|退出登录/i }).click()
    await expect(page).toHaveURL(/\/login/)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    let authorDialog = await openCreateDialogPrepared(page)
    await expect(authorDialog.getByText(/share to groups/i)).toHaveCount(0)
    await captureCeU10Screenshot(page, '03-create-author-no-share-redbc-1920x1080.png')
    await closeDialog(authorDialog)

    await switchBrand(page, 'GREENBC')
    authorDialog = await openCreateDialogPrepared(page)
    await expect(authorDialog.getByText(/share to groups/i)).toHaveCount(0)
    await captureCeU10Screenshot(page, '04-create-author-no-share-greenbc-1920x1080.png')
    await closeDialog(authorDialog)
  })

  test('05–10 dual-brand: summary + settings dialog + confirm', async ({ page, request }) => {
    await page.setViewportSize(CE_U10_VIEWPORT)
    const fixture = await createDraftContentModuleWithSharedGroups(request, {
      name: `E2E SGC UIUX ${Date.now()}`,
      sharedGroupCodes: [SHARE_TARGET_GROUP],
    })

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await page.goto(`/content-modules/${fixture.moduleId}`)
    await expect(page.getByRole('heading', { level: 1, name: fixture.name })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.page-header')).toContainText(`Owner: ${DEMO_GROUP_CODE}`)
    await expect(page.locator('.page-header')).toContainText(`Shared with: ${SHARE_TARGET_GROUP}`)
    await expect(page.getByTestId('content-module-settings-open')).toBeVisible()

    await captureCeU10Screenshot(page, '05-detail-summary-shared-redbc-1920x1080.png')
    await captureCeU10LocatorScreenshot(
      page.locator('.page-header'),
      '05b-page-header-summary-crop-redbc.png',
    )

    await switchBrand(page, 'GREENBC')
    await captureCeU10Screenshot(page, '06-detail-summary-shared-greenbc-1920x1080.png')

    await switchBrand(page, 'REDBC')
    await page.getByTestId('content-module-settings-open').click()
    let settings = page.getByRole('dialog', { name: /module settings/i })
    await expect(settings).toBeVisible()
    await expect(settings.getByTestId('content-module-shared-groups-select')).toBeVisible()
    await captureCeU10Screenshot(page, '07-settings-dialog-redbc-1920x1080.png')
    await captureCeU10LocatorScreenshot(settings, '07b-settings-dialog-crop-redbc.png')
    await closeDialog(settings)

    await switchBrand(page, 'GREENBC')
    await page.getByTestId('content-module-settings-open').click()
    settings = page.getByRole('dialog', { name: /module settings/i })
    await expect(settings).toBeVisible()
    await captureCeU10Screenshot(page, '08-settings-dialog-greenbc-1920x1080.png')
    await closeDialog(settings)

    await switchBrand(page, 'REDBC')
    await page.getByTestId('content-module-settings-open').click()
    settings = page.getByRole('dialog', { name: /module settings/i })
    await expect(settings).toBeVisible()
    const select = settings.getByTestId('content-module-shared-groups-select')
    await select.locator('.el-tag__close').first().click()
    await settings.getByTestId('content-module-shared-groups-save').click()
    const confirmBox = page.locator('.el-message-box')
    await expect(confirmBox).toBeVisible()
    await expect(confirmBox).toContainText(/confirm shared group changes/i)
    await captureCeU10Screenshot(page, '09-settings-confirm-redbc-1920x1080.png')
    await confirmBox.getByRole('button', { name: /^cancel$/i }).click()
    await closeDialog(settings)

    await switchBrand(page, 'GREENBC')
    await captureCeU10LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '09b-brand-header-greenbc-crop.png',
    )
    await switchBrand(page, 'REDBC')
    await captureCeU10LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '10-brand-header-redbc-crop.png',
    )
  })
})
