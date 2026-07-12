import { computed, onMounted, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import * as contentModulesApi from '@/api/contentModules'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { canAccessContentModuleManagement } from '@/auth/roles'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import { useContentModulesStore } from '@/stores/contentModules'
import { useSessionStore } from '@/stores/session'
import { DEFAULT_STRUCTURED_CONTENT_JSON, serializeStructuredContent } from '@/utils/structuredContentNodes'
import { normalizeStructuredContentJson } from '@/utils/structuredContentCompat'
import type { ContentModuleSummary, ContentModuleVersion } from '@/types/contentModule'
import type { TemplateContentModuleReference } from '@/types/template'
import { ElMessage } from 'element-plus'

export type ClauseAuthoringPanelProps = {
  templateId: string
  groupCode: string
  editable: boolean
  refreshToken?: number
}

export type ClauseAuthoringPanelEmit = {
  (e: 'updated'): void
  (e: 'referencesLoaded', references: TemplateContentModuleReference[]): void
}

export function useClauseAuthoringPanel(
  props: ClauseAuthoringPanelProps,
  emit: ClauseAuthoringPanelEmit,
) {
  const { t, te } = useI18n()
  const sessionStore = useSessionStore()
  const contentModulesStore = useContentModulesStore()
  const panelDataStore = useTemplatePanelDataStore()

  const saving = ref(false)
  const savingClause = ref(false)
  const referenceDialogOpen = ref(false)
  const previewDialogOpen = ref(false)
  const clauseEditDialogOpen = ref(false)
  const entry = computed(() => panelDataStore.getEntry(props.templateId))
  const loading = computed(() => entry.value.loadingContentModuleReferences)
  const references = computed(() => entry.value.contentModuleReferences)
  const moduleOptions = ref<ContentModuleSummary[]>([])
  const versionOptions = ref<ContentModuleVersion[]>([])
  const editingReferenceKey = ref<string | null>(null)
  const previewContentJson = ref(DEFAULT_STRUCTURED_CONTENT_JSON)
  const clauseEditContentJson = ref(DEFAULT_STRUCTURED_CONTENT_JSON)
  const clauseEditReadonly = ref(false)
  const clauseEditVersion = ref<ContentModuleVersion | null>(null)
  const clauseEditModuleId = ref('')

  const form = reactive({
    referenceKey: '',
    moduleId: '',
    semanticVersion: '',
  })

  const referenceDialogTitle = computed(() =>
    editingReferenceKey.value
      ? t('templates.clauseAuthoring.editReferenceTitle', {
          referenceKey: editingReferenceKey.value,
        })
      : t('templates.clauseAuthoring.addReferenceTitle'),
  )

  function moduleOptionLabel(module: ContentModuleSummary): string {
    const base = `${module.moduleCode} — ${module.name}`
    if (module.groupCode === props.groupCode) {
      return base
    }
    return `${base} (${module.groupCode})`
  }

  function resolveModuleName(moduleId: string): string {
    const module = moduleOptions.value.find((item) => item.moduleId === moduleId)
    return module ? moduleOptionLabel(module) : moduleId
  }

  function approvedReferencableVersions(versions: ContentModuleVersion[]): ContentModuleVersion[] {
    return versions.filter(
      (version) =>
        version.reviewState === 'APPROVED' && (version.lifecycleState ?? 'ACTIVE') === 'ACTIVE',
    )
  }

  async function loadReferences() {
    try {
      await panelDataStore.fetchContentModuleReferences(props.templateId)
      emit('referencesLoaded', references.value)
    } catch (error) {
      const key = resolveApiErrorMessageKey(error, 'templates.clauseAuthoring.error.load')
      ElMessage.error(te(key) ? t(key) : t('templates.clauseAuthoring.error.load'))
    }
  }

  async function loadModuleOptions() {
    if (!props.groupCode) {
      moduleOptions.value = []
      return
    }
    if (!canAccessContentModuleManagement(sessionStore.session?.roles ?? [])) {
      moduleOptions.value = []
      return
    }
    try {
      const pageView = await contentModulesApi.listAllContentModules({
        groupCode: props.groupCode,
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
    referenceDialogOpen.value = true
    void loadModuleOptions()
  }

  function openEditReferenceDialog(reference: TemplateContentModuleReference) {
    if (reference.locked || !props.editable) {
      return
    }
    editingReferenceKey.value = reference.referenceKey
    form.referenceKey = reference.referenceKey
    form.moduleId = reference.moduleId
    form.semanticVersion = reference.semanticVersion
    referenceDialogOpen.value = true
    void loadModuleOptions()
    void loadVersionOptions(reference.moduleId)
  }

  async function handleModuleChange(moduleId: string) {
    form.moduleId = moduleId
    form.semanticVersion = ''
    await loadVersionOptions(moduleId)
  }

  async function handleSubmitReference() {
    const referenceKey = form.referenceKey.trim()
    const moduleId = form.moduleId.trim()
    const semanticVersion = form.semanticVersion.trim()
    if (!referenceKey || !moduleId || !semanticVersion) {
      ElMessage.warning(t('templates.clauseAuthoring.validation.required'))
      return
    }

    saving.value = true
    try {
      const upsertKey = editingReferenceKey.value ?? referenceKey
      await panelDataStore.upsertContentModuleReference(props.templateId, upsertKey, {
        referenceKey,
        moduleId,
        semanticVersion,
      })
      referenceDialogOpen.value = false
      ElMessage.success(t('templates.clauseAuthoring.saveReferenceSuccess'))
      emit('updated')
    } catch (error) {
      const key = resolveApiErrorMessageKey(error, 'templates.clauseAuthoring.error.saveReference')
      ElMessage.error(te(key) ? t(key) : t('templates.clauseAuthoring.error.saveReference'))
    } finally {
      saving.value = false
    }
  }

  async function resolveReferencedVersion(
    reference: TemplateContentModuleReference,
  ): Promise<ContentModuleVersion | null> {
    const detail = await contentModulesApi.getContentModule(reference.moduleId)
    return detail.versions.find((version) => version.semanticVersion === reference.semanticVersion) ?? null
  }

  async function openPreviewDialog(reference: TemplateContentModuleReference) {
    try {
      const version = await resolveReferencedVersion(reference)
      if (!version?.contentStructureJson) {
        ElMessage.warning(t('templates.clauseAuthoring.noContentStructure'))
        return
      }
      previewContentJson.value = serializeStructuredContent(
        normalizeStructuredContentJson(version.contentStructureJson),
      )
      previewDialogOpen.value = true
    } catch {
      ElMessage.error(t('templates.clauseAuthoring.error.loadContent'))
    }
  }

  async function openClauseEditor(reference: TemplateContentModuleReference) {
    if (!props.editable) {
      return
    }
    try {
      const version = await resolveReferencedVersion(reference)
      if (!version) {
        ElMessage.warning(t('templates.clauseAuthoring.versionNotFound'))
        return
      }
      clauseEditVersion.value = version
      clauseEditModuleId.value = reference.moduleId
      clauseEditContentJson.value = version.contentStructureJson
        ? serializeStructuredContent(normalizeStructuredContentJson(version.contentStructureJson))
        : DEFAULT_STRUCTURED_CONTENT_JSON
      clauseEditReadonly.value = version.reviewState !== 'DRAFT'
      clauseEditDialogOpen.value = true
    } catch {
      ElMessage.error(t('templates.clauseAuthoring.error.loadContent'))
    }
  }

  async function handleSaveClauseContent() {
    const version = clauseEditVersion.value
    if (!version || clauseEditReadonly.value) {
      return
    }
    savingClause.value = true
    try {
      await contentModulesStore.updateDraftVersion(clauseEditModuleId.value, version.semanticVersion, {
        contentStructureJson: clauseEditContentJson.value,
      })
      ElMessage.success(t('templates.clauseAuthoring.saveClauseSuccess'))
      clauseEditDialogOpen.value = false
      emit('updated')
    } catch {
      ElMessage.error(t('templates.clauseAuthoring.error.saveClause'))
    } finally {
      savingClause.value = false
    }
  }

  onMounted(async () => {
    await loadModuleOptions()
    await loadReferences()
  })

  watch(
    () => props.refreshToken,
    () => {
      void loadReferences()
    },
  )

  return {
    saving,
    savingClause,
    referenceDialogOpen,
    previewDialogOpen,
    clauseEditDialogOpen,
    loading,
    references,
    moduleOptions,
    versionOptions,
    editingReferenceKey,
    previewContentJson,
    clauseEditContentJson,
    clauseEditReadonly,
    form,
    referenceDialogTitle,
    moduleOptionLabel,
    resolveModuleName,
    openCreateDialog,
    openEditReferenceDialog,
    handleModuleChange,
    handleSubmitReference,
    openPreviewDialog,
    openClauseEditor,
    handleSaveClauseContent,
  }
}

export type ClauseAuthoringPanelApi = ReturnType<typeof useClauseAuthoringPanel>

export type ClauseAuthoringDialogState = {
  referenceDialogOpen: Ref<boolean>
  previewDialogOpen: Ref<boolean>
  clauseEditDialogOpen: Ref<boolean>
  saving: Ref<boolean>
  savingClause: Ref<boolean>
  editingReferenceKey: Ref<string | null>
  previewContentJson: Ref<string>
  clauseEditContentJson: Ref<string>
  clauseEditReadonly: Ref<boolean>
  form: {
    referenceKey: string
    moduleId: string
    semanticVersion: string
  }
  moduleOptions: Ref<ContentModuleSummary[]>
  versionOptions: Ref<ContentModuleVersion[]>
}
