/**
 * KEEP-8 runtime generate E2E (FOS-W14-1 / BDD-DEMO-TYP-011/012).
 *
 * Prerequisite: Docker stack up and demos imported + published.
 *   pwsh ./scripts/docker-deploy-queue.ps1
 *   ./deploy/import-all-demos.sh   # or pwsh ./deploy/import-all-demos.ps1
 *   ./deploy/publish-all-demos.sh
 *
 * Canonical run: pnpm -C frontend test:e2e:docker:demos
 *
 * FOS-W14-1: KEEP-8 templates **fail** (not skip) when missing / not PUBLISHED.
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

  test('publish registry has keep-set of 8 runtime cases', () => {
    expect(DEMO_PUBLISH_EXTERNAL_IDS).toHaveLength(8)
    expect(DEMO_RUNTIME_CASES).toHaveLength(8)
    expect(DEMO_RUNTIME_CASES.map((demoCase) => demoCase.externalId)).toEqual([...DEMO_PUBLISH_EXTERNAL_IDS])
  })

  for (const demoCase of DEMO_RUNTIME_CASES) {
    test(`runtime generate — ${demoCase.externalId}`, async ({ request }) => {
      const published = await ensurePublishedDemoWithCredential(request, demoCase.externalId)
      if (!published.ok) {
        throw new Error(
          `FOS-W14-1 KEEP-8 fail-closed: ${published.reason}. `
            + 'Import/publish KEEP-8 demos before running test:e2e:docker:demos.',
        )
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
