import type { Ref } from 'vue'
import * as mastersApi from '@/api/masters'
import { resolveApiErrorMessageKey, resolveStoreErrorMessageKey } from '@/api/http'
import type {
  MasterDocumentDetail,
  MasterDocumentSummary,
  MasterImpactAnalysis,
  MasterRevisionLineDetail,
  MasterRevisionLinePage,
} from '@/types/master'
import { applyUpdatedMaster } from '@/stores/mastersStoreHelpers'

export function createMastersRevisionActions(deps: {
  selectedMaster: Ref<MasterDocumentDetail | null>
  masters: Ref<MasterDocumentSummary[]>
  selectedRevisionLine: Ref<MasterRevisionLineDetail | null>
  revisionLinesPage: Ref<MasterRevisionLinePage | null>
  impactAnalysis: Ref<MasterImpactAnalysis | null>
  loadingRevisionLines: Ref<boolean>
  loadingRevisionLine: Ref<boolean>
  submitting: Ref<boolean>
  uploadProgress: Ref<number | null>
  lastErrorMessageKey: Ref<string | null>
}) {
  const {
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
  } = deps

  async function fetchImpactAnalysis(masterId: string): Promise<void> {
    lastErrorMessageKey.value = null
    try {
      impactAnalysis.value = await mastersApi.getMasterImpactAnalysis(masterId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.loadImpact')
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
    uploadProgress.value = null
    lastErrorMessageKey.value = null
    try {
      const updated = await mastersApi.replaceMasterFile(masterId, file, {
        onUploadProgress: (percent) => {
          uploadProgress.value = percent
        },
      })
      applyUpdatedMaster(selectedMaster, masters, updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'masters.error.replaceFile')
      throw error
    } finally {
      submitting.value = false
      uploadProgress.value = null
    }
  }

  return {
    fetchImpactAnalysis,
    fetchRevisionLines,
    fetchRevisionLine,
    downloadMasterFile,
    downloadRevisionLineFile,
    replaceMasterFile,
  }
}
