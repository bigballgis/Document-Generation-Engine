import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  resolveContentModuleApproverActorRole,
  resolveContentModuleAuthorActorRole,
  resolveContentModuleLifecycleActorRole,
} from '@/auth/contentModuleRoles'
import { useCapabilities } from '@/composables/useCapabilities'
import { useContentModulesStore } from '@/stores/contentModules'
import { useSessionStore } from '@/stores/session'
import type {
  ContentModuleLifecycleOperation,
  ContentModuleVersion,
} from '@/types/contentModule'
import {
  CONTENT_MODULE_WORKSPACE_TAB_LABEL_KEYS,
  buildContentModuleWorkspaceQuery,
  resolveContentModuleWorkspaceTabFromQuery,
  type ContentModuleWorkspaceTab,
} from '@/views/contentModules/contentModuleWorkspaceTabs'
import { createContentModuleDetailDerived } from '@/views/contentModules/createContentModuleDetailDerived'
import { useContentModuleDetailActions } from '@/views/contentModules/useContentModuleDetailActions'

export function useContentModuleDetailController() {
  const { t, te } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const contentModulesStore = useContentModulesStore()
  const sessionStore = useSessionStore()
  const { authorContentModules, decideContentModuleReviews, manageContentModuleLifecycle } =
    useCapabilities()

  const loadFailed = ref(false)
  const versionDialogOpen = ref(false)
  const versionDialogMode = ref<'create' | 'edit'>('create')
  const selectedVersion = ref<ContentModuleVersion | null>(null)
  const impactDialogOpen = ref(false)
  const pendingLifecycleOperation = ref<ContentModuleLifecycleOperation | null>(null)
  const activeWorkspaceTab = ref<ContentModuleWorkspaceTab>(
    resolveContentModuleWorkspaceTabFromQuery(route.query),
  )

  const workspaceTabs = computed(() =>
    (['versions', 'content', 'lifecycle'] as const).map((name) => ({
      name,
      labelKey: CONTENT_MODULE_WORKSPACE_TAB_LABEL_KEYS[name],
    })),
  )

  watch(
    () => route.query.workspaceTab,
    () => {
      activeWorkspaceTab.value = resolveContentModuleWorkspaceTabFromQuery(route.query)
    },
  )

  watch(activeWorkspaceTab, (tab) => {
    if (resolveContentModuleWorkspaceTabFromQuery(route.query) === tab) {
      return
    }
    void router.replace({
      query: buildContentModuleWorkspaceQuery(route.query, tab),
    })
  })

  const moduleId = computed(() => String(route.params.moduleId ?? ''))
  const detail = computed(() => contentModulesStore.selectedModule)
  const versions = computed(() => detail.value?.versions ?? [])

  const authorActorRole = computed(() =>
    resolveContentModuleAuthorActorRole(sessionStore.session?.roles ?? []),
  )
  const approverActorRole = computed(() =>
    resolveContentModuleApproverActorRole(sessionStore.session?.roles ?? []),
  )
  const lifecycleActorRole = computed(() =>
    resolveContentModuleLifecycleActorRole(sessionStore.session?.roles ?? []),
  )

  const derived = createContentModuleDetailDerived({
    versions,
    authorContentModules,
    decideContentModuleReviews,
    manageContentModuleLifecycle,
    t,
    getLastErrorMessageKey: () => contentModulesStore.lastErrorMessageKey,
    te,
  })

  const lifecycleOperationLabelKey = computed(() => {
    switch (pendingLifecycleOperation.value) {
      case 'STOP_USE':
        return 'contentModules.lifecycle.stopDescription'
      case 'RECOVER':
        return 'contentModules.lifecycle.recoverDescription'
      case 'DEPRECATE':
        return 'contentModules.lifecycle.deprecateDescription'
      default:
        return 'contentModules.lifecycle.impactTitle'
    }
  })

  onMounted(async () => {
    await reloadPage()
  })

  onUnmounted(() => {
    contentModulesStore.clearSelected()
  })

  async function reloadPage() {
    loadFailed.value = false
    try {
      await contentModulesStore.fetchModule(moduleId.value)
    } catch {
      loadFailed.value = true
    }
  }

  function goBackToList() {
    router.push('/content-modules')
  }

  const actions = useContentModuleDetailActions({
    moduleId,
    versions,
    errorMessage: derived.errorMessage,
    authorActorRole,
    approverActorRole,
    lifecycleActorRole,
    versionDialogOpen,
    versionDialogMode,
    selectedVersion,
    impactDialogOpen,
    pendingLifecycleOperation,
    reloadPage,
  })

  return {
    t,
    contentModulesStore,
    loadFailed,
    versionDialogOpen,
    versionDialogMode,
    selectedVersion,
    impactDialogOpen,
    activeWorkspaceTab,
    workspaceTabs,
    moduleId,
    detail,
    versions,
    canSubmitReview: derived.canSubmitReview,
    canApproveReview: derived.canApproveReview,
    canCreateVersion: derived.canCreateVersion,
    canEditDraft: derived.canEditDraft,
    canStop: derived.canStop,
    canRecover: derived.canRecover,
    canDeprecate: derived.canDeprecate,
    previewVersion: derived.previewVersion,
    previewContentJson: derived.previewContentJson,
    previewVersionLabel: derived.previewVersionLabel,
    lifecycleOperationLabelKey,
    reloadPage,
    goBackToList,
    ...actions,
  }
}
