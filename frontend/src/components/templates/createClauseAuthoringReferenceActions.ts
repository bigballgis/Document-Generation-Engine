import { ref, type Ref } from 'vue'
import * as contentModulesApi from '@/api/contentModules'
import { canAccessContentModuleManagement } from '@/auth/roles'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { useSessionStore } from '@/stores/session'
import type { ContentModuleSummary, ContentModuleVersion } from '@/types/contentModule'
import type { TemplateContentModuleReference } from '@/types/template'
import { ElMessage } from 'element-plus'
import { suggestReferenceKey } from '@/utils/referenceKeyFromModuleCode'
import type {
  ClauseAuthoringPanelEmit,
  ClauseAuthoringPanelProps,
} from '@/components/templates/useClauseAuthoringPanel'

type Translate = (key: string, params?: Record<string, unknown>) => string
type HasKey = (key: string) => boolean

export function createClauseAuthoringReferenceActions(options: {
  t: Translate
  te: HasKey
  props: ClauseAuthoringPanelProps
  emit: ClauseAuthoringPanelEmit
  panelDataStore: ReturnType<typeof useTemplatePanelDataStore>
  sessionStore: ReturnType<typeof useSessionStore>
  form: { referenceKey: string; moduleId: string; semanticVersion: string }
  moduleOptions: Ref<ContentModuleSummary[]>
  versionOptions: Ref<ContentModuleVersion[]>
  editingReferenceKey: Ref<string | null>
  referenceDialogOpen: Ref<boolean>
  saving: Ref<boolean>
  references: Ref<TemplateContentModuleReference[]>
}) {
  const {
    t,
    te,
    props,
    emit,
    panelDataStore,
    sessionStore,
    form,
    moduleOptions,
    versionOptions,
    editingReferenceKey,
    referenceDialogOpen,
    saving,
    references,
  } = options

  /** Create-dialog session flag: Advanced custom key must not be clobbered (BEI-C10). */
  const referenceKeyUserOverridden = ref(false)

  function existingReferenceKeys(): string[] {
    return references.value.map((item) => item.referenceKey)
  }

  function applyAutoReferenceKey(moduleId: string) {
    if (editingReferenceKey.value || referenceKeyUserOverridden.value) {
      return
    }
    const module = moduleOptions.value.find((item) => item.moduleId === moduleId)
    form.referenceKey = suggestReferenceKey(module?.moduleCode ?? '', existingReferenceKeys())
  }

  function markReferenceKeyOverridden() {
    if (editingReferenceKey.value) {
      return
    }
    referenceKeyUserOverridden.value = true
  }

  function clearReferenceKeyOverride() {
    if (editingReferenceKey.value) {
      return
    }
    referenceKeyUserOverridden.value = false
    applyAutoReferenceKey(form.moduleId)
  }

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
    referenceKeyUserOverridden.value = false
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
    referenceKeyUserOverridden.value = false
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
    applyAutoReferenceKey(moduleId)
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

  return {
    moduleOptionLabel,
    resolveModuleName,
    loadReferences,
    loadModuleOptions,
    openCreateDialog,
    openEditReferenceDialog,
    handleModuleChange,
    handleSubmitReference,
    referenceKeyUserOverridden,
    markReferenceKeyOverridden,
    clearReferenceKeyOverride,
  }
}
