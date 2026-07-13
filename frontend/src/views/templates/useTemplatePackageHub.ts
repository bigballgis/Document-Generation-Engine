import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { WorkspaceTabOption } from '@/components/common/WorkspaceTabShell.vue'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useTemplatesStore } from '@/stores/templates'
import { isTemplateExportEligible } from '@/utils/templateExportEligibility'
import { templateDetailTabLabelKey } from '@/views/templates/templateDetailTabs'
import { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'
import { createTemplatePackageHubActions } from '@/views/templates/createTemplatePackageHubActions'
import {
  useTemplatePackageHubRouting,
  type HubSecondaryTab,
} from '@/views/templates/useTemplatePackageHubRouting'

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

  const { loadTemplate } = useTemplatePackageHubRouting({
    templateId,
    template,
    secondaryTab,
    showPolicyPanel,
    loadPolicyData,
    loadFailed,
  })

  const hubActions = createTemplatePackageHubActions({
    t,
    router,
    templatesStore,
    templateId,
    errorMessage,
    metadataEditOpen,
    confirmAction,
    loadTemplate,
    workspaceRef,
  })

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
    ...hubActions,
    handleCreateCredential,
    handleRotateCredential,
    handleRevokeCredential,
  }
}
