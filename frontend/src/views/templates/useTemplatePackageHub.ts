import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { WorkspaceTabOption } from '@/components/common/WorkspaceTabShell.vue'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import {
  ROUTE_PATH_BY_KEY,
  ROUTE_KEYS,
  templateDevVersionPath,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import { isTemplateExportEligible } from '@/utils/templateExportEligibility'
import { templateDetailTabLabelKey } from '@/views/templates/templateDetailTabs'
import { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import type { DeleteTemplatePayload } from '@/types/template'

const HUB_SECONDARY_TABS = ['overview', 'apiAccess'] as const
type HubSecondaryTab = (typeof HUB_SECONDARY_TABS)[number]

type HubWorkspaceExpose = {
  reloadVersionLines: () => Promise<void> | undefined
  revealCredentialSecret: (externalId: string, secret: string) => void
}

export function useTemplatePackageHub() {
  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const route = useRoute()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const {
    authorTemplates,
    deleteTemplates,
    exportTemplates,
    editTemplateMetadata,
    manageReleaseVersionState,
  } = useCapabilities()
  const { confirmAction } = useConfirmAction()

  const metadataEditOpen = ref(false)
  const loadFailed = ref(false)
  const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)
  const workspaceRef = ref<HubWorkspaceExpose | null>(null)

  const templateId = computed(() => String(route.params.templateId ?? ''))
  const secondaryTab = ref<HubSecondaryTab | undefined>(undefined)

  const template = computed(() => {
    const selected = templatesStore.selectedTemplate
    if (!selected || selected.id !== templateId.value) {
      return null
    }
    return selected
  })

  const showDetailSkeleton = computed(
    () =>
      templatesStore.loadingDetail ||
      (templatesStore.selectedTemplate !== null &&
        templatesStore.selectedTemplate.id !== templateId.value),
  )

  const errorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('templates.error.loadDetail')
  })

  const {
    showPolicyPanel,
    policyLoadFailed,
    apiPolicy,
    loadingPolicy,
    policySubmitting,
    policyLoadErrorKey,
    credentialColumnFilters,
    credentialsCurrentPage,
    paginatedCredentials,
    credentialStatusFilterOptions,
    totalCredentialRows,
    sortCredentialsByCreatedAt,
    loadPolicyData,
    handleCreateCredential,
    handleRotateCredential,
    handleRevokeCredential,
  } = useTemplatePolicyCredentials({
    templateId,
    template,
    errorMessage,
    revealSecret: (externalId, secret) => {
      workspaceRef.value?.revealCredentialSecret(externalId, secret)
    },
  })

  const showMetadataEdit = computed(() => {
    const status = template.value?.lifecycleStatus
    if (!status || !editTemplateMetadata.value) {
      return false
    }
    return status !== 'PUBLISHED' && status !== 'STOPPED' && status !== 'DEPRECATED'
  })
  const showDeleteTemplateAction = computed(
    () => deleteTemplates.value && template.value?.lifecycleStatus !== 'DELETED',
  )
  const showExportActions = computed(
    () =>
      exportTemplates.value &&
      Boolean(template.value) &&
      isTemplateExportEligible(template.value!.lifecycleStatus),
  )

  const hubWorkspaceTabs = computed((): WorkspaceTabOption[] => {
    const tabs: WorkspaceTabOption[] = [
      { name: 'overview', labelKey: templateDetailTabLabelKey('overview') },
    ]
    if (showPolicyPanel.value) {
      tabs.push({ name: 'apiAccess', labelKey: templateDetailTabLabelKey('apiAccess') })
    }
    return tabs
  })

  const activeHubTab = computed({
    get: () => secondaryTab.value ?? 'overview',
    set: (value: string) => {
      secondaryTab.value = value as HubSecondaryTab
    },
  })

  function resolveSecondaryTab(value: unknown): HubSecondaryTab | undefined {
    if (typeof value === 'string' && (HUB_SECONDARY_TABS as readonly string[]).includes(value)) {
      return value as HubSecondaryTab
    }
    return undefined
  }

  function syncSecondaryTabFromRoute() {
    if (route.query.tab === 'authoring') {
      void redirectAuthoringDeepLink()
      return
    }
    if (route.query.tab === 'lifecycle' || route.query.focus === 'lifecycle') {
      void redirectLifecycleDeepLink()
      return
    }
    if (route.query.tab === 'releaseVersions') {
      secondaryTab.value = undefined
      void router.replace(templatePackageHubPath(templateId.value))
      return
    }
    secondaryTab.value = resolveSecondaryTab(route.query.tab)
  }

  async function redirectLifecycleDeepLink() {
    try {
      if (!template.value) {
        await templatesStore.fetchTemplate(templateId.value)
      }
      openDevEditor('approval')
    } catch {
      await router.replace(templatePackageHubPath(templateId.value))
    }
  }

  async function redirectAuthoringDeepLink() {
    try {
      if (!template.value) {
        await templatesStore.fetchTemplate(templateId.value)
      }
      const devVersionId = templatesStore.selectedTemplate?.devVersionId
      if (devVersionId) {
        await router.replace(templateDevVersionPath(templateId.value, devVersionId))
      }
    } catch {
      await router.replace(templatePackageHubPath(templateId.value))
    }
  }

  onMounted(async () => {
    syncSecondaryTabFromRoute()
    if (
      route.query.tab === 'authoring' ||
      route.query.tab === 'lifecycle' ||
      route.query.focus === 'lifecycle'
    ) {
      return
    }
    await loadTemplate()
  })

  onUnmounted(() => {
    templatesStore.clearSelected()
  })

  watch(
    () => templateId.value,
    () => {
      void loadTemplate()
    },
  )

  watch(
    () => route.query,
    () => {
      syncSecondaryTabFromRoute()
    },
    { deep: true },
  )

  watch(secondaryTab, (tab) => {
    const queryTab = resolveSecondaryTab(route.query.tab)
    if (queryTab === tab) {
      return
    }
    if (!tab) {
      const query = { ...route.query }
      delete query.tab
      delete query.focus
      void router.replace({ query })
      return
    }
    void router.replace({ query: { ...route.query, tab } })
  })

  async function loadTemplate() {
    loadFailed.value = false
    try {
      await templatesStore.fetchTemplate(templateId.value)
      if (showPolicyPanel.value) {
        await loadPolicyData()
      }
    } catch {
      loadFailed.value = true
    }
  }

  function backToList() {
    router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
  }

  function openDevEditor(
    workspaceTab: TemplateDevWorkspaceTab = 'design',
    extraQuery?: Record<string, string>,
  ) {
    const devVersionId = template.value?.devVersionId
    if (!devVersionId) {
      return
    }
    router.push(
      templateDevVersionPath(templateId.value, devVersionId, undefined, {
        workspaceTab,
        ...extraQuery,
      }),
    )
  }

  async function handleMetadataUpdate(payload: { name: string; description: string | null }) {
    try {
      await templatesStore.updateTemplateMetadata(templateId.value, payload)
      metadataEditOpen.value = false
      ElMessage.success(t('templates.metadata.success'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.updateMetadata'))
    }
  }

  async function handleDeleteTemplate() {
    let reason = ''
    try {
      const result = await ElMessageBox.prompt(
        t('templates.deleteAction.reasonPrompt'),
        t('templates.deleteAction.title'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          inputValidator: (value) =>
            value.trim().length > 0 ? true : t('templates.deleteAction.reasonRequired'),
        },
      )
      reason = result.value.trim()
    } catch {
      return
    }

    const confirmed = await confirmAction({
      titleKey: 'templates.deleteAction.confirmTitle',
      messageKey: 'templates.deleteAction.confirmMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }

    try {
      const payload: DeleteTemplatePayload = { reason }
      await templatesStore.deleteTemplate(templateId.value, payload)
      ElMessage.success(t('templates.deleteAction.success'))
      router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.delete'))
    }
  }

  async function handleVersionLinesChanged() {
    await loadTemplate()
    await workspaceRef.value?.reloadVersionLines()
  }

  return {
    t,
    formatDateTime,
    templatesStore,
    authorTemplates,
    manageReleaseVersionState,
    metadataEditOpen,
    loadFailed,
    selectedContractEnvironment,
    workspaceRef,
    templateId,
    template,
    showDetailSkeleton,
    showPolicyPanel,
    policyLoadFailed,
    apiPolicy,
    loadingPolicy,
    policySubmitting,
    policyLoadErrorKey,
    credentialColumnFilters,
    credentialsCurrentPage,
    paginatedCredentials,
    credentialStatusFilterOptions,
    totalCredentialRows,
    sortCredentialsByCreatedAt,
    CLIENT_TABLE_PAGE_SIZE,
    showMetadataEdit,
    showDeleteTemplateAction,
    showExportActions,
    hubWorkspaceTabs,
    activeHubTab,
    loadTemplate,
    loadPolicyData,
    backToList,
    handleMetadataUpdate,
    handleDeleteTemplate,
    handleCreateCredential,
    handleRotateCredential,
    handleRevokeCredential,
    handleVersionLinesChanged,
  }
}
