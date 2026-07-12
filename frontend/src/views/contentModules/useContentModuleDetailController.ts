import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  hasApprovedActiveVersion,
  hasApprovedStoppedVersion,
  latestDraftVersion,
  latestSubmittedVersion,
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
import { DEFAULT_STRUCTURED_CONTENT_JSON, serializeStructuredContent } from '@/utils/structuredContentNodes'
import { normalizeStructuredContentJson } from '@/utils/structuredContentCompat'
import {
  CONTENT_MODULE_WORKSPACE_TAB_LABEL_KEYS,
  buildContentModuleWorkspaceQuery,
  resolveContentModuleWorkspaceTabFromQuery,
  type ContentModuleWorkspaceTab,
} from '@/views/contentModules/contentModuleWorkspaceTabs'
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

  const canSubmitReview = computed(
    () => authorContentModules.value && Boolean(latestDraftVersion(versions.value)),
  )
  const canApproveReview = computed(
    () => decideContentModuleReviews.value && Boolean(latestSubmittedVersion(versions.value)),
  )
  const canCreateVersion = computed(() => authorContentModules.value)
  const canEditDraft = computed(
    () => authorContentModules.value && Boolean(latestDraftVersion(versions.value)),
  )
  const canStop = computed(
    () => manageContentModuleLifecycle.value && hasApprovedActiveVersion(versions.value),
  )
  const canRecover = computed(
    () => manageContentModuleLifecycle.value && hasApprovedStoppedVersion(versions.value),
  )
  const canDeprecate = canRecover

  const previewVersion = computed(() => {
    const draft = latestDraftVersion(versions.value)
    if (draft) {
      return draft as ContentModuleVersion
    }
    return (
      versions.value.find(
        (version) =>
          version.reviewState === 'APPROVED' && (version.lifecycleState ?? 'ACTIVE') === 'ACTIVE',
      ) ?? null
    )
  })

  const previewContentJson = computed(() => {
    const version = previewVersion.value
    if (!version?.contentStructureJson) {
      return DEFAULT_STRUCTURED_CONTENT_JSON
    }
    return serializeStructuredContent(normalizeStructuredContentJson(version.contentStructureJson))
  })

  const previewVersionLabel = computed(() => {
    if (!previewVersion.value) {
      return ''
    }
    return t('contentModules.detail.contentPreviewVersion', {
      version: previewVersion.value.semanticVersion,
      state: previewVersion.value.reviewState,
    })
  })

  const errorMessage = computed(() => {
    const key = contentModulesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('contentModules.error.loadDetail')
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
    canSubmitReview,
    canApproveReview,
    canCreateVersion,
    canEditDraft,
    canStop,
    canRecover,
    canDeprecate,
    previewVersion,
    previewContentJson,
    previewVersionLabel,
    lifecycleOperationLabelKey,
    reloadPage,
    goBackToList,
    ...actions,
  }
}
