/**
 * IBL-E1 / #128 — Management UI locale-variant journeys (functional).
 * BDD: docs/behavior/ibl-e1-locale-variant-model.md
 *   - BDD-IBL-E1-013 create requires locale
 *   - BDD-IBL-E1-014 catalog locale filter
 *   - BDD-IBL-E1-015 family sibling navigation
 */
import { expect, test, type Page, type Request } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  createLocaleVariantTemplate,
  prepareLocaleVariantSiblingPair,
} from './helpers/ibl-e1-locale-variant-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { randomUUID } from 'node:crypto'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openTemplateCreateDialog(page: Page) {
  await page.goto('/templates')
  await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
  await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
  await page.getByRole('button', { name: /new template package/i }).click()
  const dialog = page.getByRole('dialog', { name: /^create template$/i })
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  return dialog
}

function isTemplateListGet(req: Request): boolean {
  if (req.method() !== 'GET') {
    return false
  }
  const url = req.url()
  return url.includes('/templates') && !url.includes('/templates/')
}

/** AppDataTable activatable rows expose name "Open row details" — match by cell text. */
function templateCatalogRow(page: Page, externalId: string) {
  return page.locator('.el-table__body tr').filter({ hasText: externalId })
}

test.describe('IBL-E1 locale-variant model UI (BDD-IBL-E1-013…015)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-IBL-E1-013 — create form requires body locale; blocks blank submit', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    const dialog = await openTemplateCreateDialog(page)

    await expect(dialog.getByTestId('template-create-locale')).toBeVisible()
    await expect(dialog.getByLabel(/^body locale$/i)).toBeVisible()

    let createPosted = false
    page.on('request', (req) => {
      if (
        req.method() === 'POST' &&
        /\/api\/management\/v1\/templates(?:\?|$)/.test(new URL(req.url()).pathname)
      ) {
        createPosted = true
      }
    })

    // Submit with empty locale (and other empties) — client must surface locale validation.
    await dialog.getByRole('button', { name: /^create$/i }).click()

    await expect(dialog.getByText(/select a body locale for this template/i)).toBeVisible()
    await expect(dialog).toBeVisible()
    expect(createPosted).toBe(false)
  })

  test('BDD-IBL-E1-013 — locale options selectable; hub shows persisted locale', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    const dialog = await openTemplateCreateDialog(page)

    await dialog.getByTestId('template-create-locale').click()
    const localeDropdown = page.locator('.el-select-dropdown:visible')
    await expect(localeDropdown).toBeVisible()
    await expect(localeDropdown.getByRole('option', { name: /english \(en-us\)/i })).toBeVisible()
    await expect(localeDropdown.getByRole('option', { name: /chinese \(zh-cn\)/i })).toBeVisible()
    await localeDropdown.getByRole('option', { name: /english \(en-us\)/i }).click()
    await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
    await expect(dialog.getByTestId('template-create-locale')).toContainText(/en-US/i)

    // Persist via API (letterhead picker depends on mastersStore warm; out of E1 scope).
    const created = await createLocaleVariantTemplate(request, {
      locale: 'en-US',
      localeVariantFamilyId: randomUUID(),
    })
    await page.goto(`/templates/${created.templateId}`)
    await expect(page.getByTestId('template-overview-locale')).toContainText('en-US')
  })

  test('BDD-IBL-E1-014 — catalog locale filter shows only matching packages', async ({
    page,
    request,
  }) => {
    const pair = await prepareLocaleVariantSiblingPair(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await page.goto('/templates')
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /^locale$/i })).toBeVisible()

    const searchBox = page.getByTestId('catalog-filter-search')
    // Stamp appears in both sibling externalIds / names — isolate this pair first.
    await searchBox.fill(pair.stamp)

    await expect(templateCatalogRow(page, pair.en.externalId)).toBeVisible({ timeout: 30_000 })
    await expect(templateCatalogRow(page, pair.zh.externalId)).toBeVisible()

    const listRequestPromise = page.waitForRequest(
      (req) => isTemplateListGet(req) && new URL(req.url()).searchParams.get('locale') === 'en-US',
    )

    const localeFilter = page
      .locator('.catalog-filter-toolbar')
      .getByRole('combobox', { name: /^locale$/i })
    await expect(localeFilter).toBeVisible()
    await localeFilter.click()
    const dropdown = page.locator('.el-select-dropdown:visible')
    await expect(dropdown).toBeVisible()
    await dropdown.getByRole('option', { name: 'en-US', exact: true }).click()
    await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)

    const listRequest = await listRequestPromise
    expect(new URL(listRequest.url()).searchParams.get('locale')).toBe('en-US')

    await expect(templateCatalogRow(page, pair.en.externalId)).toBeVisible({ timeout: 30_000 })
    await expect(templateCatalogRow(page, pair.zh.externalId)).toHaveCount(0)

    const enRow = templateCatalogRow(page, pair.en.externalId)
    await expect(enRow.getByText('en-US', { exact: true })).toBeVisible()
  })

  test('BDD-IBL-E1-015 — template hub shows sibling locale and navigates to brother package', async ({
    page,
    request,
  }) => {
    const pair = await prepareLocaleVariantSiblingPair(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await page.goto(`/templates/${pair.en.templateId}`)

    await expect(page.getByTestId('template-overview-locale')).toContainText('en-US')
    const familyNav = page.getByTestId('locale-variant-family-nav')
    await expect(familyNav).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('locale-variant-current')).toContainText('en-US')

    const sibling = familyNav.getByTestId('locale-variant-sibling')
    await expect(sibling).toHaveCount(1)
    await expect(sibling).toContainText(pair.zh.name)
    await expect(sibling).toContainText('zh-CN')
    await expect(sibling).toContainText(pair.zh.externalId)

    await sibling.getByRole('link', { name: pair.zh.name }).click()
    await expect(page).toHaveURL(new RegExp(`/templates/${pair.zh.templateId}`), {
      timeout: 15_000,
    })
    await expect(page.getByTestId('template-overview-locale')).toContainText('zh-CN')
    await expect(page.getByTestId('locale-variant-current')).toContainText('zh-CN')
    await expect(page.getByTestId('locale-variant-sibling')).toContainText('en-US')
  })
})
