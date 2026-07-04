import axios from 'axios'
import type { Ref } from 'vue'
import { resolveApiError, resolveApiErrorMessageKey, resolveStoreErrorMessageKey } from '@/api/http'

export type AbortableRequestOptions = {
  signal?: AbortSignal
}

export function isAbortError(error: unknown): boolean {
  if (axios.isCancel(error)) {
    return true
  }
  if (error instanceof DOMException && error.name === 'AbortError') {
    return true
  }
  return error instanceof Error && error.name === 'AbortError'
}

export function clearStoreListError(
  messageKey: Ref<string | null>,
  retryable: Ref<boolean>,
): void {
  messageKey.value = null
  retryable.value = false
}

export function recordStoreListError(
  error: unknown,
  fallbackKey: string,
  messageKey: Ref<string | null>,
  retryable: Ref<boolean>,
  options: { useStoreResolver?: boolean } = {},
): string | null {
  const resolvedKey = options.useStoreResolver
    ? resolveStoreErrorMessageKey(error, fallbackKey)
    : resolveApiErrorMessageKey(error, fallbackKey)
  if (resolvedKey === null) {
    messageKey.value = null
    retryable.value = false
    return null
  }
  messageKey.value = resolvedKey
  retryable.value = resolveApiError(error)?.error.retryable ?? false
  return resolvedKey
}

export function handleStoreListFailure(
  error: unknown,
  fallbackKey: string,
  messageKey: Ref<string | null>,
  retryable: Ref<boolean>,
  options: { useStoreResolver?: boolean } = {},
): void {
  if (isAbortError(error)) {
    return
  }
  const resolvedKey = recordStoreListError(error, fallbackKey, messageKey, retryable, options)
  if (resolvedKey === null) {
    return
  }
  throw error
}
