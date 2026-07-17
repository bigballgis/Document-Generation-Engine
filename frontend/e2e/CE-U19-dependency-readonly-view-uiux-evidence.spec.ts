/**
 * CE-U19 UIUX evidence — Package Hub Dependencies tab (read-only)
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/ce-u19-dependency-readonly-view.md (DRV-001…007 / DRV-012 visual)
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { preparePublishedTemplateWithLockedReference } from './helpers/content-modules-api'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareEmptyDraftTemplate } from './helpers/structured-authoring-api'
import { cloneReleaseVersion as cloneReleaseVersionApi } from './helpers/template-version-lines-api'
import {
  captureCeU19LocatorScreenshot,
  captureCeU19Screenshot,
  CE_U19_VIEWPORT,
  ensureCeU19EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openDependenciesTab(page: Page, templateId: string): Promise<void> {
  await page.goto(`/templates/${templateId}?tab=dependencies`)
  await expect(page).toHaveURL(/tab=dependencies/)
  await expect(page.getByTestId('template-dependencies-panel')).toBeVisible({ timeout: 30_000 })
}

function dependenciesPanel(page: Page) {
  return page.getByTestId('template-dependencies-panel')
}

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return {
      scrollWidth: doc.scrollWidth,
      clientWidth: doc.clientWidth,
    }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function expectNoCriticalAxeViolations(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .include('[data-testid=template-dependencies-panel]')
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

async function assertReadOnlyNoWriteCtas(page: Page): Promise<void> {
  const panel = dependenciesPanel(page)
  await expect(panel.getByRole('button', { name: /save|publish|clone|abandon|upsert|bump/i })).toHaveCount(
    0,
  )
}

test.describe('CE-U19 dependency read-only view UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  let publishedTemplateId = ''
  let emptyDraftTemplateId = ''
  let referenceKey = ''

  test.beforeAll(async ({ request }) => {
    ensureCeU19EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)

    const published = await preparePublishedTemplateWithLockedReference(request)
    publishedTemplateId = published.templateId
    referenceKey = published.referenceKey
    await cloneReleaseVersionApi(request, published.templateId, '1.0.0', 201)

    const empty = await prepareEmptyDraftTemplate(request)
    emptyDraftTemplateId = empty.templateId
  })

  test('01–02 dual-brand: published Dependencies tab (pinned + clauses + release lines)', async ({
    page,
  }) => {
    await page.setViewportSize(CE_U19_VIEWPORT)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openDependenciesTab(page, publishedTemplateId)
    const panel = dependenciesPanel(page)

    await expect(page.getByTestId('template-dependencies-pinned')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('template-dependencies-master-link')).toBeVisible()
    await expect(page.getByTestId('template-dependencies-clauses-table')).toBeVisible()
    await expect(
      page.getByTestId('template-dependencies-clauses-table').getByText(referenceKey, { exact: true }),
    ).toBeVisible()
    await expect(page.getByTestId('template-dependencies-release-lines-table')).toBeVisible()
    await assertReadOnlyNoWriteCtas(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U19 REDBC published dependencies')

    await captureCeU19Screenshot(page, '01-dependencies-published-redbc-1920x1080.png')
    await captureCeU19LocatorScreenshot(panel, '01b-dependencies-panel-crop-redbc-1920x1080.png')
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-master-section'),
      '01c-master-pin-section-crop-redbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-clauses-section'),
      '01d-clauses-section-crop-redbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-release-lines-section'),
      '01e-release-lines-section-crop-redbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01f-brand-header-redbc-crop.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(panel).toBeVisible()
    await expect(page.getByTestId('template-dependencies-pinned')).toBeVisible()
    await assertReadOnlyNoWriteCtas(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U19 GREENBC published dependencies')

    await captureCeU19Screenshot(page, '02-dependencies-published-greenbc-1920x1080.png')
    await captureCeU19LocatorScreenshot(panel, '02b-dependencies-panel-crop-greenbc-1920x1080.png')
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-master-section'),
      '02c-master-pin-section-crop-greenbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-clauses-section'),
      '02d-clauses-section-crop-greenbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-release-lines-section'),
      '02e-release-lines-section-crop-greenbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02f-brand-header-greenbc-crop.png',
    )
  })

  test('03–04 dual-brand: empty draft Dependencies (not pinned + anchors empty)', async ({
    page,
  }) => {
    await page.setViewportSize(CE_U19_VIEWPORT)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    await openDependenciesTab(page, emptyDraftTemplateId)
    const panel = dependenciesPanel(page)

    await expect(page.getByTestId('template-dependencies-not-pinned')).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByTestId('template-dependencies-anchors-empty')).toBeVisible()
    await assertReadOnlyNoWriteCtas(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U19 REDBC empty draft dependencies')

    await captureCeU19Screenshot(page, '03-dependencies-empty-draft-redbc-1920x1080.png')
    await captureCeU19LocatorScreenshot(panel, '03b-dependencies-panel-crop-redbc-1920x1080.png')
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-master-section'),
      '03c-not-pinned-section-crop-redbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-anchors-section'),
      '03d-anchors-empty-crop-redbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03e-brand-header-redbc-crop.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.getByTestId('template-dependencies-not-pinned')).toBeVisible()
    await expect(page.getByTestId('template-dependencies-anchors-empty')).toBeVisible()
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U19 GREENBC empty draft dependencies')

    await captureCeU19Screenshot(page, '04-dependencies-empty-draft-greenbc-1920x1080.png')
    await captureCeU19LocatorScreenshot(panel, '04b-dependencies-panel-crop-greenbc-1920x1080.png')
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-master-section'),
      '04c-not-pinned-section-crop-greenbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.getByTestId('template-dependencies-anchors-section'),
      '04d-anchors-empty-crop-greenbc-1920x1080.png',
    )
    await captureCeU19LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04e-brand-header-greenbc-crop.png',
    )
  })
})
