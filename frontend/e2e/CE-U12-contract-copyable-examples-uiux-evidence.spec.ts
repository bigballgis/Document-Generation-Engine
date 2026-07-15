/**
 * CE-U12 UIUX evidence — caller contract copyable examples (curl + payload + copy)
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/ce-u12-contract-copyable-examples.md (CCE-001…007 surfaces)
 */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { ensureDemoFullFlowPublished } from './helpers/content-modules-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  createTestDataSet,
  deleteTestDataSet,
} from './helpers/template-testing-api'
import {
  captureCeU12LocatorScreenshot,
  captureCeU12Screenshot,
  CE_U12_VIEWPORT,
  ensureCeU12EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const DATASET_NAME = `E2E-U12-UIUX Sample ${Date.now().toString(36)}`
const CUSTOMER = 'E2E-U12-UIUX-Acme'

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openCallerContractExamples(page: Page, templateId: string): Promise<void> {
  await page.goto(`/templates/${templateId}?tab=apiAccess`)
  await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.locator('.api-access-layout')).toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const contractCollapse = page.locator('.contract-collapse .el-collapse-item').first()
  await expect(contractCollapse.getByText(/caller contract|调用方契约/i)).toBeVisible()
  if (!(await contractCollapse.evaluate((el) => el.classList.contains('is-active')))) {
    await contractCollapse.getByRole('button', { name: /caller contract|调用方契约/i }).click()
  }
  await expect(page.getByTestId('contract-copyable-example')).toBeVisible({ timeout: 30_000 })
}

async function selectTestDataSet(page: Page, name: string): Promise<void> {
  const select = page.getByTestId('contract-example-dataset')
  await select.click()
  const dropdown = page.locator('.el-select-dropdown:visible')
  await expect(dropdown).toBeVisible()
  await dropdown.getByRole('option', { name }).click()
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
  await expect(select).toContainText(name)
}

async function clearTestDataSetSelection(page: Page): Promise<void> {
  const select = page.getByTestId('contract-example-dataset')
  await select.hover()
  const clearBtn = select.locator('.el-select__clear')
  await expect(clearBtn).toBeVisible({ timeout: 5_000 })
  await clearBtn.click()
  await expect(page.getByTestId('contract-example-empty-dataset')).toBeVisible({ timeout: 10_000 })
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

test.describe('CE-U12 contract copyable examples UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  let templateId: string
  let dataSetId: string

  test.beforeAll(async ({ request }) => {
    ensureCeU12EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })

    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId

    const dataSet = await createTestDataSet(request, templateId, {
      name: DATASET_NAME,
      required: false,
      variables: { customerName: CUSTOMER },
      scenarioName: 'CE-U12-UIUX',
    })
    dataSetId = dataSet.testDataSetId
  })

  test.afterAll(async ({ request }) => {
    await cleanupDataSet(request, templateId, dataSetId)
  })

  test('01–06 dual-brand: examples with dataset + empty hint + copy affordances', async ({
    page,
    context,
  }) => {
    await page.setViewportSize(CE_U12_VIEWPORT)
    await context.grantPermissions(['clipboard-read', 'clipboard-write'], {
      origin: FRONTEND_BASE_URL,
    })

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openCallerContractExamples(page, templateId)
    await selectTestDataSet(page, DATASET_NAME)

    const example = page.getByTestId('contract-copyable-example')
    await expect(example.getByTestId('contract-example-curl')).toContainText('curl -X POST')
    await expect(example.getByTestId('contract-example-payload')).toContainText(CUSTOMER)
    await expect(page.getByTestId('contract-copy-curl')).toBeVisible()
    await expect(page.getByTestId('contract-copy-payload')).toBeVisible()
    await assertNoViewportOverflow(page)

    await captureCeU12Screenshot(page, '01-contract-examples-dataset-redbc-1920x1080.png')
    await captureCeU12LocatorScreenshot(
      example,
      '01b-copyable-example-crop-redbc-1920x1080.png',
    )
    await captureCeU12LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )

    await page.getByTestId('contract-copy-curl').click()
    await expect(page.getByText(/copied to clipboard/i)).toBeVisible({ timeout: 10_000 })
    await captureCeU12Screenshot(page, '02-copy-curl-feedback-redbc-1920x1080.png')

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.getByTestId('contract-copyable-example')).toBeVisible()
    await assertNoViewportOverflow(page)
    await captureCeU12Screenshot(page, '03-contract-examples-dataset-greenbc-1920x1080.png')
    await captureCeU12LocatorScreenshot(
      page.getByTestId('contract-copyable-example'),
      '03b-copyable-example-crop-greenbc-1920x1080.png',
    )
    await captureCeU12LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03c-brand-header-greenbc-crop.png',
    )

    await switchBrand(page, 'REDBC')
    await clearTestDataSetSelection(page)
    await expect(page.getByTestId('contract-example-empty-dataset')).toContainText(
      /no test data set selected/i,
    )
    await expect(page.getByTestId('contract-example-payload')).toContainText('"variables": {}')
    await assertNoViewportOverflow(page)
    await captureCeU12Screenshot(page, '04-empty-dataset-hint-redbc-1920x1080.png')
    await captureCeU12LocatorScreenshot(
      page.getByTestId('contract-copyable-example'),
      '04b-empty-dataset-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.getByTestId('contract-example-empty-dataset')).toBeVisible()
    await captureCeU12Screenshot(page, '05-empty-dataset-hint-greenbc-1920x1080.png')
    await captureCeU12LocatorScreenshot(
      page.getByTestId('contract-copyable-example'),
      '05b-empty-dataset-crop-greenbc-1920x1080.png',
    )

    await expect(page.getByTestId('contract-copy-curl')).toBeVisible()
    await page.getByTestId('contract-copy-curl').focus()
    await expect(page.getByTestId('contract-copy-curl')).toBeFocused()
    await captureCeU12LocatorScreenshot(
      page.getByTestId('contract-copyable-example'),
      '06-copy-curl-focus-greenbc-crop.png',
    )
  })
})

async function cleanupDataSet(
  request: APIRequestContext,
  templateId: string | undefined,
  id: string | undefined,
): Promise<void> {
  if (!templateId || !id) {
    return
  }
  try {
    await deleteTestDataSet(request, templateId, id)
  } catch {
    // Best-effort cleanup for shared demo template.
  }
}
