import type { ComputedRef, Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { latestDraftVersion } from '@/auth/contentModuleRoles'
import { useContentModulesStore } from '@/stores/contentModules'
import { useSessionStore } from '@/stores/session'
import type {
  ContentModuleGovernanceActorRole,
  ContentModuleLifecycleOperation,
  ContentModuleVersion,
} from '@/types/contentModule'
import {
  createContentModuleLifecycleActions,
  createContentModuleReviewActions,
} from '@/views/contentModules/createContentModuleDetailActionHandlers'

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

  const reviewActions = createContentModuleReviewActions({
    t,
    moduleId,
    errorMessage,
    authorActorRole,
    approverActorRole,
    contentModulesStore,
    sessionStore,
  })

  const lifecycleActions = createContentModuleLifecycleActions({
    t,
    moduleId,
    errorMessage,
    lifecycleActorRole,
    impactDialogOpen,
    pendingLifecycleOperation,
    contentModulesStore,
    sessionStore,
  })

  async function handleVersionSaved() {
    ElMessage.success(t('contentModules.version.saveSuccess'))
    await reloadPage()
  }

  return {
    openCreateVersionDialog,
    openEditDraftDialog,
    ...reviewActions,
    ...lifecycleActions,
    handleVersionSaved,
  }
}
