import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { canReviewMasters, sessionContext } from '@/auth/roles'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useCapabilities } from '@/composables/useCapabilities'
import { shouldShowMasterDesignerJourney } from '@/utils/masterDesignerJourney'
import { sortMasterAnchorsByDocumentSequence } from '@/utils/masterAnchorDocumentOrder'
import {
  MASTER_REVISION_WORKSPACE_TAB_LABEL_KEYS,
  buildMasterRevisionWorkspaceQuery,
  resolveMasterRevisionWorkspaceTabFromQuery,
  type MasterRevisionWorkspaceTab,
} from '@/views/masters/masterRevisionWorkspaceTabs'
import type { MasterAnchor, MasterDocumentDetail } from '@/types/master'
import { formatMasterRevisionLineLabel } from '@/utils/masterRevisionLineLabel'
import { createMasterRevisionDetailActions } from '@/views/masters/createMasterRevisionDetailActions'

export function useMasterRevisionDetailController() {
  const { t, te } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const mastersStore = useMastersStore()
  const sessionStore = useSessionStore()
  const { manageMasters, reviewMasters } = useCapabilities()

  const submitReviewOpen = ref(false)
  const reviewDialogOpen = ref(false)
  const loadFailed = ref(false)
  const downloading = ref(false)
  const editLabelOpen = ref(false)
  const editingAnchor = ref<MasterAnchor | null>(null)
  const savingAnchorLabel = ref(false)
  const activeWorkspaceTab = ref<MasterRevisionWorkspaceTab>(
    resolveMasterRevisionWorkspaceTabFromQuery(route.query),
  )

  const workspaceTabs = computed(() =>
    (['design', 'approval'] as const).map((name) => ({
      name,
      labelKey: MASTER_REVISION_WORKSPACE_TAB_LABEL_KEYS[name],
    })),
  )

  watch(
    () => route.query.workspaceTab,
    () => {
      activeWorkspaceTab.value = resolveMasterRevisionWorkspaceTabFromQuery(route.query)
    },
  )

  watch(activeWorkspaceTab, (tab) => {
    if (resolveMasterRevisionWorkspaceTabFromQuery(route.query) === tab) {
      return
    }
    void router.replace({
      query: buildMasterRevisionWorkspaceQuery(route.query, tab),
    })
  })

  const masterId = computed(() => String(route.params.masterId ?? ''))
  const revisionLineId = computed(() => String(route.params.revisionLineId ?? ''))
  const master = computed(() => mastersStore.selectedMaster)
  const revisionLine = computed(() => mastersStore.selectedRevisionLine)

  const workflowMaster = computed<MasterDocumentDetail | null>(() => {
    if (!master.value || !revisionLine.value) {
      return null
    }
    return {
      ...master.value,
      status: revisionLine.value.status,
      originalFilename: revisionLine.value.originalFilename,
      changeSummary: revisionLine.value.changeSummary ?? null,
      anchors: revisionLine.value.anchors,
      reviewHistory: revisionLine.value.reviewHistory,
      updatedAt: revisionLine.value.updatedAt,
      updatedBy: revisionLine.value.updatedBy,
    }
  })

  const anchorsSource = computed(() =>
    sortMasterAnchorsByDocumentSequence(revisionLine.value?.anchors ?? []),
  )
  const { filters: anchorColumnFilters, filteredRows: filteredAnchors } = useDataTableFilters(
    anchorsSource,
    [
      { key: 'anchorId', getValue: (row) => row.anchorId },
      { key: 'displayLabel', getValue: (row) => row.displayLabel },
    ],
  )

  const isCurrentRevision = computed(() => revisionLine.value?.current === true)
  const canReview = computed(() => canReviewMasters(sessionContext(sessionStore.session)))
  const canSubmitForReview = computed(
    () =>
      isCurrentRevision.value &&
      (revisionLine.value?.status === 'DRAFT' || revisionLine.value?.status === 'REJECTED'),
  )
  const canDecideReview = computed(
    () =>
      canReview.value &&
      isCurrentRevision.value &&
      revisionLine.value?.status === 'PENDING_REVIEW',
  )

  const showDesignerJourney = computed(() => {
    if (!workflowMaster.value) {
      return false
    }
    return shouldShowMasterDesignerJourney({
      roles: sessionStore.session?.roles ?? [],
      manageMasters: manageMasters.value,
      reviewMasters: reviewMasters.value,
      status: workflowMaster.value.status,
    })
  })

  const journeyContext = computed(() => {
    if (!workflowMaster.value) {
      return null
    }
    return {
      status: workflowMaster.value.status,
      originalFilename: workflowMaster.value.originalFilename,
      anchorsLength: workflowMaster.value.anchors.length,
      reviewHistory: workflowMaster.value.reviewHistory,
    }
  })

  const canWriteJourney = computed(
    () =>
      Boolean(
        manageMasters.value &&
          isCurrentRevision.value &&
          workflowMaster.value &&
          workflowMaster.value.status !== 'PENDING_REVIEW',
      ),
  )

  /** CE-U06 — same write gate as Hub journey (manageMasters + current + not PENDING_REVIEW). */
  const canEditAnchorDisplayLabel = computed(() => canWriteJourney.value)

  const errorMessage = computed(() => {
    const key = mastersStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('masters.error.loadDetail')
  })

  const revisionLineTitle = computed(() =>
    formatMasterRevisionLineLabel(
      t,
      revisionLine.value?.lineLabel,
      revisionLine.value?.revisionSequence,
    ),
  )

  const actions = createMasterRevisionDetailActions({
    t,
    te,
    mastersStore,
    masterId,
    revisionLineId,
    router,
    loadFailed,
    downloading,
    submitReviewOpen,
    reviewDialogOpen,
    errorMessage,
  })

  function openEditAnchorLabel(anchor: MasterAnchor) {
    if (!canEditAnchorDisplayLabel.value) {
      return
    }
    editingAnchor.value = anchor
    editLabelOpen.value = true
  }

  async function handleSaveAnchorDisplayLabel(payload: { displayLabel: string }) {
    if (!editingAnchor.value || !canEditAnchorDisplayLabel.value) {
      return
    }
    savingAnchorLabel.value = true
    try {
      await mastersStore.updateRevisionLineAnchorDisplayLabel(
        masterId.value,
        revisionLineId.value,
        editingAnchor.value.anchorId,
        payload,
      )
      ElMessage.success(t('masters.revision.anchorLabelSaveSuccess'))
      editLabelOpen.value = false
      editingAnchor.value = null
    } catch {
      ElMessage.error(errorMessage.value || t('masters.error.updateAnchorLabel'))
    } finally {
      savingAnchorLabel.value = false
    }
  }

  onMounted(async () => {
    await actions.reloadPage()
  })

  onUnmounted(() => {
    mastersStore.clearSelected()
  })

  return {
    t,
    mastersStore,
    submitReviewOpen,
    reviewDialogOpen,
    reviewMode: actions.reviewMode,
    loadFailed,
    downloading,
    editLabelOpen,
    editingAnchor,
    savingAnchorLabel,
    activeWorkspaceTab,
    workspaceTabs,
    masterId,
    revisionLineId,
    master,
    revisionLine,
    workflowMaster,
    anchorColumnFilters,
    filteredAnchors,
    isCurrentRevision,
    canSubmitForReview,
    canDecideReview,
    showDesignerJourney,
    journeyContext,
    canWriteJourney,
    canEditAnchorDisplayLabel,
    revisionLineTitle,
    reloadPage: actions.reloadPage,
    goBackToPackage: actions.goBackToPackage,
    handleSubmitReview: actions.handleSubmitReview,
    openReviewDialog: actions.openReviewDialog,
    handleReviewDecision: actions.handleReviewDecision,
    handleDownload: actions.handleDownload,
    formatReviewAction: actions.formatReviewAction,
    openEditAnchorLabel,
    handleSaveAnchorDisplayLabel,
  }
}
