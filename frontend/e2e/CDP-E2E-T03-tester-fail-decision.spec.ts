import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_AUTHOR, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import {
  listCollaborationWorkItems,
  prepareRetailTemplateInTesting,
  type TestingTemplateFixture,
} from './helpers/collaboration-api'
import { confirmTestFailAfterTesterOpen } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { fetchTemplateDetail } from './helpers/submit-approval-gate-api'
import {
  dashboardTaskRow,
  filterDashboardTasksByItem,
  reLoginAs,
} from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('CDP-E2E-T03 Tester structured fail decision (BDD-CDP-TEST-002)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: TestingTemplateFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    // API setup only — fail decision clicks are browser-only below.
    fixture = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-CDP-T03-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CDP Tester Fail ${Date.now().toString(36).toUpperCase()}`,
    })

    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('TESTING')
  })

  test('tester Fail with reason + remediation returns DRAFT and author REMEDIATION todo', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await filterDashboardTasksByItem(page, fixture.name)
    const testRow = await dashboardTaskRow(page, fixture.name)
    await expect(testRow).toBeVisible({ timeout: 30_000 })
    await testRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

    await confirmTestFailAfterTesterOpen(page, {
      reasonCategoryLabel: /binding or layout placeholder issue/i,
      impactSummary:
        'Header binding invalid — author must fix layout placeholder and re-run full test.',
      remediationChecklistCode: 'ANCHOR_INTEGRITY',
    })

    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('DRAFT')

    const testItemsAfter = await listCollaborationWorkItems(request, E2E_TEMPLATE_TESTER, {
      queue: 'TEST',
    })
    expect(testItemsAfter.some((item) => item.templateId === fixture.templateId)).toBeFalsy()

    const remediationItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_AUTHOR, {
      queue: 'REMEDIATION',
    })
    expect(remediationItems.some((item) => item.templateId === fixture.templateId)).toBeTruthy()

    // Author dashboard shows the remediation collaboration todo.
    await reLoginAs(page, loginAs, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard?queue=REMEDIATION#tasks-section')
    await filterDashboardTasksByItem(page, fixture.name)
    const remediationRow = await dashboardTaskRow(page, fixture.name)
    await expect(remediationRow).toBeVisible({ timeout: 30_000 })
    await expect(remediationRow.getByRole('button', { name: /^open$/i })).toBeVisible()
  })
})
