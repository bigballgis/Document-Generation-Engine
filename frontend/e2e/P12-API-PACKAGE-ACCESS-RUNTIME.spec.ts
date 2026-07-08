import { expect, test } from '@playwright/test'

import { DEMO_FULL_FLOW_EXTERNAL_ID } from './helpers/auth'
import {
  createTemplateApiCredential,
  ensureDemoFullFlowPublished,
  fetchCallerInvocationDetail,
  fetchCallerInvocations,
  fetchRecentManagementInvocations,
  runtimeBatchGenerateDefault,
  runtimeGenerateDefault,
  updateApiPolicyBatchSettings,
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

  test('BDD S7 — batch logical root vs flat item rows', async ({ request }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    await updateApiPolicyBatchSettings(request, fixture.templateId, true, 10)
    const credential = await createTemplateApiCredential(request, fixture.templateId)
    const idempotencyKey = `e2e-p12-s7-${Date.now()}`

    const batchResult = await runtimeBatchGenerateDefault(
      request,
      DEMO_FULL_FLOW_EXTERNAL_ID,
      credential,
      idempotencyKey,
      3,
    )
    expect(batchResult.status).toBe(200)
    expect(batchResult.batchId).toBeTruthy()

    const logical = await fetchCallerInvocations(
      request,
      DEMO_FULL_FLOW_EXTERNAL_ID,
      credential,
      'logical',
    )
    const logicalRoots = logical.items.filter((item) => item.invocationKind === 'BATCH_ROOT')
    expect(logicalRoots).toHaveLength(1)
    expect(logicalRoots[0]?.batchId).toBe(batchResult.batchId)

    const flat = await fetchCallerInvocations(
      request,
      DEMO_FULL_FLOW_EXTERNAL_ID,
      credential,
      'flat',
    )
    const flatItems = flat.items.filter((item) => item.invocationKind === 'BATCH_ITEM')
    expect(flatItems).toHaveLength(3)
    expect(flat.items.some((item) => item.invocationKind === 'BATCH_ROOT')).toBe(false)
    expect(new Set(flatItems.map((item) => item.batchId))).toEqual(new Set([batchResult.batchId]))
  })
})
