import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useTemplatesStore } from '@/stores/templates'
import { useTemplateLifecycleActions } from '@/views/templates/useTemplateLifecycleActions'
import { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'
import {
  resolveTemplateDetailTabFromQuery,
  type TemplateDetailTab,
} from '@/views/templates/templateDetailTabs'
import {
  resolveTemplateDevWorkspaceTabFromQuery,
  type TemplateDevWorkspaceTab,
} from '@/views/templates/templateDevWorkspaceTabs'
import { useTemplateDetailNavigation } from '@/views/templates/useTemplateDetailNavigation'
import { useTemplatePreviewActions } from '@/views/templates/useTemplatePreviewActions'
import { useTemplateDetailVisibility } from '@/views/templates/useTemplateDetailVisibility'
import { assembleTemplateDetailControllerApi } from '@/views/templates/assembleTemplateDetailControllerApi'

export function useTemplateDetailController(workspace: Ref<'legacy' | 'dev-editor'>) {
  const isDevEditor = computed(() => workspace.value === 'dev-editor')
  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const route = useRoute()
  const templatesStore = useTemplatesStore()
  const { authorTemplates, decideTests, decideApprovals, publishTemplates, reviewMasters } =
    useCapabilities()

  const metadataEditOpen = ref(false)
  const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)
  const templateId = computed(() => route.params.templateId as string)
  const devVersionId = computed(() =>
    isDevEditor.value ? String(route.params.devVersionId ?? '') : '',
  )
  const loadTemplateHolder = { fn: async (): Promise<void> => {} }

  const template = computed(() => {
    if (templatesStore.selectedTemplate?.id !== templateId.value) {
      return null
    }
    return templatesStore.selectedTemplate
  })

  const showDetailSkeleton = computed(() => {
    const selected = templatesStore.selectedTemplate
    // Keep the mounted workspace during soft refresh of the same template.
    if (selected?.id === templateId.value) {
      return false
    }
    if (templatesStore.loadingDetail) {
      return true
    }
    return selected !== null && selected.id !== templateId.value
  })

  const errorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    return !key ? '' : te(key) ? t(key) : t('templates.error.loadDetail')
  })

  const activeDetailTab = ref<TemplateDetailTab>(
    isDevEditor.value ? 'authoring' : resolveTemplateDetailTabFromQuery(route.query),
  )
  const activeDevWorkspaceTab = ref<TemplateDevWorkspaceTab>(
    resolveTemplateDevWorkspaceTabFromQuery(route.query),
  )

  const lifecycle = useTemplateLifecycleActions({
    templateId,
    template,
    isDevEditor,
    errorMessage,
    loadTemplate: () => loadTemplateHolder.fn(),
    activeDetailTab,
  })

  const policy = useTemplatePolicyCredentials({ templateId, template, errorMessage })

  const visibility = useTemplateDetailVisibility({ isDevEditor, template })

  const previewActionsRef: { openDevWorkspaceTab: (tab: TemplateDevWorkspaceTab) => void } = {
    openDevWorkspaceTab: () => {},
  }

  const preview = useTemplatePreviewActions({
    templateId,
    errorMessage,
    openDevWorkspaceTab: (tab) => previewActionsRef.openDevWorkspaceTab(tab),
  })

  const navigation = useTemplateDetailNavigation({
    isDevEditor,
    templateId,
    devVersionId,
    template,
    lastPreview: preview.lastPreview,
    showAuthoringSection: visibility.showAuthoringSection,
    activeDetailTab,
    activeDevWorkspaceTab,
    loadTemplateHolder,
    lifecycle,
    policy,
    handleTestGenerate: preview.handleTestGenerate,
  })

  previewActionsRef.openDevWorkspaceTab = navigation.openDevWorkspaceTab

  async function handleMetadataUpdate(payload: { name: string; description: string | null }) {
    try {
      await templatesStore.updateTemplateMetadata(templateId.value, payload)
      metadataEditOpen.value = false
      ElMessage.success(t('templates.metadata.success'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.updateMetadata'))
    }
  }

  return assembleTemplateDetailControllerApi({
    t,
    te,
    formatDateTime,
    templateId,
    devVersionId,
    isDevEditor,
    template,
    showDetailSkeleton,
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    metadataEditOpen,
    selectedContractEnvironment,
    templatesStore,
    lifecycle,
    policy,
    visibility,
    preview,
    navigation,
    handleMetadataUpdate,
  })
}
