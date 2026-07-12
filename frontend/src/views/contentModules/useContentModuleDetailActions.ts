import type { ComputedRef, Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  latestDraftVersion,
  resolveContentModuleActorId,
} from '@/auth/contentModuleRoles'
import { useContentModulesStore } from '@/stores/contentModules'
import { useSessionStore } from '@/stores/session'
import type {
  ContentModuleGovernanceActorRole,
  ContentModuleLifecycleOperation,
  ContentModuleVersion,
} from '@/types/contentModule'

export interface UseContentModuleDetailActionsOptions {
  moduleId: ComputedRef<string>
  versions: ComputedRef<ContentModuleVersion[]>
  errorMessage: ComputedRef<string>
  authorActorRole: ComputedRef<ContentModuleGovernanceActorRole | null>
  approverActorRole: ComputedRef<ContentModuleGovernanceActorRole | null>
  lifecycleActorRole: ComputedRef<ContentModuleGovernanceActorRole | null>
  versionDialogOpen: Ref<boolean>
  versionDialogMode: Ref<'create' | 'edit'>
  selectedVersion: Ref<ContentModuleVersion | null>
  impactDialogOpen: Ref<boolean>
  pendingLifecycleOperation: Ref<ContentModuleLifecycleOperation | null>
  reloadPage: () => Promise<void>
}

export function useContentModuleDetailActions(options: UseContentModuleDetailActionsOptions) {
  const {
    moduleId,
    versions,
    errorMessage,
    authorActorRole,
    approverActorRole,
    lifecycleActorRole,
    versionDialogOpen,
    versionDialogMode,
    selectedVersion,
    impactDialogOpen,
    pendingLifecycleOperation,
    reloadPage,
  } = options

  const { t } = useI18n()
  const contentModulesStore = useContentModulesStore()
  const sessionStore = useSessionStore()

  function openCreateVersionDialog() {
    versionDialogMode.value = 'create'
    selectedVersion.value = null
    versionDialogOpen.value = true
  }

  function openEditDraftDialog() {
    const draft = latestDraftVersion(versions.value)
    if (!draft) {
      return
    }
    versionDialogMode.value = 'edit'
    selectedVersion.value = draft as ContentModuleVersion
    versionDialogOpen.value = true
  }

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

  async function handleVersionSaved() {
    ElMessage.success(t('contentModules.version.saveSuccess'))
    await reloadPage()
  }

  return {
    openCreateVersionDialog,
    openEditDraftDialog,
    handleSubmitReview,
    handleApproveReview,
    handleRejectReview,
    openLifecycleImpact,
    confirmLifecycleOperation,
    handleVersionSaved,
  }
}
