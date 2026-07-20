import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as documentBrandsApi from '@/api/documentBrands'
import { resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import type {
  CreateDocumentBrandPayload,
  DocumentBrandStatus,
  DocumentBrandView,
  UpdateDocumentBrandPayload,
} from '@/types/documentBrand'

export const useDocumentBrandsStore = defineStore('documentBrands', () => {
  const brands = ref<DocumentBrandView[]>([])
  const groupCode = ref('')
  const statusFilter = ref<DocumentBrandStatus | ''>('')
  const loadingList = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)
  const lastListErrorRetryable = ref(false)
  const lastMutationErrorMessageKey = ref<string | null>(null)

  async function fetchBrands(
    nextGroupCode: string,
    options: AbortableRequestOptions & { status?: DocumentBrandStatus | '' } = {},
  ): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    const status = options.status !== undefined ? options.status : statusFilter.value
    try {
      const pageView = await documentBrandsApi.listDocumentBrands(nextGroupCode, {
        status: status || undefined,
        signal: options.signal,
      })
      brands.value = pageView.content
      groupCode.value = nextGroupCode
      if (options.status !== undefined) {
        statusFilter.value = options.status
      }
    } catch (error) {
      handleStoreListFailure(
        error,
        'documentBrands.error.loadList',
        lastErrorMessageKey,
        lastListErrorRetryable,
        { useStoreResolver: true },
      )
    } finally {
      loadingList.value = false
    }
  }

  async function createBrand(payload: CreateDocumentBrandPayload): Promise<DocumentBrandView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      return await documentBrandsApi.createDocumentBrand(payload)
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'documentBrands.error.create',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function updateBrand(
    documentBrandCode: string,
    payload: UpdateDocumentBrandPayload,
  ): Promise<DocumentBrandView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      return await documentBrandsApi.updateDocumentBrand(documentBrandCode, payload)
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'documentBrands.error.update',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  return {
    brands,
    groupCode,
    statusFilter,
    loadingList,
    submitting,
    lastErrorMessageKey,
    lastListErrorRetryable,
    lastMutationErrorMessageKey,
    fetchBrands,
    createBrand,
    updateBrand,
  }
})
