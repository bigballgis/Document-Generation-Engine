import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { canReviewMasters, sessionContext } from '@/auth/roles'
import { masterDetailPath } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useCapabilities } from '@/composables/useCapabilities'
import { shouldShowMasterDesignerJourney } from '@/utils/masterDesignerJourney'
import {
  MASTER_REVISION_WORKSPACE_TAB_LABEL_KEYS,
  buildMasterRevisionWorkspaceQuery,
  resolveMasterRevisionWorkspaceTabFromQuery,
  type MasterRevisionWorkspaceTab,
} from '@/views/masters/masterRevisionWorkspaceTabs'
import type { MasterDocumentDetail, MasterReviewDecision } from '@/types/master'
import { formatMasterRevisionLineLabel } from '@/utils/masterRevisionLineLabel'
import { ElMessage } from 'element-plus'

export function useMasterRevisionDetailController() {
  const { t, te } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const mastersStore = useMastersStore()
  const sessionStore = useSessionStore()
  const { manageMasters, reviewMasters } = useCapabilities()

  const submitReviewOpen = ref(false)
  const reviewDialogOpen = ref(false)
  const reviewMode = ref<MasterReviewDecision>('APPROVED')
  const loadFailed = ref(false)
  const downloading = ref(false)
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

  const anchorsSource = computed(() => revisionLine.value?.anchors ?? [])
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
    () => canReview.value && isCurrentRevision.value && revisionLine.value?.status === 'PENDING_REVIEW',
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

  onMounted(async () => {
    await reloadPage()
  })

  onUnmounted(() => {
    mastersStore.clearSelected()
  })

  async function reloadPage() {
    loadFailed.value = false
    try {
      await mastersStore.fetchMaster(masterId.value)
      await mastersStore.fetchRevisionLine(masterId.value, revisionLineId.value)
    } catch {
      loadFailed.value = true
    }
  }

  function goBackToPackage() {
    router.push(masterDetailPath(masterId.value))
  }

  async function handleSubmitReview(payload: { changeSummary: string }) {
    try {
      await mastersStore.submitReview(masterId.value, payload)
      submitReviewOpen.value = false
      ElMessage.success(t('masters.submitReview.success'))
      await reloadPage()
    } catch {
      ElMessage.error(errorMessage.value || t('masters.error.submitReview'))
    }
  }

  function openReviewDialog(mode: MasterReviewDecision) {
    reviewMode.value = mode
    reviewDialogOpen.value = true
  }

  async function handleReviewDecision(payload: {
    decision: MasterReviewDecision
    commentSummary: string
  }) {
    try {
      await mastersStore.decideReview(masterId.value, {
        decision: payload.decision,
        commentSummary: payload.commentSummary || undefined,
      })
      reviewDialogOpen.value = false
      ElMessage.success(
        t(payload.decision === 'APPROVED' ? 'masters.review.approveSuccess' : 'masters.review.rejectSuccess'),
      )
      await reloadPage()
    } catch {
      ElMessage.error(errorMessage.value || t('masters.error.decideReview'))
    }
  }

  async function handleDownload() {
    downloading.value = true
    try {
      await mastersStore.downloadRevisionLineFile(masterId.value, revisionLineId.value)
      ElMessage.success(t('masters.download.success'))
    } catch {
      ElMessage.error(errorMessage.value || t('masters.error.download'))
    } finally {
      downloading.value = false
    }
  }

  function formatReviewAction(action: string): string {
    const key = `masters.reviewHistory.action.${action}`
    return te(key) ? t(key) : action
  }

  return {
    t,
    mastersStore,
    submitReviewOpen,
    reviewDialogOpen,
    reviewMode,
    loadFailed,
    downloading,
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
    revisionLineTitle,
    reloadPage,
    goBackToPackage,
    handleSubmitReview,
    openReviewDialog,
    handleReviewDecision,
    handleDownload,
    formatReviewAction,
  }
}
