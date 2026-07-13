import { ref, type ComputedRef, type Ref } from 'vue'
import type { Router } from 'vue-router'
import { masterDetailPath } from '@/routing/routeKeys'
import type { useMastersStore } from '@/stores/masters'
import type { MasterReviewDecision } from '@/types/master'
import { ElMessage } from 'element-plus'

type Translate = (key: string) => string
type MastersStore = ReturnType<typeof useMastersStore>

type HasKey = (key: string) => boolean

export function createMasterRevisionDetailActions(options: {
  t: Translate
  te: HasKey
  mastersStore: MastersStore
  masterId: ComputedRef<string>
  revisionLineId: ComputedRef<string>
  router: Router
  loadFailed: Ref<boolean>
  downloading: Ref<boolean>
  submitReviewOpen: Ref<boolean>
  reviewDialogOpen: Ref<boolean>
  errorMessage: ComputedRef<string>
}) {
  const {
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
  } = options

  const reviewMode = ref<MasterReviewDecision>('APPROVED')

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
    reviewMode,
    reloadPage,
    goBackToPackage,
    handleSubmitReview,
    openReviewDialog,
    handleReviewDecision,
    handleDownload,
    formatReviewAction,
  }
}
