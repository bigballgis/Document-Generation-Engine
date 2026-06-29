import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as mastersApi from '@/api/masters'
import { resolveApiErrorMessageKey, resolveStoreErrorMessageKey } from '@/api/http'
import type {
  CreateMasterPayload,
  DecideMasterReviewPayload,
  MasterDocumentDetail,
  MasterDocumentSummary,
  MasterImpactAnalysis,
  MasterRevisionLineDetail,
  MasterRevisionLinePage,
  SubmitMasterReviewPayload,
  UpdateMasterMetadataPayload,
} from '@/types/master'

export const useMastersStore = defineStore('masters', () => {
  const masters = ref<MasterDocumentSummary[]>([])
  const selectedMaster = ref<MasterDocumentDetail | null>(null)
  const selectedRevisionLine = ref<MasterRevisionLineDetail | null>(null)
  const revisionLinesPage = ref<MasterRevisionLinePage | null>(null)
  const impactAnalysis = ref<MasterImpactAnalysis | null>(null)
  const loadingList = ref(false)
  const loadingDetail = ref(false)
  const loadingRevisionLines = ref(false)
  const loadingRevisionLine = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)

  const mastersByGroup = computed(() => {
    const grouped = new Map<string, MasterDocumentSummary[]>()
    for (const master of masters.value) {
      const existing = grouped.get(master.groupCode) ?? []
      existing.push(master)
      grouped.set(master.groupCode, existing)
    }
    return grouped
  })

  async function fetchMasters(): Promise<void> {
    loadingList.value = true
    lastErrorMessageKey.value = null
    try {
      masters.value = await mastersApi.listMasters()
    } catch (error) {
      lastErrorMessageKey.value = resolveStoreErrorMessageKey(error, 'masters.error.loadList')
      throw error
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

  async function fetchImpactAnalysis(masterId: string): Promise<void> {
    lastErrorMessageKey.value = null
    try {
      impactAnalysis.value = await mastersApi.getMasterImpactAnalysis(masterId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.loadImpact')
      throw error
    }
  }

  async function uploadMaster(payload: CreateMasterPayload, file: File): Promise<MasterDocumentDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const created = await mastersApi.createMaster(payload, file)
      masters.value = [toSummary(created), ...masters.value.filter((item) => item.id !== created.id)]
      selectedMaster.value = created
      return created
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.upload')
      throw error
    } finally {
      submitting.value = false
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
      applyUpdatedMaster(updated)
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
      applyUpdatedMaster(updated)
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
      applyUpdatedMaster(updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.updateMetadata')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function downloadMasterFile(masterId: string): Promise<void> {
    lastErrorMessageKey.value = null
    try {
      const { downloadBlobExport } = await import('@/utils/downloadExport')
      const { blob, filename } = await mastersApi.downloadMasterFile(masterId)
      downloadBlobExport(filename, blob)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.download')
      throw error
    }
  }

  async function fetchRevisionLines(
    masterId: string,
    page = 0,
    size = 20,
  ): Promise<MasterRevisionLinePage> {
    loadingRevisionLines.value = true
    lastErrorMessageKey.value = null
    try {
      const pageResult = await mastersApi.listMasterRevisionLines(masterId, page, size)
      revisionLinesPage.value = pageResult
      return pageResult
    } catch (error) {
      lastErrorMessageKey.value = resolveStoreErrorMessageKey(error, 'masters.revisionLines.loadError')
      throw error
    } finally {
      loadingRevisionLines.value = false
    }
  }

  async function fetchRevisionLine(
    masterId: string,
    revisionLineId: string,
  ): Promise<MasterRevisionLineDetail> {
    loadingRevisionLine.value = true
    lastErrorMessageKey.value = null
    try {
      selectedRevisionLine.value = await mastersApi.getMasterRevisionLine(masterId, revisionLineId)
      return selectedRevisionLine.value
    } catch (error) {
      lastErrorMessageKey.value = resolveStoreErrorMessageKey(error, 'masters.revision.loadError')
      throw error
    } finally {
      loadingRevisionLine.value = false
    }
  }

  async function downloadRevisionLineFile(masterId: string, revisionLineId: string): Promise<void> {
    lastErrorMessageKey.value = null
    try {
      const { downloadBlobExport } = await import('@/utils/downloadExport')
      const { blob, filename } = await mastersApi.downloadMasterRevisionLineFile(
        masterId,
        revisionLineId,
      )
      downloadBlobExport(filename, blob)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.download')
      throw error
    }
  }

  async function replaceMasterFile(masterId: string, file: File): Promise<MasterDocumentDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await mastersApi.replaceMasterFile(masterId, file)
      applyUpdatedMaster(updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.replaceFile')
      throw error
    } finally {
      submitting.value = false
    }
  }

  function applyUpdatedMaster(updated: MasterDocumentDetail) {
    selectedMaster.value = updated
    masters.value = masters.value.map((item) => (item.id === updated.id ? toSummary(updated) : item))
  }

  function toSummary(detail: MasterDocumentDetail): MasterDocumentSummary {
    return {
      id: detail.id,
      groupCode: detail.groupCode,
      name: detail.name,
      status: detail.status,
      originalFilename: detail.originalFilename,
      anchorCount: detail.anchors.length,
      updatedBy: detail.updatedBy,
      updatedAt: detail.updatedAt,
    }
  }

  function clearSelected() {
    selectedMaster.value = null
    selectedRevisionLine.value = null
    revisionLinesPage.value = null
    impactAnalysis.value = null
  }

  function clearListError() {
    lastErrorMessageKey.value = null
  }

  return {
    masters,
    selectedMaster,
    selectedRevisionLine,
    revisionLinesPage,
    impactAnalysis,
    loadingList,
    loadingDetail,
    loadingRevisionLines,
    loadingRevisionLine,
    submitting,
    lastErrorMessageKey,
    mastersByGroup,
    fetchMasters,
    fetchMaster,
    fetchImpactAnalysis,
    fetchRevisionLines,
    fetchRevisionLine,
    uploadMaster,
    submitReview,
    decideReview,
    updateMasterMetadata,
    downloadMasterFile,
    downloadRevisionLineFile,
    replaceMasterFile,
    clearSelected,
    clearListError,
  }
})
