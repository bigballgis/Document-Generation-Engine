/**
 * P23-T13 — Runtime generate E2E for all published bank-grade demos.
 *
 * BDD: BDD-DEMO-TYP-011 (all demo families import + generate),
 *      BDD-DEMO-TYP-012 (DOCX size floor per template).
 *
 * Prerequisite: Docker stack up and demos imported + published.
 *   .\\scripts\\docker-deploy.ps1
 *   .\\deploy\\import-all-demos.ps1 -BackendUrl http://localhost:8080
 *   .\\deploy\\publish-all-demos.ps1 -BackendUrl http://localhost:8080
 *
 * Canonical run: pnpm -C frontend test:e2e:docker:demos
 *
 * Tests skip (not fail) when a template is missing or not PUBLISHED.
 */
import { expect, test } from '@playwright/test'

import { requireBackendReady } from './helpers/stack-readiness'

import {
  assertDocxArtifact,
  DEMO_PUBLISH_EXTERNAL_IDS,
  DEMO_RUNTIME_CASES,
  ensurePublishedDemoWithCredential,
  runtimeGenerateDocx,
} from './helpers/demo-runtime-api'

test.describe('Demo bank-grade runtime generate (BDD-DEMO-TYP-011/012)', () => {
  test.beforeAll(async ({ request }) => {
    await requireBackendReady(request, {
      skipMessage: 'Backend :8080 required for demo runtime generate E2E.',
    })
  })

  test('publish registry has 20 runtime cases', () => {
    expect(DEMO_PUBLISH_EXTERNAL_IDS).toHaveLength(20)
    expect(DEMO_RUNTIME_CASES).toHaveLength(20)
    expect(DEMO_RUNTIME_CASES.map((demoCase) => demoCase.externalId)).toEqual([...DEMO_PUBLISH_EXTERNAL_IDS])
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
      expect(result.documentId, 'documentId header required for audit correlation (BDD-DEMO-TYP-012)').toBeTruthy()
      assertDocxArtifact(result.body, {
        minBytes: demoCase.minDocxBytes,
        contentMarkers: demoCase.contentMarkers,
        requireDocumentXml: true,
      })
    })
  }
})
