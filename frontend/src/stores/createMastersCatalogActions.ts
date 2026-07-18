import type { Ref } from 'vue'
import * as mastersApi from '@/api/masters'
import { resolveApiErrorMessageKey, resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import { createMastersCatalogMutationActions } from '@/stores/createMastersCatalogMutationActions'
import { toMasterSummary } from '@/stores/mastersStoreHelpers'
import type {
  CreateMasterPayload,
  MasterDocumentDetail,
  MasterDocumentSummary,
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

  /** Full catalog merge for import pickers (not list-view paging; not Dashboard Overview). */
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

  /**
   * Status-filtered multi-page collect for Dashboard master workflow todos / journeys
   * (PRR-D01c D01C-C6). Candidate sets are far smaller than the full catalog.
   */
  async function fetchDashboardWorkflowMasters(
    options: AbortableRequestOptions & {
      includePendingReview?: boolean
      includeDraftOrRejected?: boolean
    } = {},
  ): Promise<void> {
    const statuses: string[] = []
    if (options.includePendingReview) {
      statuses.push('PENDING_REVIEW')
    }
    if (options.includeDraftOrRejected) {
      statuses.push('DRAFT', 'REJECTED')
    }
    if (statuses.length === 0) {
      masters.value = []
      return
    }

    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    try {
      const pages = await Promise.all(
        statuses.map((status) =>
          mastersApi.listAllMasters({
            status,
            signal: options.signal,
          }),
        ),
      )
      const byId = new Map<string, MasterDocumentSummary>()
      for (const page of pages) {
        for (const master of page.content) {
          byId.set(master.id, master)
        }
      }
      masters.value = [...byId.values()]
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

  const mutationActions = createMastersCatalogMutationActions({
    masters,
    selectedMaster,
    submitting,
    lastErrorMessageKey,
  })

  return {
    fetchMasters,
    fetchAllMasters,
    fetchDashboardWorkflowMasters,
    fetchMaster,
    uploadMaster,
    ...mutationActions,
  }
}
