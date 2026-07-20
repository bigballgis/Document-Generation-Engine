/**
 * SYS-NORM Wave 2 / #146 UIUX evidence — Package Hub IA dual-brand @1440×900.
 *
 * Surfaces: Template hub Version lines, Properties drawer, API package settings,
 * Master hub Properties parity.
 *
 * BDD SoT: docs/behavior/sys-norm-hub-ia.md
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/SYS-NORM-W2-hub-ia-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Page } from '@playwright/test'

import {
  DEMO_MASTER_NAME,
  DEMO_TEMPLATE_EXTERNAL_ID,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { findTemplateByExternalId } from './helpers/content-modules-api'
import { demoMasterDetailPath } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureSysNormW2LocatorScreenshot,
  captureSysNormW2Screenshot,
  dismissOnboardingTourIfPresent,
  ensureSysNormW2EvidenceDirs,
  switchBrand,
  SYS_NORM_W2_VIEWPORT,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return { scrollWidth: doc.scrollWidth, clientWidth: doc.clientWidth }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function assertPrimaryBrandColor(page: Page, brand: 'REDBC' | 'GREENBC'): Promise<void> {
  const expected = brand === 'REDBC' ? '#db0011' : '#00847f'
  const primary = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim().toLowerCase(),
  )
  expect(primary, `expected --brand-primary ${expected} for ${brand}`).toBe(expected)
}

async function openTemplateHub(page: Page, templateId: string) {
  await page.goto(`/templates/${templateId}`)
  await expect(page.getByText(/unable to load template/i)).not.toBeVisible()
  await expect(page.getByTestId('template-package-hub')).toBeVisible({ timeout: 20_000 })
  await expect(page.locator('.version-lines-card')).toBeVisible({ timeout: 20_000 })
}

test.describe('SYS-NORM Wave 2 UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let templateId = ''
  let masterHubPath = ''

  test.beforeAll(async ({ request }) => {
    ensureSysNormW2EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080).`,
    })
    const template = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
    if (!template) {
      throw new Error(
        `Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" missing. Ensure DOCGEN_SEED_DEMO_CATALOG=true.`,
      )
    }
    templateId = template.id
    masterHubPath = await demoMasterDetailPath(request)
  })

  test('01–02 Template hub Version lines dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W2_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await openTemplateHub(page, templateId)

      const hub = page.getByTestId('template-package-hub')
      await expect(hub).toHaveClass(/app-page-layout--fluid/)
      await expect(hub.locator('.secondary-tabs')).toHaveCount(0)
      await expect(page.getByText(/version lines/i).first()).toBeVisible()
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureSysNormW2Screenshot(page, `01-template-hub-version-lines-${suffix}-1440x900.png`)
      await captureSysNormW2LocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `01b-brand-header-${suffix}-crop.png`,
      )
      await captureSysNormW2LocatorScreenshot(
        page.locator('.version-lines-card'),
        `01c-version-lines-card-${suffix}-crop.png`,
      )
    }
  })

  test('03–04 Properties drawer dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W2_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await openTemplateHub(page, templateId)
      await page.getByTestId('hub-properties-action').click()
      const drawer = page.getByTestId('template-properties-drawer')
      await expect(drawer).toBeVisible({ timeout: 15_000 })
      await expect(drawer.getByTestId('template-overview-summary')).toBeVisible()
      await expect(page.locator('.version-lines-card')).toBeVisible()
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureSysNormW2Screenshot(page, `02-template-hub-properties-drawer-${suffix}-1440x900.png`)
      await captureSysNormW2LocatorScreenshot(
        drawer,
        `02b-properties-drawer-${suffix}-crop.png`,
      )
      await page.keyboard.press('Escape')
      await expect(drawer).toBeHidden({ timeout: 15_000 })
    }
  })

  test('05–06 API package settings dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W2_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await openTemplateHub(page, templateId)
      await page.getByTestId('hub-api-settings-action').click()
      await expect(page).toHaveURL(
        new RegExp(`/api/packages/${templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/settings`),
        { timeout: 20_000 },
      )
      await expect(page.getByTestId('api-package-settings-interim-banner')).toBeVisible({
        timeout: 20_000,
      })
      await expect(page.getByTestId('api-package-settings-panel')).toBeVisible()
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureSysNormW2Screenshot(page, `03-api-package-settings-${suffix}-1440x900.png`)
      await captureSysNormW2LocatorScreenshot(
        page.getByTestId('api-package-settings-interim-banner'),
        `03b-api-settings-interim-banner-${suffix}-crop.png`,
      )
    }
  })

  test('07–08 Master hub Properties parity dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W2_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await page.goto(masterHubPath)
      await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible({
        timeout: 20_000,
      })
      const revisionLines = page.locator('.revision-lines-card')
      await expect(revisionLines.getByText(/^revision lines$/i)).toBeVisible({ timeout: 20_000 })
      await expect(page.getByTestId('hub-api-settings-action')).toHaveCount(0)

      await page.getByTestId('master-hub-properties-action').click()
      const drawer = page.getByTestId('master-properties-drawer')
      await expect(drawer).toBeVisible({ timeout: 15_000 })
      await expect(revisionLines).toBeVisible()
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureSysNormW2Screenshot(page, `04-master-hub-properties-${suffix}-1440x900.png`)
      await captureSysNormW2LocatorScreenshot(
        drawer,
        `04b-master-properties-drawer-${suffix}-crop.png`,
      )
      await page.keyboard.press('Escape')
      await expect(drawer).toBeHidden({ timeout: 15_000 })
    }
  })
})
