import type { Ref } from 'vue'
import * as mastersApi from '@/api/masters'
import { resolveApiErrorMessageKey, resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import { applyUpdatedMaster, toMasterSummary } from '@/stores/mastersStoreHelpers'
import type {
  CreateMasterPayload,
  DecideMasterReviewPayload,
  MasterDocumentDetail,
  MasterDocumentSummary,
  SubmitMasterReviewPayload,
  UpdateMasterMetadataPayload,
} from '@/types/master'

export function createMastersCatalogActions(deps: {
  masters: Ref<MasterDocumentSummary[]>
  masterListPage: Ref<number>
  masterListSize: Ref<number>
  masterListTotalElements: Ref<number>
  masterListTotalPages: Ref<number>
  selectedMaster: Ref<MasterDocumentDetail | null>
  loadingList: Ref<boolean>
  loadingDetail: Ref<boolean>
  submitting: Ref<boolean>
  uploadProgress: Ref<number | null>
  lastErrorMessageKey: Ref<string | null>
  lastListErrorRetryable: Ref<boolean>
}) {
  const {
    masters,
    masterListPage,
    masterListSize,
    masterListTotalElements,
    masterListTotalPages,
    selectedMaster,
    loadingList,
    loadingDetail,
    submitting,
    uploadProgress,
    lastErrorMessageKey,
    lastListErrorRetryable,
  } = deps

  async function fetchMasters(
    page = masterListPage.value,
    size = masterListSize.value,
    options: AbortableRequestOptions & {
      search?: string
      groupCode?: string
      status?: string
      sort?: string
    } = {},
  ): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    try {
      const pageView = await mastersApi.listMasters(page, size, options)
      masters.value = pageView.content
      masterListPage.value = pageView.page
      masterListSize.value = pageView.size
      masterListTotalElements.value = pageView.totalElements
      masterListTotalPages.value = pageView.totalPages
    } catch (error) {
      handleStoreListFailure(
        error,
        'masters.error.loadList',
        lastErrorMessageKey,
        lastListErrorRetryable,
        { useStoreResolver: true },
      )
    } finally {
      loadingList.value = false
    }
  }

  /** Full catalog merge for dashboard / import pickers (not list-view paging). */
  async function fetchAllMasters(
    options: AbortableRequestOptions & {
      search?: string
      groupCode?: string
      status?: string
      sort?: string
    } = {},
  ): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    try {
      const collected = await mastersApi.listAllMasters(options)
      masters.value = collected.content
      masterListPage.value = 0
      masterListSize.value = collected.content.length || masterListSize.value
      masterListTotalElements.value = collected.totalElements
      masterListTotalPages.value =
        collected.totalElements === 0
          ? 0
          : Math.max(1, Math.ceil(collected.totalElements / 100))
    } catch (error) {
      handleStoreListFailure(
        error,
        'masters.error.loadList',
        lastErrorMessageKey,
        lastListErrorRetryable,
        { useStoreResolver: true },
      )
    } finally {
      loadingList.value = false
    }
  }

  async function fetchMaster(masterId: string): Promise<void> {
    loadingDetail.value = true
    lastErrorMessageKey.value = null
    try {
      selectedMaster.value = await mastersApi.getMaster(masterId)
    } catch (error) {
      lastErrorMessageKey.value = resolveStoreErrorMessageKey(error, 'masters.error.loadDetail')
      throw error
    } finally {
      loadingDetail.value = false
    }
  }

  async function uploadMaster(payload: CreateMasterPayload, file: File): Promise<MasterDocumentDetail> {
    submitting.value = true
    uploadProgress.value = null
    lastErrorMessageKey.value = null
    try {
      const created = await mastersApi.createMaster(payload, file, {
        onUploadProgress: (percent) => {
          uploadProgress.value = percent
        },
      })
      masters.value = [toMasterSummary(created), ...masters.value.filter((item) => item.id !== created.id)]
      selectedMaster.value = created
      return created
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.upload')
      throw error
    } finally {
      submitting.value = false
      uploadProgress.value = null
    }
  }

  async function submitReview(
    masterId: string,
    payload: SubmitMasterReviewPayload,
  ): Promise<MasterDocumentDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await mastersApi.submitMasterReview(masterId, payload)
      applyUpdatedMaster(selectedMaster, masters, updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.submitReview')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function decideReview(
    masterId: string,
    payload: DecideMasterReviewPayload,
  ): Promise<MasterDocumentDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await mastersApi.decideMasterReview(masterId, payload)
      applyUpdatedMaster(selectedMaster, masters, updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.decideReview')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function updateMasterMetadata(
    masterId: string,
    payload: UpdateMasterMetadataPayload,
  ): Promise<MasterDocumentDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await mastersApi.updateMasterMetadata(masterId, payload)
      applyUpdatedMaster(selectedMaster, masters, updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.updateMetadata')
      throw error
    } finally {
      submitting.value = false
    }
  }

  return {
    fetchMasters,
    fetchAllMasters,
    fetchMaster,
    uploadMaster,
    submitReview,
    decideReview,
    updateMasterMetadata,
  }
}
