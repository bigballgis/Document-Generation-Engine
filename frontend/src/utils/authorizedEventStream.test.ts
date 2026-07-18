import { readFileSync } from 'node:fs'
import { describe, expect, it, vi } from 'vitest'
import { connectAuthorizedEventStream } from '@/utils/authorizedEventStream'

function encodeChunks(...chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder()

  return new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk))
      }
      controller.close()
    },
  })
}

describe('connectAuthorizedEventStream', () => {
  it('opens the stream with an authorization header instead of a token query param', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue({
      ok: true,
      body: encodeChunks('event: progress\ndata: {"percent":40}\n\n'),
    } as Response)
    const events: Array<{ type: string; data: string }> = []

    const stream = await connectAuthorizedEventStream({
      url: '/api/management/v1/templates/tpl-1/previews/prev-1/progress-stream',
      token: 'test-token',
      fetch: fetchMock,
      onMessage: (event) => events.push(event),
    })
    await stream.done

    const [, requestInit] = fetchMock.mock.calls[0] ?? []
    const headers = requestInit?.headers as Headers

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/management/v1/templates/tpl-1/previews/prev-1/progress-stream',
      expect.objectContaining({ headers: expect.any(Headers) }),
    )
    expect(headers.get('Accept')).toBe('text/event-stream')
    expect(headers.get('Authorization')).toBe('Bearer test-token')
    expect(events).toEqual([{ type: 'progress', data: '{"percent":40}' }])

    stream.close()
  })

  it('parses multiple named events even when frames span chunks', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue({
      ok: true,
      body: encodeChunks(
        'event: progress\n',
        'data: {"percent":40}\n\n',
        'event: completed\ndata: {"previewId":"prev-1"}\n\n',
      ),
    } as Response)
    const events: Array<{ type: string; data: string }> = []

    const stream = await connectAuthorizedEventStream({
      url: '/stream',
      token: 'test-token',
      fetch: fetchMock,
      onMessage: (event) => events.push(event),
    })
    await stream.done

    expect(events).toEqual([
      { type: 'progress', data: '{"percent":40}' },
      { type: 'completed', data: '{"previewId":"prev-1"}' },
    ])
  })

  it('removes token query strings and EventSource usage from progress dialogs', () => {
    for (const relativePath of [
      '../components/templates/BatchTestProgressDialog.vue',
      '../components/templates/PreviewProgressDialog.vue',
    ]) {
      const source = readFileSync(new URL(relativePath, import.meta.url), 'utf8')

      expect(source).not.toContain('token=')
      expect(source).not.toContain('new EventSource(')
    }
  })
})
