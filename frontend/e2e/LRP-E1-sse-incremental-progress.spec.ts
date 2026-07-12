import { expect, test } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { requireDockerStack } from './helpers/stack-readiness'
import {
  prepareCdpMvpGoldenDraft,
  type CdpMvpGoldenFixture,
} from './helpers/cdp-mvp-golden-api'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  listTestDataSets,
  openFolDevEditorTestingTab,
  previewProgressDialog,
  runPreviewFromFirstDataSetRow,
  waitForPreviewConcurrencySlot,
  waitForPreviewDialogSuccess,
  runFullTestFromUi,
  batchProgressDialog,
} from './helpers/template-testing-api'
import {
  distinctArrivalGapsMs,
  installSseFetchCapture,
  observeIdleProgressStreamHeartbeats,
  parseSseFramesFromChunks,
  readSseCapture,
} from './helpers/sse-stream-capture'

/** Docker acceptance UI (override with E2E_BASE_URL / FRONTEND_PORT). */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

/** Minimum gap between stream chunk arrivals to prove unbuffered incremental delivery. */
const MIN_INCREMENTAL_GAP_MS = 100

/** Idle observation window for heartbeat survival (LR-B3 keep-alive cadence ~20s). */
const HEARTBEAT_OBSERVE_MS = 65_000

const SPEC_DIR = path.dirname(fileURLToPath(import.meta.url))
const EVIDENCE_DIR = path.join(SPEC_DIR, 'evidence')
const EVIDENCE_JSON = path.join(EVIDENCE_DIR, 'LRP-E1-sse-timestamps.json')

type EvidencePayload = {
  recordedAt: string
  scenarioA?: Record<string, unknown>
  scenarioB?: Record<string, unknown>
  batchProbe?: Record<string, unknown>
}

function mergeEvidence(partial: EvidencePayload): void {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  let existing: EvidencePayload = { recordedAt: new Date().toISOString() }
  if (existsSync(EVIDENCE_JSON)) {
    existing = JSON.parse(readFileSync(EVIDENCE_JSON, 'utf8')) as EvidencePayload
  }
  const next: EvidencePayload = {
    ...existing,
    recordedAt: new Date().toISOString(),
    scenarioA: partial.scenarioA ?? existing.scenarioA,
    scenarioB: partial.scenarioB ?? existing.scenarioB,
    batchProbe: partial.batchProbe ?? existing.batchProbe,
  }
  writeFileSync(EVIDENCE_JSON, `${JSON.stringify(next, null, 2)}\n`, 'utf8')
}

/**
 * LR-E1: SSE-through-proxy incremental E2E (Task Master #42).
 *
 * Verifies preview progress events arrive INCREMENTALLY through the nginx proxy on
 * Docker :4173 (not as one terminal burst) and that `: keep-alive` heartbeats keep an
 * idle stream alive past 60s (LR-B3 browser-level proof; closes CD-PIT-12).
 *
 * Precondition: Docker stack deployed (Stage 5 DEPLOY_OK).
 * Run:
 *   pnpm -C frontend exec playwright test e2e/LRP-E1-sse-incremental-progress.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 */
