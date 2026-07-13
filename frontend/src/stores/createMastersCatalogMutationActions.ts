import type { Ref } from 'vue'
import * as mastersApi from '@/api/masters'
import { resolveApiErrorMessageKey } from '@/api/http'
import { applyUpdatedMaster } from '@/stores/mastersStoreHelpers'
import type {
  DecideMasterReviewPayload,
  MasterDocumentDetail,
  MasterDocumentSummary,
  SubmitMasterReviewPayload,
  UpdateMasterMetadataPayload,
} from '@/types/master'

export function createMastersCatalogMutationActions(deps: {
  masters: Ref<MasterDocumentSummary[]>
  selectedMaster: Ref<MasterDocumentDetail | null>
  submitting: Ref<boolean>
  lastErrorMessageKey: Ref<string | null>
}) {
  const { masters, selectedMaster, submitting, lastErrorMessageKey } = deps

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
    submitReview,
    decideReview,
    updateMasterMetadata,
  }
}
