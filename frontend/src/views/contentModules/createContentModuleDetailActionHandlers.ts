import type { ComputedRef, Ref } from 'vue'
import type { ComposerTranslation } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resolveContentModuleActorId } from '@/auth/contentModuleRoles'
import type { useContentModulesStore } from '@/stores/contentModules'
import type { useSessionStore } from '@/stores/session'
import type {
  ContentModuleGovernanceActorRole,
  ContentModuleLifecycleOperation,
} from '@/types/contentModule'

type ContentModulesStore = ReturnType<typeof useContentModulesStore>
type SessionStore = ReturnType<typeof useSessionStore>

export function createContentModuleReviewActions(deps: {
  t: ComposerTranslation
  moduleId: ComputedRef<string>
  errorMessage: ComputedRef<string>
  authorActorRole: ComputedRef<ContentModuleGovernanceActorRole | null>
  approverActorRole: ComputedRef<ContentModuleGovernanceActorRole | null>
  contentModulesStore: ContentModulesStore
  sessionStore: SessionStore
}) {
  const {
    t,
    moduleId,
    errorMessage,
    authorActorRole,
    approverActorRole,
    contentModulesStore,
    sessionStore,
  } = deps

  async function handleSubmitReview() {
    const actorRole = authorActorRole.value
    if (!actorRole) {
      return
    }
    try {
      const result = await ElMessageBox.prompt(
        t('contentModules.review.changeDescriptionPrompt'),
        t('contentModules.review.submitTitle'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          inputValidator: (value) =>
            value.trim().length > 0 ? true : t('contentModules.review.changeDescriptionRequired'),
        },
      )
      await contentModulesStore.transitionReview(moduleId.value, {
        operation: 'SUBMIT_FOR_REVIEW',
        actorRole,
        actorId: resolveContentModuleActorId(sessionStore.session),
        changeDescription: result.value.trim(),
      })
      ElMessage.success(t('contentModules.review.submitSuccess'))
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return
      }
      ElMessage.error(errorMessage.value || t('contentModules.error.reviewTransition'))
    }
  }

  async function handleApproveReview() {
    const actorRole = approverActorRole.value
    if (!actorRole) {
      return
    }
    try {
      await ElMessageBox.confirm(
        t('contentModules.review.approveConfirm'),
        t('contentModules.review.approveTitle'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'success',
        },
      )
      await contentModulesStore.transitionReview(moduleId.value, {
        operation: 'APPROVE_REVIEW',
        actorRole,
        actorId: resolveContentModuleActorId(sessionStore.session),
      })
      ElMessage.success(t('contentModules.review.approveSuccess'))
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return
      }
      ElMessage.error(errorMessage.value || t('contentModules.error.reviewTransition'))
    }
  }

  async function handleRejectReview() {
    const actorRole = approverActorRole.value
    if (!actorRole) {
      return
    }
    try {
      const result = await ElMessageBox.prompt(
        t('contentModules.review.rejectionReasonPrompt'),
        t('contentModules.review.rejectTitle'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          inputValidator: (value) =>
            value.trim().length > 0 ? true : t('contentModules.review.rejectionReasonRequired'),
        },
      )
      await contentModulesStore.transitionReview(moduleId.value, {
        operation: 'REJECT_REVIEW',
        actorRole,
        actorId: resolveContentModuleActorId(sessionStore.session),
        rejectionReason: result.value.trim(),
      })
      ElMessage.success(t('contentModules.review.rejectSuccess'))
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return
      }
      ElMessage.error(errorMessage.value || t('contentModules.error.reviewTransition'))
    }
  }

  return { handleSubmitReview, handleApproveReview, handleRejectReview }
}

export function createContentModuleLifecycleActions(deps: {
  t: ComposerTranslation
  moduleId: ComputedRef<string>
  errorMessage: ComputedRef<string>
  lifecycleActorRole: ComputedRef<ContentModuleGovernanceActorRole | null>
  impactDialogOpen: Ref<boolean>
  pendingLifecycleOperation: Ref<ContentModuleLifecycleOperation | null>
  contentModulesStore: ContentModulesStore
  sessionStore: SessionStore
}) {
  const {
    t,
    moduleId,
    errorMessage,
    lifecycleActorRole,
    impactDialogOpen,
    pendingLifecycleOperation,
    contentModulesStore,
    sessionStore,
  } = deps

  async function openLifecycleImpact(operation: ContentModuleLifecycleOperation) {
    pendingLifecycleOperation.value = operation
    impactDialogOpen.value = true
    try {
      await contentModulesStore.fetchLifecycleImpactPreview(moduleId.value)
    } catch {
      ElMessage.error(errorMessage.value || t('contentModules.error.loadImpactPreview'))
      impactDialogOpen.value = false
    }
  }

  async function confirmLifecycleOperation() {
    const operation = pendingLifecycleOperation.value
    const actorRole = lifecycleActorRole.value
    const impact = contentModulesStore.lifecycleImpactPreview
    if (!operation || !actorRole || !impact) {
      return
    }
    try {
      await contentModulesStore.applyLifecycleOperation(moduleId.value, {
        operationType: operation,
        actorRole,
        actorId: resolveContentModuleActorId(sessionStore.session),
        impactSummaryViewed: true,
        secondConfirmation: true,
        impactSummary: operation === 'RECOVER' ? undefined : impact,
      })
      impactDialogOpen.value = false
      pendingLifecycleOperation.value = null
      ElMessage.success(t(`contentModules.lifecycle.success.${operation}`))
    } catch {
      ElMessage.error(errorMessage.value || t('contentModules.error.lifecycle'))
    }
  }

  return { openLifecycleImpact, confirmLifecycleOperation }
}