test.describe('LRP-E1 — SSE incremental progress through proxy', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: CdpMvpGoldenFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareCdpMvpGoldenDraft(request)
    expect(fixture.lifecycleStatus).toBe('DRAFT')
  })

  test('Scenario A — preview progress arrives incrementally (≥2 chunk timestamps)', async ({
    page,
    request,
  }) => {
    const dataSets = await listTestDataSets(request, fixture.templateId)
    expect(dataSets.length).toBeGreaterThan(0)
    await waitForPreviewConcurrencySlot(request, fixture.templateId, dataSets[0]!.testDataSetId)

    await installSseFetchCapture(page)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)

    const uiPhaseArrivals: Array<{ atMs: number; label: string }> = []
    const wallStart = Date.now()

    const previewStart = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/previews/async-preview'),
      { timeout: 30_000 },
    )

    await runPreviewFromFirstDataSetRow(page)
    const startResponse = await previewStart
    expect([200, 202]).toContain(startResponse.status())

    const dialog = previewProgressDialog(page)
    await expect(
      dialog.getByText(/queued|generating docx|converting to pdf|uploading/i).first(),
    ).toBeVisible({ timeout: 30_000 })
    uiPhaseArrivals.push({
      atMs: Date.now() - wallStart,
      label: 'first-progress-label-visible',
    })

    await waitForPreviewDialogSuccess(page)
    uiPhaseArrivals.push({
      atMs: Date.now() - wallStart,
      label: 'success-download-visible',
    })

    // Allow the tee capture reader to flush the final chunk.
    await page.waitForTimeout(250)

    const capture = await readSseCapture(page)
    expect(
      capture.streamUrls.length,
      'Expected at least one progress-stream fetch via authorizedEventStream',
    ).toBeGreaterThan(0)
    expect(
      capture.chunks.length,
      `Expected ≥2 SSE body chunks through nginx (got ${capture.chunks.length}). ` +
        'A single terminal flush indicates proxy buffering (CD-PIT-12 regression).',
    ).toBeGreaterThanOrEqual(2)

    const gaps = distinctArrivalGapsMs(capture.chunks.map((c) => c.atMs))
    const maxGap = gaps.length > 0 ? Math.max(...gaps) : 0
    expect(
      maxGap,
      `Expected ≥${MIN_INCREMENTAL_GAP_MS}ms between chunk arrivals (gaps=${JSON.stringify(gaps)}); ` +
        'near-zero gaps suggest a buffered terminal burst.',
    ).toBeGreaterThanOrEqual(MIN_INCREMENTAL_GAP_MS)

    const frames = parseSseFramesFromChunks(capture.chunks)
    const progressOrTerminal = frames.filter(
      (f) =>
        f.kind === 'event' &&
        (f.eventType === 'progress' || f.eventType === 'completed' || f.eventType === 'failed'),
    )

    mergeEvidence({
      recordedAt: new Date().toISOString(),
      scenarioA: {
        verdict: 'PASS',
        streamUrls: capture.streamUrls,
        chunkCount: capture.chunks.length,
        chunkArrivalOffsetsMs: capture.chunks.map((c) =>
          capture.startedAt == null ? c.atMs : Math.round(c.atMs - capture.startedAt),
        ),
        maxGapMs: Math.round(maxGap),
        minIncrementalGapMs: MIN_INCREMENTAL_GAP_MS,
        frames: progressOrTerminal.map((f) => ({
          eventType: f.eventType,
          atOffsetMs: capture.startedAt == null ? f.atMs : Math.round(f.atMs - capture.startedAt),
          preview: f.preview,
        })),
        uiPhaseArrivals,
        wallClockMs: Date.now() - wallStart,
      },
    })
  })

  test('Scenario B — idle stream survives ≥60s via keep-alive through nginx', async ({
    page,
    request,
  }) => {
    // Optional batch probe: record duration; CDP golden batches are usually <<60s so
    // heartbeat survival uses the dedicated idle path (same as LR-B3 curl smoke).
    const dataSets = await listTestDataSets(request, fixture.templateId)
    expect(dataSets.length).toBeGreaterThan(0)

    await installSseFetchCapture(page)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)

    const batchWallStart = Date.now()
    try {
      await runFullTestFromUi(page, request, fixture.templateId)
      const batchDurationMs = Date.now() - batchWallStart
      const batchCapture = await readSseCapture(page)
      await batchProgressDialog(page)
        .getByRole('button', { name: /^close$/i })
        .click()
        .catch(() => undefined)
      mergeEvidence({
        recordedAt: new Date().toISOString(),
        batchProbe: {
          attempted: true,
          failed: false,
          durationMs: batchDurationMs,
          chunkCount: batchCapture.chunks.length,
          exceeded60s: batchDurationMs >= 60_000,
          note:
            batchDurationMs >= 60_000
              ? 'Batch run itself exceeded 60s — stream longevity incidental.'
              : 'Batch completed under 60s; Scenario B proof uses dedicated idle heartbeat path (LR-B3 equivalent).',
        },
      })
    } catch (error) {
      mergeEvidence({
        recordedAt: new Date().toISOString(),
        batchProbe: {
          attempted: true,
          failed: true,
          durationMs: Date.now() - batchWallStart,
          error: String(error),
          note: 'Batch probe failed; continuing with dedicated idle heartbeat path.',
        },
      })
    }

    // Dedicated idle path: open progress-stream for a never-started previewId so the
    // emitter stays registered and only `: keep-alive` comments are written (~20s).
    const idlePreviewId = randomUUID()
    const streamUrl = `/api/management/v1/templates/${fixture.templateId}/previews/${idlePreviewId}/progress-stream`

    // Ensure we are on same-origin UI with a session token before fetch.
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })

    const idle = await observeIdleProgressStreamHeartbeats(page, {
      streamUrl,
      observeMs: HEARTBEAT_OBSERVE_MS,
    })

    expect(idle.ok, `Idle progress-stream GET failed with status ${idle.status}`).toBeTruthy()
    expect(
      idle.closedBeforeObserveEnd,
      'Stream closed before 60s idle window — proxy/backend dropped the connection',
    ).toBe(false)
    expect(
      idle.durationMs,
      `Observed only ${Math.round(idle.durationMs)}ms; need ≥60000ms idle survival`,
    ).toBeGreaterThanOrEqual(60_000)
    expect(
      idle.heartbeatAtMs.length,
      `Expected ≥2 :keep-alive heartbeats over ~65s (got ${idle.heartbeatAtMs.length}). Samples: ${JSON.stringify(idle.sampleTexts)}`,
    ).toBeGreaterThanOrEqual(2)

    const hbGaps = distinctArrivalGapsMs(idle.heartbeatAtMs)
    // Cadence is ~20s; allow 12–28s jitter for scheduler + network.
    for (const gap of hbGaps) {
      expect(gap, `Heartbeat gap ${gap}ms outside 12–28s LR-B3 cadence window`).toBeGreaterThanOrEqual(
        12_000,
      )
      expect(gap, `Heartbeat gap ${gap}ms outside 12–28s LR-B3 cadence window`).toBeLessThanOrEqual(
        28_000,
      )
    }

    mergeEvidence({
      recordedAt: new Date().toISOString(),
      scenarioB: {
        verdict: 'PASS',
        mode: 'dedicated-idle-progress-stream',
        streamUrl,
        observeMs: HEARTBEAT_OBSERVE_MS,
        durationMs: Math.round(idle.durationMs),
        heartbeatOffsetsMs: idle.heartbeatAtMs.map((ms) => Math.round(ms)),
        heartbeatGapsMs: hbGaps.map((ms) => Math.round(ms)),
        chunkCount: idle.chunkCount,
        closedBeforeObserveEnd: idle.closedBeforeObserveEnd,
        sampleTexts: idle.sampleTexts,
        equivalentTo: 'LR-B3 Docker curl idle survival (~20s :keep-alive cadence)',
      },
    })
  })
})
