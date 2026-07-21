import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as libraryAssetsApi from '@/api/libraryAssets'
import { resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import type {
  LibraryAssetClass,
  LibraryAssetListStatusFilter,
  LibraryAssetView,
} from '@/types/libraryAsset'

export const useLibraryAssetsStore = defineStore('libraryAssets', () => {
  const assets = ref<LibraryAssetView[]>([])
  const assetListPage = ref(0)
  const assetListSize = ref(20)
  const assetListTotalElements = ref(0)
  const assetListTotalPages = ref(0)
  const loadingList = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)
  const lastListErrorRetryable = ref(false)
  const lastMutationErrorMessageKey = ref<string | null>(null)

  async function fetchAssets(
    page = assetListPage.value,
    size = assetListSize.value,
    options: AbortableRequestOptions & {
      groupCode?: string
      assetClass?: LibraryAssetClass
      status?: LibraryAssetListStatusFilter
      q?: string
    } = {},
  ): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    try {
      const pageView = await libraryAssetsApi.listLibraryAssets(page, size, options)
      assets.value = pageView.content
      assetListPage.value = pageView.page
      assetListSize.value = pageView.size
      assetListTotalElements.value = pageView.totalElements
      assetListTotalPages.value = pageView.totalPages
    } catch (error) {
      handleStoreListFailure(
        error,
        'assetLibrary.error.loadList',
        lastErrorMessageKey,
        lastListErrorRetryable,
        { useStoreResolver: true },
      )
    } finally {
      loadingList.value = false
    }
  }

  async function uploadAsset(payload: {
    groupCode: string
    assetKey: string
    assetClass: LibraryAssetClass
    file: File
  }): Promise<LibraryAssetView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      return await libraryAssetsApi.uploadLibraryAsset(payload)
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'assetLibrary.error.upload',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function disableAsset(assetKey: string, groupCode: string): Promise<LibraryAssetView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      return await libraryAssetsApi.disableLibraryAsset(assetKey, groupCode)
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'assetLibrary.error.disable',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  function clearListError() {
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
  }

  function clearMutationError() {
    lastMutationErrorMessageKey.value = null
  }

  return {
    assets,
    assetListPage,
    assetListSize,
    assetListTotalElements,
    assetListTotalPages,
    loadingList,
    submitting,
    lastErrorMessageKey,
    lastListErrorRetryable,
    lastMutationErrorMessageKey,
    fetchAssets,
    uploadAsset,
    disableAsset,
    clearListError,
    clearMutationError,
  }
})
