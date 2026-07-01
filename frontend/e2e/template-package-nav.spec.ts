import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs, loginAsGlobalAdmin } from './helpers/auth'
import { preparePublishedTemplateWithLockedReference } from './helpers/content-modules-api'
import { assertFolCatalogSeeded } from './helpers/fol-api'
import {
  assertCloneBlockedWhenInFlight,
  assertCrossGroupVersionLineAccessDenied,
  assertPublishedVersionImmutable,
  cloneReleaseVersion as cloneReleaseVersionApi,
  listTemplateVersionLines,
} from './helpers/template-version-lines-api'

test.describe('template package hub navigation (BDD-TEMPLATE-PACKAGE-NAV-001)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsGlobalAdmin(page)
  })

  test('hub shows version lines and opens in-flight dev editor', async ({ page, request }) => {
    const fixture = await assertFolCatalogSeeded(request)

    await page.goto(`/templates/${fixture.templateId}`)

    await expect(page.getByText(/unable to load template/i)).not.toBeVisible()
    await expect(page.locator('.version-lines-card')).toBeVisible()
    await expect(page.getByText(/version lines/i)).toBeVisible()

    await page
      .locator('.version-lines-card')
      .getByRole('button', { name: /view detail/i })
      .first()
      .click()

    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/dev/`),
    )

    await expect(page.locator('.detail-tabs')).toBeVisible({ timeout: 30_000 })
  })

  test('lifecycle deep-link redirects to dev editor workflow actions', async ({ page, request }) => {
    const fixture = await assertFolCatalogSeeded(request)

    await page.goto(`/templates/${fixture.templateId}?focus=lifecycle`)

    await expect(page).toHaveURL(
      new RegExp(
        `/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/dev/.*tab=authoring`,
      ),
    )
    await expect(page.locator('#dev-version-actions')).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('.detail-tabs').getByRole('tab', { name: /^template design$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page.locator('.detail-tabs').getByRole('tab', { name: /^workflow status$/i })).toHaveCount(0)
  })

  test('hub workflow status tab redirects to dev editor workflow actions', async ({ page, request }) => {
    const fixture = await assertFolCatalogSeeded(request)

    await page.goto(`/templates/${fixture.templateId}?tab=overview`)

    const tabs = page.locator('.secondary-tabs')
    await expect(tabs.getByRole('tab', { name: /^overview$/i })).toHaveAttribute('aria-selected', 'true', {
      timeout: 15_000,
    })

    await tabs.getByRole('tab', { name: /^workflow status$/i }).click()
    await expect(page).toHaveURL(
      new RegExp(
        `/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/dev/.*tab=authoring`,
      ),
      { timeout: 15_000 },
    )
    await expect(page.locator('#dev-version-actions')).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('template release line navigation (BDD-TEMPLATE-PACKAGE-NAV-001 S3/S4)', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('published version rejects variable mutation (S5)', async ({ request }) => {
    test.setTimeout(120_000)
    const fixture = await preparePublishedTemplateWithLockedReference(request)
    await assertPublishedVersionImmutable(request, fixture.templateId)
  })

  test('published release opens read-only detail from hub', async ({ page, request }) => {
    test.setTimeout(120_000)
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await page.goto(`/templates/${fixture.templateId}`)

    await expect(page.locator('.version-lines-card')).toBeVisible()
    await expect(page.getByText(/release 1\.0\.0/i)).toBeVisible()

    await page
      .locator('.version-lines-card')
      .getByRole('button', { name: /view detail/i })
      .first()
      .click()

    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/releases/1\\.0\\.0`),
    )
    await expect(page.getByText(/published release snapshot — read-only/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /clone to new draft/i })).toBeVisible()
  })

  test('clone published release opens new dev editor', async ({ page, request }) => {
    test.setTimeout(120_000)
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await page.goto(`/templates/${fixture.templateId}`)

    await page.locator('[data-version-line-clone]').click()

    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/dev/`),
      { timeout: 30_000 },
    )
    await expect(page.locator('.detail-tabs')).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('.version-lines-card')).not.toBeVisible()
  })

  test('clone blocked while in-flight dev exists (S7)', async ({ page, request }) => {
    test.setTimeout(120_000)
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await cloneReleaseVersionApi(request, fixture.templateId, '1.0.0', 201)

    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const publishedLine = lines.find((line) => line.lineKind === 'PUBLISHED')
    expect(publishedLine?.cloneable).toBe(false)

    await page.goto(`/templates/${fixture.templateId}`)
    await expect(page.locator('[data-version-line-clone]')).toHaveCount(0)

    await assertCloneBlockedWhenInFlight(request, fixture.templateId, '1.0.0')
  })

  test('cross-group author receives access denied (S6)', async ({ request }) => {
    test.setTimeout(120_000)
    const fixture = await preparePublishedTemplateWithLockedReference(request)
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const publishedLine = lines.find((line) => line.lineKind === 'PUBLISHED' && line.releaseVersion === '1.0.0')
    if (!publishedLine) {
      throw new Error('Expected published 1.0.0 version line for S6 fixture')
    }

    await assertCrossGroupVersionLineAccessDenied(
      request,
      fixture.templateId,
      publishedLine.devVersionId,
      '1.0.0',
    )
  })
})

test.describe('template release global admin (BDD-TEMPLATE-PACKAGE-NAV-001 S8)', () => {
  test('global admin can list version lines and clone published release', async ({ page, request }) => {
    test.setTimeout(120_000)
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await loginAsGlobalAdmin(page)
    await page.goto(`/templates/${fixture.templateId}`)

    await expect(page.locator('.version-lines-card')).toBeVisible()
    await expect(page.getByText(/release 1\.0\.0/i)).toBeVisible()

    await page.locator('[data-version-line-clone]').click()
    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/dev/`),
      { timeout: 30_000 },
    )
  })
})
