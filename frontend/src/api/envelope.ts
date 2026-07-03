import type { ApiEnvelope } from '@/types/session'

export function unwrapEnvelope<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }

  return envelope.result
}
