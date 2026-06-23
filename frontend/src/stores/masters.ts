import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as mastersApi from '@/api/masters'
import { resolveApiErrorMessageKey } from '@/api/http'
import type {
  CreateMasterPayload,
  DecideMasterReviewPayload,
  MasterDocumentDetail,
  MasterDocumentSummary,
  MasterImpactAnalysis,
  SubmitMasterReviewPayload,
  UpdateMasterMetadataPayload,
} from '@/types/master'

export const useMastersStore = defineStore('masters', () => {
  const masters = ref<MasterDocumentSummary[]>([])
  const selectedMaster = ref<MasterDocumentDetail | null>(null)
  const impactAnalysis = ref<MasterImpactAnalysis | null>(null)
  const loadingList = ref(false)
  const loadingDetail = ref(false)
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
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.loadList')
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
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.loadDetail')
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
      updatedAt: detail.updatedAt,
    }
  }

  function clearSelected() {
    selectedMaster.value = null
    impactAnalysis.value = null
  }

  return {
    masters,
    selectedMaster,
    impactAnalysis,
    loadingList,
    loadingDetail,
    submitting,
    lastErrorMessageKey,
    mastersByGroup,
    fetchMasters,
    fetchMaster,
    fetchImpactAnalysis,
    uploadMaster,
    submitReview,
    decideReview,
    updateMasterMetadata,
    clearSelected,
  }
})
