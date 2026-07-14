import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as mastersApi from '@/api/masters'
import { createMastersRevisionActions } from '@/stores/createMastersRevisionActions'
import { createMastersCatalogActions } from '@/stores/createMastersCatalogActions'
import type {
  MasterDocumentDetail,
  MasterDocumentSummary,
  MasterImpactAnalysis,
  MasterRevisionLineDetail,
  MasterRevisionLinePage,
  MasterReviewRecord,
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
  const currentRevisionLineIdByMasterId = ref<Record<string, string>>({})

  const mastersByGroup = computed(() => {
    const grouped = new Map<string, MasterDocumentSummary[]>()
    for (const master of masters.value) {
      const existing = grouped.get(master.groupCode) ?? []
      existing.push(master)
      grouped.set(master.groupCode, existing)
    }
    return grouped
  })

  const catalogActions = createMastersCatalogActions({
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
  })

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
          currentRevisionLineIdByMasterId.value[master.id] = current.id
          const detail = await mastersApi.getMasterRevisionLine(master.id, current.id)
          draftReviewHistoryByMasterId.value[master.id] = detail.reviewHistory
        } catch {
          /* degrade to summary-only mapping */
        }
      }),
    )
  }

  /** Resolve current revision line ids for dashboard master-review deep links (CE-U09). */
  async function enrichCurrentRevisionLineIdsForWorkflow(): Promise<void> {
    const candidates = masters.value.filter(
      (master) =>
        master.status === 'PENDING_REVIEW' ||
        master.status === 'REJECTED' ||
        master.status === 'DRAFT',
    )
    await Promise.all(
      candidates.map(async (master) => {
        if (currentRevisionLineIdByMasterId.value[master.id]) {
          return
        }
        try {
          const page = await mastersApi.listMasterRevisionLines(master.id, 0, 5)
          const current = page.content.find((line) => line.current) ?? page.content[0]
          if (current) {
            currentRevisionLineIdByMasterId.value[master.id] = current.id
          }
        } catch {
          /* degrade to Hub fallback path */
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
    currentRevisionLineIdByMasterId,
    mastersByGroup,
    ...catalogActions,
    ...revisionActions,
    clearSelected,
    clearListError,
    getDraftReviewHistory,
    enrichDraftMasterReviewHistory,
    enrichCurrentRevisionLineIdsForWorkflow,
  }
})
