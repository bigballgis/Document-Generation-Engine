/**
 * CE-U13 — Variable rename cascade + conditionExpression autocomplete
 * BDD: docs/behavior/ce-u13-variable-rename.md (BDD-CE-U13-VRC-001…012)
 *
 *   pnpm -C frontend exec playwright test e2e/CE-U13-variable-rename.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 */
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import {
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  attemptVariableWriteAsTester,
  fetchTemplateDetailViaApi,
  fetchTestDataSetsViaApi,
  prepareCeU13CascadeFixture,
  type CeU13Fixture,
} from './helpers/ce-u13-variable-rename-api'
import { openDevBindingEditor } from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import { confirmMessageBox } from './helpers/ui'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'CE-U13-variable-rename',
)

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 2_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openDevVariablesPanel(
  page: Page,
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight) {
    throw new Error(`No in-flight dev version for template ${templateId}`)
  }
  await page.goto(
    `/templates/${templateId}/dev/${inFlight.devVersionId}?workspaceTab=design&designTab=variables`,
  )
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
  await dismissOnboardingTourIfPresent(page)
  const designSubTabs = page.locator('.design-sub-tabs')
  await designSubTabs.getByRole('tab', { name: /^variables$/i }).click()
  const panel = page.locator('.variable-tree-panel').or(page.getByTestId('variable-tree-panel'))
  await expect(panel).toBeVisible({ timeout: 30_000 })
}

