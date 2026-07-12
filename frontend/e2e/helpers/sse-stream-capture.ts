import type { Page } from '@playwright/test'

/**
 * LR-E1 — capture incremental SSE chunk arrivals for progress-stream fetches.
 *
 * Preview/batch progress uses fetch + ReadableStream (authorizedEventStream.ts),
 * not EventSource. page.on('response') only fires once when headers arrive, so it
 * cannot prove incremental delivery through nginx. This helper tees the response body
 * and records each chunk arrival timestamp in the page.
 */

export type SseChunkArrival = {
  atMs: number
  byteLength: number
  text: string
}

export type SseCaptureState = {
  chunks: SseChunkArrival[]
  startedAt: number | null
  streamUrls: string[]
}

declare global {
  interface Window {
    __lrpE1SseCapture?: SseCaptureState
  }
}

const CAPTURE_INIT = `(() => {
  const g = window
  g.__lrpE1SseCapture = { chunks: [], startedAt: null, streamUrls: [] }
  const originalFetch = window.fetch.bind(window)
  window.fetch = async (...args) => {
    const input = args[0]
    const url =
      typeof input === 'string'
        ? input
        : input instanceof URL
          ? input.href
          : input.url
    const response = await originalFetch(...args)
    if (!/\\/progress-stream(?:\\?|$)/.test(url) || !response.body) {
      return response
    }
    const capture = g.__lrpE1SseCapture
    if (capture) {
      capture.streamUrls.push(url)
      capture.startedAt ??= performance.now()
    }
    const [appStream, captureStream] = response.body.tee()
    void (async () => {
      const reader = captureStream.getReader()
      const decoder = new TextDecoder()
      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          const text = decoder.decode(value, { stream: true })
          capture?.chunks.push({
            atMs: performance.now(),
            byteLength: value.byteLength,
            text,
          })
        }
      } catch {
        /* aborted / closed */
      }
    })()
    return new Response(appStream, {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
    })
  }
})()`

/** Install before first navigation so preview/batch SSE fetches are instrumented. */
export async function installSseFetchCapture(page: Page): Promise<void> {
  await page.addInitScript(CAPTURE_INIT)
}

export async function readSseCapture(page: Page): Promise<SseCaptureState> {
  return page.evaluate(() => {
    return (
      window.__lrpE1SseCapture ?? {
        chunks: [],
        startedAt: null,
        streamUrls: [],
      }
    )
  })
}

export function parseSseFramesFromChunks(chunks: SseChunkArrival[]): Array<{
  atMs: number
  kind: 'event' | 'comment' | 'other'
  eventType?: string
  preview: string
}> {
  const frames: Array<{
    atMs: number
    kind: 'event' | 'comment' | 'other'
    eventType?: string
    preview: string
  }> = []
  let buffer = ''

  for (const chunk of chunks) {
    buffer += chunk.text.replace(/\r\n/g, '\n')
    const parts = buffer.split('\n\n')
    buffer = parts.pop() ?? ''
    for (const frame of parts) {
      const trimmed = frame.trim()
      if (!trimmed) continue
      const preview = trimmed.slice(0, 160).replace(/\s+/g, ' ')
      if (/^:\s*keep-alive/im.test(trimmed) || trimmed.split('\n').every((l) => l.startsWith(':'))) {
        frames.push({ atMs: chunk.atMs, kind: 'comment', preview })
        continue
      }
      const eventLine = trimmed.split('\n').find((l) => l.startsWith('event:'))
      const eventType = eventLine ? eventLine.slice('event:'.length).trim() : 'message'
      frames.push({ atMs: chunk.atMs, kind: 'event', eventType, preview })
    }
  }
  return frames
}

export function distinctArrivalGapsMs(arrivalTimesMs: number[]): number[] {
  const sorted = [...arrivalTimesMs].sort((a, b) => a - b)
  const gaps: number[] = []
  for (let i = 1; i < sorted.length; i += 1) {
    gaps.push(sorted[i]! - sorted[i - 1]!)
  }
  return gaps
}

/**
 * Observe an idle progress-stream through the browser origin (nginx :4173 proxy).
 * Uses a non-existent previewId so the emitter stays registered and only heartbeats
 * are written — mirrors LR-B3 curl idle survival without waiting on a fast preview/batch.
 */
export async function observeIdleProgressStreamHeartbeats(
  page: Page,
  options: {
    streamUrl: string
    observeMs: number
  },
): Promise<{
  ok: boolean
  status: number
  durationMs: number
  heartbeatAtMs: number[]
  chunkCount: number
  closedBeforeObserveEnd: boolean
  sampleTexts: string[]
}> {
  return page.evaluate(async ({ streamUrl, observeMs }) => {
    const token = localStorage.getItem('docgen.accessToken')
    const controller = new AbortController()
    const started = performance.now()
    const heartbeatAtMs: number[] = []
    const sampleTexts: string[] = []
    let chunkCount = 0
    let closedBeforeObserveEnd = false

    const response = await fetch(streamUrl, {
      headers: {
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      signal: controller.signal,
    })

    if (!response.ok || !response.body) {
      return {
        ok: false,
        status: response.status,
        durationMs: performance.now() - started,
        heartbeatAtMs,
        chunkCount,
        closedBeforeObserveEnd: true,
        sampleTexts,
      }
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let stop = false

    const consume = (async () => {
      try {
        while (!stop) {
          const { done, value } = await reader.read()
          if (done) {
            closedBeforeObserveEnd = !stop
            break
          }
          if (!value) continue
          chunkCount += 1
          const text = decoder.decode(value, { stream: true })
          if (sampleTexts.length < 8) {
            sampleTexts.push(text.slice(0, 120))
          }
          buffer += text.replace(/\r\n/g, '\n')
          const parts = buffer.split('\n\n')
          buffer = parts.pop() ?? ''
          for (const frame of parts) {
            if (/:\s*keep-alive/i.test(frame)) {
              heartbeatAtMs.push(performance.now() - started)
            }
          }
        }
      } catch {
        /* aborted */
      }
    })()

    await Promise.race([
      consume,
      new Promise<void>((resolve) => {
        setTimeout(() => {
          stop = true
          resolve()
        }, observeMs)
      }),
    ])

    stop = true
    controller.abort()
    try {
      await reader.cancel()
    } catch {
      /* ignore */
    }
    await consume.catch(() => undefined)

    return {
      ok: true,
      status: response.status,
      durationMs: performance.now() - started,
      heartbeatAtMs,
      chunkCount,
      closedBeforeObserveEnd,
      sampleTexts,
    }
  }, options)
}
