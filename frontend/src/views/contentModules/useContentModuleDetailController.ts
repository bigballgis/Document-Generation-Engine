import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  hasApprovedActiveVersion,
  hasApprovedStoppedVersion,
  latestDraftVersion,
  latestSubmittedVersion,
  resolveContentModuleActorId,
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
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CONTENT_MODULE_WORKSPACE_TAB_LABEL_KEYS,
  buildContentModuleWorkspaceQuery,
  resolveContentModuleWorkspaceTabFromQuery,
  type ContentModuleWorkspaceTab,
} from '@/views/contentModules/contentModuleWorkspaceTabs'

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
