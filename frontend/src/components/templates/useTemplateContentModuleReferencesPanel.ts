import { computed, onMounted, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import * as contentModulesApi from '@/api/contentModules'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import type { ContentModuleSummary, ContentModuleVersion } from '@/types/contentModule'
import type { TemplateContentModuleReference } from '@/types/template'
import { ElMessage } from 'element-plus'

export interface UseTemplateContentModuleReferencesPanelOptions {
  templateId: Ref<string>
  groupCode: Ref<string>
  editable: Ref<boolean>
  refreshToken: Ref<number | undefined>
  emitUpdated: () => void
}

export function useTemplateContentModuleReferencesPanel(
  options: UseTemplateContentModuleReferencesPanelOptions,
) {
  const { t, te } = useI18n()
  const { contentModuleDetailLink } = useEntityLinkTargets()
  const panelDataStore = useTemplatePanelDataStore()

  const saving = ref(false)
  const dialogOpen = ref(false)
  const entry = computed(() => panelDataStore.getEntry(options.templateId.value))
  const loading = computed(() => entry.value.loadingContentModuleReferences)
  const references = computed(() => entry.value.contentModuleReferences)
  const moduleOptions = ref<ContentModuleSummary[]>([])
  const versionOptions = ref<ContentModuleVersion[]>([])
  const editingReferenceKey = ref<string | null>(null)

  const form = reactive({
    referenceKey: '',
    moduleId: '',
    semanticVersion: '',
  })

  const dialogTitle = computed(() =>
    editingReferenceKey.value
      ? t('templates.contentModuleReferences.editTitle', {
          referenceKey: editingReferenceKey.value,
        })
      : t('templates.contentModuleReferences.addTitle'),
  )

  function moduleOptionLabel(module: ContentModuleSummary): string {
    const base = `${module.moduleCode} — ${module.name}`
    if (module.groupCode === options.groupCode.value) {
      return base
    }
    return `${base} (${module.groupCode})`
  }

  function resolveModuleName(moduleId: string): string {
    const module = moduleOptions.value.find((item) => item.moduleId === moduleId)
    return module?.name ?? moduleId
  }

  function resolveModuleSubtitle(moduleId: string): string | undefined {
    const module = moduleOptions.value.find((item) => item.moduleId === moduleId)
    return module?.moduleCode
  }

  function approvedReferencableVersions(versions: ContentModuleVersion[]): ContentModuleVersion[] {
    return versions.filter(
      (version) =>
        version.reviewState === 'APPROVED' && (version.lifecycleState ?? 'ACTIVE') === 'ACTIVE',
    )
  }

  async function loadReferences() {
    try {
      await panelDataStore.fetchContentModuleReferences(options.templateId.value)
    } catch (error) {
      const key = resolveApiErrorMessageKey(error, 'templates.contentModuleReferences.error.load')
      ElMessage.error(te(key) ? t(key) : t('templates.contentModuleReferences.error.load'))
    }
  }

  async function loadModuleOptions() {
    if (!options.groupCode.value) {
      moduleOptions.value = []
      return
    }
    try {
      const pageView = await contentModulesApi.listAllContentModules({
        groupCode: options.groupCode.value,
        sort: 'groupCodeAsc',
      })
      moduleOptions.value = pageView.content
    } catch {
      moduleOptions.value = []
    }
  }

  async function loadVersionOptions(moduleId: string) {
    if (!moduleId) {
      versionOptions.value = []
      return
    }
    try {
      const detail = await contentModulesApi.getContentModule(moduleId)
      versionOptions.value = approvedReferencableVersions(detail.versions)
    } catch {
      versionOptions.value = []
    }
  }

  function resetForm() {
    form.referenceKey = ''
    form.moduleId = ''
    form.semanticVersion = ''
    versionOptions.value = []
  }

  function openCreateDialog() {
    editingReferenceKey.value = null
    resetForm()
    dialogOpen.value = true
    void loadModuleOptions()
  }

  function openEditDialog(reference: TemplateContentModuleReference) {
    if (reference.locked || !options.editable.value) {
      return
    }
    editingReferenceKey.value = reference.referenceKey
    form.referenceKey = reference.referenceKey
    form.moduleId = reference.moduleId
    form.semanticVersion = reference.semanticVersion
    dialogOpen.value = true
    void loadModuleOptions()
    void loadVersionOptions(reference.moduleId)
  }

  async function handleModuleChange(moduleId: string) {
    form.moduleId = moduleId
    form.semanticVersion = ''
    await loadVersionOptions(moduleId)
  }

  async function handleSubmit() {
    const referenceKey = form.referenceKey.trim()
    const moduleId = form.moduleId.trim()
    const semanticVersion = form.semanticVersion.trim()
    if (!referenceKey || !moduleId || !semanticVersion) {
      ElMessage.warning(t('templates.contentModuleReferences.validation.required'))
      return
    }

    saving.value = true
    try {
      const upsertKey = editingReferenceKey.value ?? referenceKey
      await panelDataStore.upsertContentModuleReference(options.templateId.value, upsertKey, {
        referenceKey,
        moduleId,
        semanticVersion,
      })
      dialogOpen.value = false
      ElMessage.success(t('templates.contentModuleReferences.saveSuccess'))
      options.emitUpdated()
    } catch (error) {
      const key = resolveApiErrorMessageKey(error, 'templates.contentModuleReferences.error.save')
      ElMessage.error(te(key) ? t(key) : t('templates.contentModuleReferences.error.save'))
    } finally {
      saving.value = false
    }
  }

  onMounted(() => {
    void Promise.all([loadReferences(), loadModuleOptions()])
  })

  watch(
    () => options.refreshToken.value,
    () => {
      void loadReferences()
    },
  )

  return {
    t,
    contentModuleDetailLink,
    saving,
    dialogOpen,
    loading,
    references,
    moduleOptions,
    versionOptions,
    editingReferenceKey,
    form,
    dialogTitle,
    moduleOptionLabel,
    resolveModuleName,
    resolveModuleSubtitle,
    openCreateDialog,
    openEditDialog,
    handleModuleChange,
    handleSubmit,
  }
}
