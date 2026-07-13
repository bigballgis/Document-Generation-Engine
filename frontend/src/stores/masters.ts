import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as mastersApi from '@/api/masters'
import { resolveApiErrorMessageKey, resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import { createMastersRevisionActions } from '@/stores/createMastersRevisionActions'
import { applyUpdatedMaster, toMasterSummary } from '@/stores/mastersStoreHelpers'
import type {
  CreateMasterPayload,
  DecideMasterReviewPayload,
  MasterDocumentDetail,
  MasterDocumentSummary,
  MasterImpactAnalysis,
  MasterRevisionLineDetail,
  MasterRevisionLinePage,
  MasterReviewRecord,
  SubmitMasterReviewPayload,
  UpdateMasterMetadataPayload,
} from '@/types/master'

export const useMastersStore = defineStore('masters', () => {
  const masters = ref<MasterDocumentSummary[]>([])
  const masterListPage = ref(0)
  const masterListSize = ref(20)
  const masterListTotalElements = ref(0)
  const masterListTotalPages = ref(0)
  const selectedMaster = ref<MasterDocumentDetail | null>(null)
  const selectedRevisionLine = ref<MasterRevisionLineDetail | null>(null)
  const revisionLinesPage = ref<MasterRevisionLinePage | null>(null)
  const impactAnalysis = ref<MasterImpactAnalysis | null>(null)
  const loadingList = ref(false)
  const loadingDetail = ref(false)
  const loadingRevisionLines = ref(false)
  const loadingRevisionLine = ref(false)
  const submitting = ref(false)
  const uploadProgress = ref<number | null>(null)
  const lastErrorMessageKey = ref<string | null>(null)
  const lastListErrorRetryable = ref(false)
  const draftReviewHistoryByMasterId = ref<Record<string, MasterReviewRecord[]>>({})

  const mastersByGroup = computed(() => {
    const grouped = new Map<string, MasterDocumentSummary[]>()
    for (const master of masters.value) {
      const existing = grouped.get(master.groupCode) ?? []
      existing.push(master)
      grouped.set(master.groupCode, existing)
    }
    return grouped
  })

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

  const revisionActions = createMastersRevisionActions({
    selectedMaster,
    masters,
    selectedRevisionLine,
    revisionLinesPage,
    impactAnalysis,
    loadingRevisionLines,
    loadingRevisionLine,
    submitting,
    uploadProgress,
    lastErrorMessageKey,
  })

  function clearSelected() {
    selectedMaster.value = null
    selectedRevisionLine.value = null
    revisionLinesPage.value = null
    impactAnalysis.value = null
  }

  function clearListError() {
    lastErrorMessageKey.value = null
  }

  function getDraftReviewHistory(masterId: string): MasterReviewRecord[] | undefined {
    return draftReviewHistoryByMasterId.value[masterId]
  }

  async function enrichDraftMasterReviewHistory(): Promise<void> {
    draftReviewHistoryByMasterId.value = {}
    const candidates = masters.value.filter((master) => master.status === 'DRAFT')
    await Promise.all(
      candidates.map(async (master) => {
        try {
          const page = await mastersApi.listMasterRevisionLines(master.id, 0, 5)
          const current = page.content.find((line) => line.current) ?? page.content[0]
          if (!current) {
            return
          }
          const detail = await mastersApi.getMasterRevisionLine(master.id, current.id)
          draftReviewHistoryByMasterId.value[master.id] = detail.reviewHistory
        } catch {
          /* degrade to summary-only mapping */
        }
      }),
    )
  }

  return {
    masters,
    masterListPage,
    masterListSize,
    masterListTotalElements,
    masterListTotalPages,
    selectedMaster,
    selectedRevisionLine,
    revisionLinesPage,
    impactAnalysis,
    loadingList,
    loadingDetail,
    loadingRevisionLines,
    loadingRevisionLine,
    submitting,
    uploadProgress,
    lastErrorMessageKey,
    lastListErrorRetryable,
    draftReviewHistoryByMasterId,
    mastersByGroup,
    fetchMasters,
    fetchAllMasters,
    fetchMaster,
    uploadMaster,
    submitReview,
    decideReview,
    updateMasterMetadata,
    ...revisionActions,
    clearSelected,
    clearListError,
    getDraftReviewHistory,
    enrichDraftMasterReviewHistory,
  }
})
