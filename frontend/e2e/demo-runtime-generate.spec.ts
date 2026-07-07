/**
 * Demo high-fidelity runtime generate (BDD-DEMO-EXP-009 / BDD-DEMO-EXP-013).
 *
 * Prerequisite: Docker stack up and demos imported + published.
 *   .\\scripts\\docker-deploy.ps1
 *   .\\deploy\\import-all-demos.ps1 -BackendUrl http://localhost:8080
 *
 * Tests skip (not fail) when a template is missing or not PUBLISHED.
 */
import { expect, test } from '@playwright/test'

import { DEMO_FULL_FLOW_EXTERNAL_ID } from './helpers/auth'
import {
  assertDocxArtifact,
  DEMO_RUNTIME_CASES,
  ensurePublishedDemoWithCredential,
  runtimeGenerateDocx,
} from './helpers/demo-runtime-api'

test.describe('Demo high-fidelity runtime generate (BDD-DEMO-EXP-009/013)', () => {
  test.beforeAll(async ({ request }) => {
    let backendReady = false
    try {
      backendReady = (await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })).ok()
    } catch {
      backendReady = false
    }
    test.skip(!backendReady, 'Backend :8080 required for demo runtime generate E2E.')
  })

  for (const demoCase of DEMO_RUNTIME_CASES) {
    test(`runtime generate — ${demoCase.externalId}`, async ({ request }) => {
      const published = await ensurePublishedDemoWithCredential(request, demoCase.externalId)
      if (!published.ok) {
        test.skip(true, published.reason)
        return
      }

      const idempotencyKey = `e2e-demo-runtime-${demoCase.externalId}-${Date.now()}`
      const result = await runtimeGenerateDocx(
        request,
        demoCase.externalId,
        published.credential,
        demoCase.loadVariables(),
        idempotencyKey,
      )

      expect(result.status).toBe(200)
      expect(result.documentId).toBeTruthy()
      assertDocxArtifact(result.body, {
        minBytes: demoCase.minDocxBytes,
        contentMarkers: demoCase.contentMarkers,
      })
    })
  }

  test(`runtime generate — ${DEMO_FULL_FLOW_EXTERNAL_ID} (published catalog)`, async ({ request }) => {
    const published = await ensurePublishedDemoWithCredential(request, DEMO_FULL_FLOW_EXTERNAL_ID)
    if (!published.ok) {
      test.skip(true, published.reason)
      return
    }

    const idempotencyKey = `e2e-demo-runtime-${DEMO_FULL_FLOW_EXTERNAL_ID}-${Date.now()}`
    const result = await runtimeGenerateDocx(
      request,
      DEMO_FULL_FLOW_EXTERNAL_ID,
      published.credential,
      { customerName: 'Bob' },
      idempotencyKey,
    )

    expect(result.status).toBe(200)
    expect(result.documentId).toBeTruthy()
    assertDocxArtifact(result.body, {
      minBytes: 2_048,
      contentMarkers: ['Bob'],
    })
  })
})
