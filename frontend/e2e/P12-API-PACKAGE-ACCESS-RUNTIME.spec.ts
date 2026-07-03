import { expect, test } from '@playwright/test'

import { DEMO_FULL_FLOW_EXTERNAL_ID } from './helpers/auth'
import {
  createTemplateApiCredential,
  ensureDemoFullFlowPublished,
  fetchCallerInvocationDetail,
  fetchCallerInvocations,
  fetchRecentManagementInvocations,
  runtimeGenerateDefault,
} from './helpers/content-modules-api'

test.describe('P12 API invocations runtime (BDD S5/S6)', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeAll(async ({ request }) => {
    let backendReady = false
    try {
      backendReady = (await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })).ok()
    } catch {
      backendReady = false
    }
    test.skip(!backendReady, 'Backend :8080 required for runtime invocation E2E.')
  })

  test('BDD S5 — runtime generate writes caller-visible invocation with parameters', async ({
    request,
  }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    const credential = await createTemplateApiCredential(request, fixture.templateId)
    const idempotencyKey = `e2e-p12-s5-${Date.now()}`

    const generateResult = await runtimeGenerateDefault(
      request,
      DEMO_FULL_FLOW_EXTERNAL_ID,
      credential,
      idempotencyKey,
    )
    expect(generateResult.status).toBe(200)

    const logical = await fetchCallerInvocations(
      request,
      DEMO_FULL_FLOW_EXTERNAL_ID,
      credential,
      'logical',
    )
    expect(logical.items.length).toBeGreaterThan(0)
    const invocationId = logical.items[0]?.invocationId
    expect(invocationId).toMatch(/^INV-/)

    const detail = await fetchCallerInvocationDetail(
      request,
      DEMO_FULL_FLOW_EXTERNAL_ID,
      credential,
      invocationId,
    )
    expect(detail.parameters).toMatchObject({ variables: { customerName: 'Bob' } })

    const adminRows = await fetchRecentManagementInvocations(request, fixture.templateId, 10)
    const adminMatch = adminRows.find((row) => row.invocationId === invocationId)
    expect(adminMatch).toBeDefined()
    expect(adminMatch).not.toHaveProperty('parameters')
  })
})
