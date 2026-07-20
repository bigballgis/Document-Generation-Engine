import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { apiPackageSettingsPath } from '@/routing/apiPackageSettings'
import { useTemplatesStore } from '@/stores/templates'
import { isTemplateExportEligible } from '@/utils/templateExportEligibility'
import { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'
import { createTemplatePackageHubActions } from '@/views/templates/createTemplatePackageHubActions'
import { useTemplatePackageHubRouting } from '@/views/templates/useTemplatePackageHubRouting'

type HubWorkspaceExpose = {
  reloadVersionLines: () => Promise<void> | undefined
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
    manageApiPolicy,
  } = useCapabilities()
  const { confirmAction } = useConfirmAction()

  const metadataEditOpen = ref(false)
  const propertiesOpen = ref(false)
  const dependenciesGuidanceVisible = ref(false)
  const loadFailed = ref(false)
  const workspaceRef = ref<HubWorkspaceExpose | null>(null)

  const templateId = computed(() => String(route.params.templateId ?? ''))

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
    loadPolicyData,
  } = useTemplatePolicyCredentials({
    templateId,
    template,
    errorMessage,
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
  const showApiSettingsAction = computed(() => manageApiPolicy.value)

  const { loadTemplate } = useTemplatePackageHubRouting({
    templateId,
    template,
    showPolicyPanel,
    loadPolicyData,
    loadFailed,
    propertiesOpen,
    dependenciesGuidanceVisible,
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

  function openProperties() {
    propertiesOpen.value = true
  }

  function openApiSettings() {
    void router.push(apiPackageSettingsPath(templateId.value))
  }

  return {
    t,
    formatDateTime,
    templatesStore,
    authorTemplates,
    manageReleaseVersionState,
    metadataEditOpen,
    propertiesOpen,
    dependenciesGuidanceVisible,
    loadFailed,
    workspaceRef,
    templateId,
    template,
    showDetailSkeleton,
    showPolicyPanel,
    showMetadataEdit,
    showDeleteTemplateAction,
    showExportActions,
    showApiSettingsAction,
    loadTemplate,
    openProperties,
    openApiSettings,
    ...hubActions,
  }
}
