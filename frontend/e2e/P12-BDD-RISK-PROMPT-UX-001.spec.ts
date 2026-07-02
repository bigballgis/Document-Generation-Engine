import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import { prepareRetailTemplateInTesting } from './helpers/collaboration-api'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  ALL_REASON_CATEGORIES,
  getDecisionFormConfig,
  getTemplateRiskPromptConfig,
  prepareTestingTemplateWithRiskPromptOverride,
  resolveDevEditorTestPreviewPath,
} from './helpers/risk-prompt-config-api'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

async function openTestPreviewWorkflow(
  page: import('@playwright/test').Page,
  request: import('@playwright/test').APIRequestContext,
  fixture: import('./helpers/collaboration-api').TestingTemplateFixture,
) {
  await page.goto(await resolveDevEditorTestPreviewPath(request, fixture))
  await page.waitForLoadState('networkidle', { timeout: 30_000 })
  await expect(page).not.toHaveURL(/\/forbidden/)
  await expect(page).toHaveURL(/authoringTab=testPreview/)
  const workflow = page.locator('.test-preview-workflow')
  await expect(workflow).toBeVisible({ timeout: 30_000 })
  const failButton = workflow.getByRole('button', { name: /^record test failure$/i })
  await expect(failButton).toBeVisible({ timeout: 30_000 })
  return workflow
}

function testFailDecisionDialog(page: import('@playwright/test').Page) {
  return page.getByRole('dialog', { name: /record test failure/i })
}

async function openReasonCategoryDropdown(page: import('@playwright/test').Page) {
  const dialog = testFailDecisionDialog(page)
  await expect(dialog).toBeVisible()
  const select = dialog.getByRole('combobox', { name: /reason category/i })
  await select.click()
  return page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
}

test.describe('P12-BDD-RISK-PROMPT-UX-001 template risk-prompt UX (BDD S1–S7, S11–S12)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    let backendReady = false
    let frontendReady = false
    try {
      backendReady = (await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })).ok()
    } catch {
      backendReady = false
    }
    try {
      frontendReady = (await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })).ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )

    try {
      await assertDemoCatalogSeeded(request)
    } catch (error) {
      test.skip(true, error instanceof Error ? error.message : String(error))
    }
  })

  test('S11 — list view has no risk-prompt panel; create dialog advanced section collapsed', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/templates')

    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByText(/^test and approval return reasons$/i)).toHaveCount(0)

    await page.getByRole('button', { name: /new template package/i }).click()
    const createDialog = page.getByRole('dialog', { name: /^create template$/i })
    await expect(createDialog).toBeVisible({ timeout: 15_000 })

    const collapseHeader = createDialog.getByRole('button', {
      name: /test and approval return reasons \(optional\)/i,
    })
    await expect(collapseHeader).toBeVisible()
    await expect(
      createDialog.getByText(/customize return reasons for this template/i),
    ).not.toBeVisible()
  })

  test('S1/S3 — template without override inherits global categories in decision dialog', async ({
    page,
    request,
  }) => {
    const fixture = await prepareRetailTemplateInTesting(request)
    const config = await getTemplateRiskPromptConfig(request, fixture.templateId, fixture.groupCode)
    expect(config.useDefault).toBe(true)
    expect(config.reasonCategories).toHaveLength(ALL_REASON_CATEGORIES.length)

    const decisionConfig = await getDecisionFormConfig(request, fixture.templateId, fixture.groupCode)
    expect(decisionConfig.reasonCategories).toHaveLength(ALL_REASON_CATEGORIES.length)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    const workflow = await openTestPreviewWorkflow(page, request, fixture)
    await workflow.getByRole('button', { name: /^record test failure$/i }).click()

    const options = await openReasonCategoryDropdown(page)
    await expect(options).toHaveCount(ALL_REASON_CATEGORIES.length)
    await expect(options.filter({ hasText: /^binding or layout placeholder issue$/i })).toHaveCount(1)
    await expect(options.filter({ hasText: /^BINDING_ISSUE$/i })).toHaveCount(0)
  })

  test('S2/S7 — template override subset filters decision dialog categories', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTestingTemplateWithRiskPromptOverride(request, [
      'COVERAGE_BELOW_THRESHOLD',
    ])

    const decisionConfig = await getDecisionFormConfig(request, fixture.templateId, fixture.groupCode)
    expect(decisionConfig.reasonCategories).toEqual(['COVERAGE_BELOW_THRESHOLD'])

    await loginAs(page, E2E_TEMPLATE_TESTER)
    const workflow = await openTestPreviewWorkflow(page, request, fixture)
    await workflow.getByRole('button', { name: /^record test failure$/i }).click()

    const options = await openReasonCategoryDropdown(page)
    await expect(options).toHaveCount(1)
    await expect(options).toHaveText(/coverage below threshold/i)
    await expect(options.filter({ hasText: /^COVERAGE_BELOW_THRESHOLD$/i })).toHaveCount(0)
  })

  test('S12 — BINDING_ISSUE copy distinguishes return reason from submission readiness gate', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/templates')
    await page.getByRole('button', { name: /new template package/i }).click()
    const createDialog = page.getByRole('dialog', { name: /^create template$/i })
    await expect(createDialog).toBeVisible({ timeout: 15_000 })

    await createDialog.getByRole('button', { name: /test and approval return reasons \(optional\)/i }).click()
    await expect(createDialog.getByText(/does not control whether approval can be submitted/i)).toBeVisible()
    await createDialog.locator('.el-checkbox').filter({ hasText: /customize return reasons for this template/i }).click()

    const bindingRow = createDialog
      .locator('.category-row')
      .filter({ hasText: /binding or layout placeholder issue/i })
    await expect(bindingRow).toBeVisible()
    await bindingRow.locator('.context-help-trigger').click()
    const bindingHelpPopover = page.locator('.context-help-popover').filter({
      hasText: /does not affect submit readiness/i,
    })
    await expect(bindingHelpPopover).toBeVisible()
    await expect(bindingHelpPopover).toContainText(/return reason/i)
  })
})
