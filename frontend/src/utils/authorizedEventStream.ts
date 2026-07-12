interface AuthorizedEventStreamMessage {
  type: string
  data: string
}

export interface AuthorizedEventStreamConnection {
  close: () => void
  done: Promise<void>
}

interface ConnectAuthorizedEventStreamOptions {
  url: string
  token?: string | null
  fetch?: typeof globalThis.fetch
  onMessage: (event: AuthorizedEventStreamMessage) => void
  onError?: (error: unknown) => void
}

function dispatchFrames(
  buffer: string,
  onMessage: (event: AuthorizedEventStreamMessage) => void,
): string {
  const normalized = buffer.replace(/\r\n/g, '\n')
  const frames = normalized.split('\n\n')
  const remainder = frames.pop() ?? ''

  for (const frame of frames) {
    let type = 'message'
    const dataLines: string[] = []

    for (const line of frame.split('\n')) {
      if (line.startsWith(':') || line.length === 0) {
        continue
      }
      if (line.startsWith('event:')) {
        type = line.slice('event:'.length).trim() || 'message'
        continue
      }
      if (line.startsWith('data:')) {
        dataLines.push(line.slice('data:'.length).trimStart())
      }
    }

    if (dataLines.length > 0) {
      onMessage({ type, data: dataLines.join('\n') })
    }
  }

  return remainder
}

export async function connectAuthorizedEventStream({
  url,
  token,
  fetch: fetchImpl = globalThis.fetch,
  onMessage,
  onError,
}: ConnectAuthorizedEventStreamOptions): Promise<AuthorizedEventStreamConnection> {
  const controller = new AbortController()
  const headers = new Headers({
    Accept: 'text/event-stream',
  })

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetchImpl(url, {
    headers,
    signal: controller.signal,
  })

  if (!response.ok) {
    throw new Error(`Event stream request failed with status ${response.status}`)
  }

  if (!response.body) {
    throw new Error('Event stream response body is unavailable')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()

  const done = (async () => {
    let buffer = ''

    try {
      while (true) {
        const { done: streamDone, value } = await reader.read()
        if (streamDone) {
          break
        }

        buffer += decoder.decode(value, { stream: true })
        buffer = dispatchFrames(buffer, onMessage)
      }

      buffer += decoder.decode()
      dispatchFrames(buffer, onMessage)
    } catch (error) {
      if (!controller.signal.aborted) {
        onError?.(error)
        throw error
      }
    }
  })()

  return {
    close() {
      controller.abort()
      void reader.cancel().catch(() => undefined)
    },
    done,
  }
}
