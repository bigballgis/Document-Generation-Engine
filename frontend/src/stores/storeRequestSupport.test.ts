import { describe, expect, it } from 'vitest'
import { ref } from 'vue'

import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'
import {
  clearStoreListError,
  handleStoreListFailure,
  isAbortError,
  recordStoreListError,
} from '@/stores/storeRequestSupport'

describe('storeRequestSupport', () => {
  it('detects axios abort errors', () => {
    expect(isAbortError(new DOMException('Aborted', 'AbortError'))).toBe(true)
  })

  it('records retryable flag from API envelope', () => {
    const messageKey = ref<string | null>(null)
    const retryable = ref(false)

    recordStoreListError(
      axiosEnvelopeError(503, 'api.error.generation.serviceUnavailable', {
        retryable: true,
      }),
      'common.loadError',
      messageKey,
      retryable,
    )

    expect(messageKey.value).toBe('api.error.generation.serviceUnavailable')
    expect(retryable.value).toBe(true)
  })

  it('ignores aborted list requests', () => {
    const messageKey = ref<string | null>('stale.error')
    const retryable = ref(true)

    expect(() =>
      handleStoreListFailure(
        new DOMException('Aborted', 'AbortError'),
        'common.loadError',
        messageKey,
        retryable,
      ),
    ).not.toThrow()

    expect(messageKey.value).toBe('stale.error')
    expect(retryable.value).toBe(true)
  })

  it('clears list error state before reload', () => {
    const messageKey = ref<string | null>('templates.error.loadList')
    const retryable = ref(true)

    clearStoreListError(messageKey, retryable)

    expect(messageKey.value).toBeNull()
    expect(retryable.value).toBe(false)
  })
})
