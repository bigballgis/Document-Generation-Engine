import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as legalHoldsApi from '@/api/legalHolds'
import { resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import type {
  CreateLegalHoldPayload,
  LegalHoldStatus,
  LegalHoldView,
} from '@/types/legalHold'

export const useLegalHoldsStore = defineStore('legalHolds', () => {
  const holds = ref<LegalHoldView[]>([])
  const listPage = ref(0)
  const listSize = ref(20)
  const listTotalElements = ref(0)
  const listTotalPages = ref(0)
  const statusFilter = ref<LegalHoldStatus | ''>('')
  const loadingList = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)
  const lastListErrorRetryable = ref(false)
  const lastMutationErrorMessageKey = ref<string | null>(null)

  async function fetchHolds(
    page = listPage.value,
    size = listSize.value,
    options: AbortableRequestOptions & { status?: LegalHoldStatus | '' } = {},
  ): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    const status = options.status !== undefined ? options.status : statusFilter.value
    try {
      const pageView = await legalHoldsApi.listLegalHolds(page, size, {
        status: status || undefined,
        signal: options.signal,
      })
      holds.value = pageView.content
      listPage.value = pageView.page
      listSize.value = pageView.size
      listTotalElements.value = pageView.totalElements
      listTotalPages.value = pageView.totalPages
      if (options.status !== undefined) {
        statusFilter.value = options.status
      }
    } catch (error) {
      handleStoreListFailure(
        error,
        'legalHold.error.loadList',
        lastErrorMessageKey,
        lastListErrorRetryable,
        { useStoreResolver: true },
      )
    } finally {
      loadingList.value = false
    }
  }

  async function createHold(payload: CreateLegalHoldPayload): Promise<LegalHoldView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      return await legalHoldsApi.createLegalHold(payload)
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'legalHold.error.create',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function releaseHold(id: string): Promise<LegalHoldView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      return await legalHoldsApi.releaseLegalHold(id)
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'legalHold.error.release',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  return {
    holds,
    listPage,
    listSize,
    listTotalElements,
    listTotalPages,
    statusFilter,
    loadingList,
    submitting,
    lastErrorMessageKey,
    lastListErrorRetryable,
    lastMutationErrorMessageKey,
    fetchHolds,
    createHold,
    releaseHold,
  }
})