async function openEditVariable(page: Page, variableKey: string): Promise<void> {
  const panel = page.locator('.variable-tree-panel').or(page.getByTestId('variable-tree-panel'))
  const search = panel.locator('.search-input input')
  await search.fill(variableKey)
  const node = panel.locator('.tree-node').filter({
    has: page.locator('.tree-node__technical-key', { hasText: new RegExp(`^${escapeRegExp(variableKey)}$`) }),
  })
  await expect(node).toBeVisible({ timeout: 15_000 })
  await node.getByTestId('edit-variable-button').click()
  await expect(page.getByTestId('variable-key-input')).toBeVisible()
  await expect(page.getByTestId('variable-key-input')).toHaveValue(variableKey)
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function renameVariableViaUi(
  page: Page,
  oldKey: string,
  newKey: string,
  options?: { confirm?: boolean; expectLockedSkipped?: number },
): Promise<void> {
  await openEditVariable(page, oldKey)
  await page.getByTestId('variable-key-input').fill(newKey)
  await page.getByTestId('save-variable-button').click()
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible({ timeout: 15_000 })
  await expect(box).toContainText(new RegExp(`Rename "${oldKey}" to "${newKey}"`, 'i'))
  if (typeof options?.expectLockedSkipped === 'number') {
    await expect(box).toContainText(
      new RegExp(`Locked test sets skipped:\\s*${options.expectLockedSkipped}`, 'i'),
    )
  }
  if (options?.confirm === false) {
    await box.getByRole('button', { name: /^cancel$/i }).click()
    await expect(box).toHaveCount(0)
    return
  }
  await confirmMessageBox(page)
}

test.describe('CE-U13 variable rename + expression autocomplete (BDD-CE-U13-VRC)', () => {
  test.describe.configure({ mode: 'serial' })

  let fixture: CeU13Fixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    fixture = await prepareCeU13CascadeFixture(request)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('VRC-001 — edit dialog variableKey is enabled', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openDevVariablesPanel(page, request, fixture.templateId)
    await openEditVariable(page, fixture.oldKey)

    const keyInput = page.getByTestId('variable-key-input')
    await expect(keyInput).toBeVisible()
    await expect(keyInput).toBeEnabled()
    await expect(page.getByTestId('save-variable-button')).toBeVisible()

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'VRC-001-variable-key-editable.png'),
      fullPage: true,
    })
  })

  test('VRC-005 — conflicting newKey is rejected without mutation', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openDevVariablesPanel(page, request, fixture.templateId)
    await openEditVariable(page, fixture.oldKey)
    await page.getByTestId('variable-key-input').fill(fixture.otherKey)
    await page.getByTestId('save-variable-button').click()

    await expect(page.locator('.el-message').getByText(/another variable already uses this key/i)).toBeVisible({
      timeout: 10_000,
    })
    await expect(page.locator('.el-message-box')).toHaveCount(0)

    const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
    expect(detail.variables.some((v) => v.variableKey === fixture.oldKey)).toBe(true)
    expect(detail.variables.filter((v) => v.variableKey === fixture.otherKey)).toHaveLength(1)
  })

  test('VRC-006 — blank newKey is rejected', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openDevVariablesPanel(page, request, fixture.templateId)
    await openEditVariable(page, fixture.oldKey)
    await page.getByTestId('variable-key-input').fill('   ')
    await page.getByTestId('save-variable-button').click()

    await expect(page.locator('.el-message').getByText(/variable key is required/i)).toBeVisible({
      timeout: 10_000,
    })
    await expect(page.locator('.el-message-box')).toHaveCount(0)

    const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
    expect(detail.variables.some((v) => v.variableKey === fixture.oldKey)).toBe(true)
  })

  test('VRC-007 — cancel confirm leaves persistence unchanged', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openDevVariablesPanel(page, request, fixture.templateId)
    await renameVariableViaUi(page, fixture.oldKey, 'cancelledParty', { confirm: false })

    const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
    expect(detail.variables.some((v) => v.variableKey === fixture.oldKey)).toBe(true)
    expect(detail.variables.some((v) => v.variableKey === 'cancelledParty')).toBe(false)
  })

  test('VRC-002/003/004/008 — rename cascades; locked skipped; whole-token safe', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000)
    await openDevVariablesPanel(page, request, fixture.templateId)
    await renameVariableViaUi(page, fixture.oldKey, fixture.newKey, { expectLockedSkipped: 1 })

    await expect(
      page.locator('.el-message').getByText(/variable renamed and references updated/i),
    ).toBeVisible({ timeout: 30_000 })
    await expect(
      page.locator('.el-message').getByText(/locked test data set\(s\) still use the previous key/i),
    ).toBeVisible({ timeout: 15_000 })

    const panel = page.locator('.variable-tree-panel').or(page.getByTestId('variable-tree-panel'))
    await panel.locator('.search-input input').fill(fixture.newKey)
    await expect(panel.locator('.tree-node').filter({ hasText: fixture.newKey }).first()).toBeVisible({
      timeout: 15_000,
    })
    await expect(panel.getByTestId('edit-variable-button').first()).toBeVisible()

    const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
    const keys = detail.variables.map((v) => v.variableKey)
    expect(keys).toContain(fixture.newKey)
    expect(keys).not.toContain(fixture.oldKey)
    expect(keys).toContain(fixture.customerNameKey)

    const compute = detail.variables.find((v) => v.variableKey === fixture.computeKey)
    expect(compute?.computeExpression ?? '').toContain(`\${${fixture.newKey}}`)
    expect(compute?.computeExpression ?? '').not.toContain(`\${${fixture.oldKey}}`)

    const header = detail.bindings.find((b) => b.anchorId === 'HEADER')
    const bindingJson = header?.structuredContentJson ?? ''
    expect(bindingJson).toContain(`\${${fixture.newKey}}`)
    expect(bindingJson).toContain(`"key":"${fixture.newKey}"`)
    expect(bindingJson).toContain(`"key":"${fixture.customerNameKey}"`)
    expect(bindingJson).not.toMatch(new RegExp(`\\$\\{${fixture.oldKey}(?=\\}|\\.)`))
    expect(bindingJson).not.toContain(`"key":"${fixture.oldKey}"`)

    const rule = detail.rules.find((r) => r.ruleId === 'E2E-U13-RULE-CUSTOMER')
    expect(rule?.conditionExpression ?? '').toContain(`\${${fixture.newKey}}`)
    expect(rule?.conditionExpression ?? '').not.toContain(`\${${fixture.oldKey}}`)

    const sets = await fetchTestDataSetsViaApi(request, fixture.templateId)
    const unlocked = sets.find((s) => s.testDataSetId === fixture.unlockedTestDataSetId)
    const locked = sets.find((s) => s.testDataSetId === fixture.lockedTestDataSetId)
    expect(unlocked?.locked).toBe(false)
    expect(unlocked?.variables).toMatchObject({
      [fixture.newKey]: 'UnlockedValue',
      [fixture.customerNameKey]: 'KeepName',
    })
    expect(unlocked?.variables).not.toHaveProperty(fixture.oldKey)
    expect(locked?.locked).toBe(true)
    expect(locked?.variables).toMatchObject({
      [fixture.oldKey]: 'LockedValue',
      [fixture.customerNameKey]: 'KeepName',
    })

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'VRC-002-rename-cascade-success.png'),
      fullPage: true,
    })
  })

  test('VRC-012 — zero-reference rename still succeeds', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openDevVariablesPanel(page, request, fixture.templateId)
    await renameVariableViaUi(page, fixture.lonelyKey, 'soloKey')

    await expect(
      page.locator('.el-message').getByText(/variable renamed and references updated/i),
    ).toBeVisible({ timeout: 30_000 })

    const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
    const keys = detail.variables.map((v) => v.variableKey)
    expect(keys).toContain('soloKey')
    expect(keys).not.toContain(fixture.lonelyKey)
  })

  test('VRC-009 — no authorTemplates write controls; write API fail-closed', async ({
    browser,
    request,
  }) => {
    test.setTimeout(120_000)

    const denied = await attemptVariableWriteAsTester(request, fixture.templateId, fixture.newKey)
    expect(denied.status).toBe(403)
    expect(denied.body.error?.code ?? '').toMatch(/ACCESS_DENIED|FORBIDDEN/i)

    const testerContext = await browser.newContext({
      baseURL: FRONTEND_BASE_URL,
    })
    const testerPage = await testerContext.newPage()
    try {
      await loginAs(testerPage, E2E_TEMPLATE_TESTER)
      const skipTour = testerPage.getByTestId('onboarding-tour-skip')
      if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
        await skipTour.click()
      }

      try {
        await openDevVariablesPanel(testerPage, request, fixture.templateId)
      } catch {
        await expect(testerPage).toHaveURL(/forbidden|login|templates/i)
      }

      if (await testerPage.getByTestId('variable-tree-panel').isVisible().catch(() => false)) {
        await expect(testerPage.getByTestId('add-variable-button')).toHaveCount(0)
        await expect(testerPage.getByTestId('edit-variable-button')).toHaveCount(0)
        await expect(testerPage.getByTestId('save-variable-button')).toHaveCount(0)
      }
    } finally {
      await testerContext.close()
    }
  })

  test('VRC-010 — conditionBlock expression autocomplete inserts ${key}', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000)
    // Fresh fixture so bindings editor is writable after prior cascade on shared fixture.
    const local = await prepareCeU13CascadeFixture(request)
    await openDevBindingEditor(page, request, local.templateId)

    await page.getByTestId('insert-block-node').filter({ hasText: /^condition$/i }).click()
    const field = page
      .locator('[data-testid="condition-expression-field"]')
      .filter({ has: page.getByTestId('condition-expression-input') })
      .last()
    await expect(field.getByTestId('condition-expression-input')).toBeVisible({ timeout: 15_000 })

    await field.getByTestId('insert-variable-button').click()
    await expect(field.getByTestId('variable-autocomplete-list')).toBeVisible()
    await expect(
      field.getByTestId(`variable-suggestion-${local.autocompleteKeys.borrowerLegalName}`),
    ).toBeVisible()
    await field.getByTestId(`variable-suggestion-${local.autocompleteKeys.borrowerLegalName}`).click()

    await expect(field.getByTestId('condition-expression-input')).toHaveValue(
      new RegExp(`\\$\\{${local.autocompleteKeys.borrowerLegalName}\\}`),
    )

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'VRC-010-condition-autocomplete.png'),
      fullPage: true,
    })
  })

  test('VRC-011 — visibility expression autocomplete inserts schema key', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000)
    const local = await prepareCeU13CascadeFixture(request)
    await openDevBindingEditor(page, request, local.templateId)

    await page.getByText(/enable conditional visibility/i).click()
    const field = page
      .locator('[data-testid="condition-expression-field"]')
      .filter({ has: page.getByTestId('visibility-expression-input') })
    await expect(field.getByTestId('visibility-expression-input')).toBeVisible({ timeout: 10_000 })

    await field.getByTestId('insert-variable-button').click()
    await expect(field.getByTestId('variable-autocomplete-list')).toBeVisible()
    await expect(
      field.getByTestId(`variable-suggestion-${local.autocompleteKeys.showNotice}`),
    ).toBeVisible()
    await field.getByTestId(`variable-suggestion-${local.autocompleteKeys.showNotice}`).click()

    await expect(field.getByTestId('visibility-expression-input')).toHaveValue(
      new RegExp(`\\$\\{${local.autocompleteKeys.showNotice}\\}`),
    )

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'VRC-011-visibility-autocomplete.png'),
      fullPage: true,
    })
  })
})
