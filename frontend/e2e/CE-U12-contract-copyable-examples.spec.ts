/**
 * CE-U12 — Caller contract copyable examples (curl + test-dataset payload + copy)
 * BDD: docs/behavior/ce-u12-contract-copyable-examples.md
 *   BDD-CE-U12-CCE-001 … BDD-CE-U12-CCE-007
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
import { selectElementPlusOption } from './helpers/ui'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const ACCESS_TOKEN_PLACEHOLDER = '<ACCESS_TOKEN>'
const IDEMPOTENCY_KEY_PLACEHOLDER = '<IDEMPOTENCY_KEY>'

const DATASET_A_NAME = `E2E-U12 Sample A ${Date.now().toString(36)}`
const DATASET_B_NAME = `E2E-U12 Sample B ${Date.now().toString(36)}`
const CUSTOMER_A = 'E2E-U12-Acme'
const CUSTOMER_B = 'E2E-U12-BetaCorp'

/** DOM innerText may use CRLF on Windows while clipboard keeps LF from the source string. */
function normalizeNewlines(value: string): string {
  return value.replace(/\r\n/g, '\n')
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

/** Clears the selected test data set so payload falls back to empty variables skeleton (CCE-006). */
async function clearTestDataSetSelection(page: Page): Promise<void> {
  const select = page.getByTestId('contract-example-dataset')
  await expect(select).toBeVisible()
  await select.hover()
  const clearBtn = select.locator('.el-select__clear')
  await expect(clearBtn).toBeVisible({ timeout: 5_000 })
  await clearBtn.click()
  await expect(page.getByTestId('contract-example-empty-dataset')).toBeVisible({ timeout: 10_000 })
}

async function selectContractEnvironment(page: Page, label: RegExp): Promise<void> {
  const envSelect = page.locator('.caller-contract-panel .environment-select')
  await envSelect.click()
  await selectElementPlusOption(page, label)
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
}

test.describe('CE-U12 contract copyable examples (BDD-CE-U12-CCE)', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  let templateId: string
  let dataSetAId: string
  let dataSetBId: string

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })

    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId

    const dataSetA = await createTestDataSet(request, templateId, {
      name: DATASET_A_NAME,
      required: false,
      variables: { customerName: CUSTOMER_A },
      scenarioName: 'CE-U12-A',
    })
    const dataSetB = await createTestDataSet(request, templateId, {
      name: DATASET_B_NAME,
      required: false,
      variables: { customerName: CUSTOMER_B },
      scenarioName: 'CE-U12-B',
    })
    dataSetAId = dataSetA.testDataSetId
    dataSetBId = dataSetB.testDataSetId
  })

  test.afterAll(async ({ request }) => {
    await cleanupDataSets(request, templateId, [dataSetAId, dataSetBId])
  })

  test.beforeEach(async ({ page, context }) => {
    await context.grantPermissions(['clipboard-read', 'clipboard-write'], {
      origin: FRONTEND_BASE_URL,
    })
    await loginAs(page, E2E_GROUP_ADMIN)
  })

  test('BDD-CE-U12-CCE-001 — curl includes Auth, Idempotency-Key, POST, generate URL', async ({
    page,
  }) => {
    await openCallerContractExamples(page, templateId)

    const curl = page.getByTestId('contract-example-curl')
    await expect(curl).toBeVisible()
    const curlText = await curl.innerText()

    expect(curlText).toContain('curl -X POST')
    expect(curlText).toContain(`Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}`)
    expect(curlText).toContain(`Idempotency-Key: ${IDEMPOTENCY_KEY_PLACEHOLDER}`)
    expect(curlText).toMatch(/\/generate/i)
    expect(curlText).not.toBe('generate-sync-docx')
    expect(curlText.length).toBeGreaterThan(40)
  })

  test('BDD-CE-U12-CCE-002 — payload reflects selected test data set without path fields', async ({
    page,
  }) => {
    await openCallerContractExamples(page, templateId)
    await selectTestDataSet(page, DATASET_A_NAME)

    const payload = page.getByTestId('contract-example-payload')
    await expect(payload).toContainText(`"customerName": "${CUSTOMER_A}"`)
    const payloadText = await payload.innerText()
    expect(payloadText).not.toMatch(/"templateId"/)
    expect(payloadText).not.toMatch(/"releaseVersion"/)
  })

  test('BDD-CE-U12-CCE-003 — switching data set updates payload', async ({ page }) => {
    await openCallerContractExamples(page, templateId)
    await selectTestDataSet(page, DATASET_A_NAME)
    await expect(page.getByTestId('contract-example-payload')).toContainText(CUSTOMER_A)

    await selectTestDataSet(page, DATASET_B_NAME)
    const payload = page.getByTestId('contract-example-payload')
    await expect(payload).toContainText(CUSTOMER_B)
    await expect(payload).not.toContainText(CUSTOMER_A)
  })

  test('BDD-CE-U12-CCE-004 — Copy curl writes full curl and shows success feedback', async ({
    page,
  }) => {
    await openCallerContractExamples(page, templateId)
    await selectTestDataSet(page, DATASET_A_NAME)

    const curlText = normalizeNewlines(
      (await page.getByTestId('contract-example-curl').innerText()).trim(),
    )
    await page.getByTestId('contract-copy-curl').click()

    await expect(page.getByText(/copied to clipboard/i)).toBeVisible({ timeout: 10_000 })
    const clipboard = normalizeNewlines(await page.evaluate(() => navigator.clipboard.readText()))
    expect(clipboard).toBe(curlText)
    expect(clipboard).toContain(`Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}`)
    expect(clipboard).toContain(`Idempotency-Key: ${IDEMPOTENCY_KEY_PLACEHOLDER}`)
  })

  test('BDD-CE-U12-CCE-005 — Copy payload writes JSON without real secrets', async ({ page }) => {
    await openCallerContractExamples(page, templateId)
    await selectTestDataSet(page, DATASET_A_NAME)

    const payloadText = normalizeNewlines(
      (await page.getByTestId('contract-example-payload').innerText()).trimEnd(),
    )
    await page.getByTestId('contract-copy-payload').click()

    await expect(page.getByText(/copied to clipboard/i)).toBeVisible({ timeout: 10_000 })
    const clipboard = normalizeNewlines(await page.evaluate(() => navigator.clipboard.readText()))
    expect(clipboard).toBe(payloadText)
    expect(clipboard).toContain(CUSTOMER_A)
    expect(clipboard).toContain(IDEMPOTENCY_KEY_PLACEHOLDER)
    expect(clipboard).not.toMatch(/eyJ[A-Za-z0-9_-]+\./)
    expect(clipboard).not.toContain('ChangeMe123!')
  })

  test('BDD-CE-U12-CCE-006 — no test data set selected shows guidance; curl still copyable', async ({
    page,
  }) => {
    await openCallerContractExamples(page, templateId)
    await selectTestDataSet(page, DATASET_A_NAME)
    await clearTestDataSetSelection(page)

    await expect(page.getByTestId('contract-example-empty-dataset')).toContainText(
      /no test data set selected/i,
    )
    await expect(page.getByTestId('contract-example-payload')).toContainText('"variables": {}')

    const curlText = normalizeNewlines(
      (await page.getByTestId('contract-example-curl').innerText()).trim(),
    )
    await page.getByTestId('contract-copy-curl').click()
    await expect(page.getByText(/copied to clipboard/i)).toBeVisible({ timeout: 10_000 })
    const clipboard = normalizeNewlines(await page.evaluate(() => navigator.clipboard.readText()))
    expect(clipboard).toBe(curlText)
    expect(clipboard).toContain(`Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}`)
  })

  test('BDD-CE-U12-CCE-007 — environment switch regenerates curl URL with Auth headers', async ({
    page,
  }) => {
    await openCallerContractExamples(page, templateId)

    // Default panel environment is typically UAT; assert then switch to Prod.
    const curlBefore = await page.getByTestId('contract-example-curl').innerText()
    expect(curlBefore).toMatch(/\/api\/(uat|dev|prod)\//i)

    await selectContractEnvironment(page, /^production$|^prod$|^生产/i)
    await expect(page.locator('.caller-contract-panel .el-skeleton')).toHaveCount(0, {
      timeout: 30_000,
    })
    await expect(page.getByTestId('contract-copyable-example')).toBeVisible({ timeout: 30_000 })

    const curl = page.getByTestId('contract-example-curl')
    await expect(curl).toContainText(/\/api\/prod\//i, { timeout: 30_000 })
    const curlText = await curl.innerText()
    expect(curlText).toContain(`Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}`)
    expect(curlText).toContain(`Idempotency-Key: ${IDEMPOTENCY_KEY_PLACEHOLDER}`)
    expect(curlText).toMatch(/\/generate/i)
  })
})

async function cleanupDataSets(
  request: APIRequestContext,
  templateId: string | undefined,
  ids: Array<string | undefined>,
): Promise<void> {
  if (!templateId) {
    return
  }
  for (const id of ids) {
    if (!id) {
      continue
    }
    try {
      await deleteTestDataSet(request, templateId, id)
    } catch {
      // Best-effort cleanup for shared demo template.
    }
  }
}
