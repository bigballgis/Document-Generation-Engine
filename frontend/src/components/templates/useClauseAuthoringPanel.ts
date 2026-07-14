import { computed, onMounted, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { useSessionStore } from '@/stores/session'
import { DEFAULT_STRUCTURED_CONTENT_JSON } from '@/utils/structuredContentNodes'
import { useClauseAuthoringEditors } from '@/components/templates/useClauseAuthoringEditors'
import type { ContentModuleSummary, ContentModuleVersion } from '@/types/contentModule'
import type { TemplateContentModuleReference } from '@/types/template'
import { createClauseAuthoringReferenceActions } from '@/components/templates/createClauseAuthoringReferenceActions'
import { useClauseOutdatedBumpActions } from '@/components/templates/useClauseOutdatedBumpActions'

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
  const panelDataStore = useTemplatePanelDataStore()

  const saving = ref(false)
  const savingClause = ref(false)
  const bumping = ref(false)
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

  const referenceActions = createClauseAuthoringReferenceActions({
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
  })

  const { openPreviewDialog, openClauseEditor, handleSaveClauseContent } = useClauseAuthoringEditors({
    editable: () => props.editable,
    previewDialogOpen,
    previewContentJson,
    clauseEditDialogOpen,
    clauseEditContentJson,
    clauseEditReadonly,
    clauseEditVersion,
    clauseEditModuleId,
    savingClause,
    emitUpdated: () => emit('updated'),
  })

  const bumpActions = useClauseOutdatedBumpActions({
    t,
    te,
    templateId: props.templateId,
    editable: () => props.editable,
    references,
    bumping,
    emitUpdated: () => emit('updated'),
  })

  onMounted(async () => {
    await referenceActions.loadModuleOptions()
    await referenceActions.loadReferences()
  })

  watch(
    () => props.refreshToken,
    () => {
      void referenceActions.loadReferences()
    },
  )

  return {
    saving,
    savingClause,
    bumping,
    hasOutdatedUnlockedReferences: bumpActions.hasOutdatedUnlockedReferences,
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
    moduleOptionLabel: referenceActions.moduleOptionLabel,
    resolveModuleName: referenceActions.resolveModuleName,
    openCreateDialog: referenceActions.openCreateDialog,
    openEditReferenceDialog: referenceActions.openEditReferenceDialog,
    handleModuleChange: referenceActions.handleModuleChange,
    handleSubmitReference: referenceActions.handleSubmitReference,
    bumpReference: bumpActions.bumpReference,
    bumpAllOutdatedReferences: bumpActions.bumpAllOutdatedReferences,
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
