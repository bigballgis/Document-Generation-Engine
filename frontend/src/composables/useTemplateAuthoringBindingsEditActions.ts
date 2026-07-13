import { computed, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useDirtyGuard } from '@/composables/useDirtyGuard'
import { useTemplateAuthoringBindingsPasteResidue } from '@/composables/useTemplateAuthoringBindingsPasteResidue'
import { createTemplateAuthoringBindingsSaveFlow } from '@/composables/createTemplateAuthoringBindingsSaveFlow'
import {
  type BindingPanelMode,
  type EditSnapshot,
  type StructuredBindingEditorExpose,
  type TemplateAuthoringBindingsPanelProps,
} from '@/composables/templateAuthoringBindingsTypes'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import type { PasteCleaningEvidence, UpsertBindingPayload } from '@/types/template'

export function useTemplateAuthoringBindingsEditActions(options: {
  props: TemplateAuthoringBindingsPanelProps
  emit: {
    (e: 'updated'): void
    (e: 'preview-refreshed', preview: import('@/types/template').PreviewRecord): void
  }
  structuredEditorRef: Ref<StructuredBindingEditorExpose | null>
  panelMode: Ref<BindingPanelMode>
  editingAnchorId: Ref<string | null>
  visibilityEnabled: Ref<boolean>
  visibilityExpression: Ref<string>
  editorDirty: Ref<boolean>
  structureRevision: Ref<number>
  previewSyncedRevision: Ref<number>
  editSnapshot: Ref<EditSnapshot | null>
  suppressStructureBump: Ref<boolean>
  bindingForm: UpsertBindingPayload
  pendingPasteEvidence: Ref<PasteCleaningEvidence | null>
  pendingClearPasteEvidence: Ref<boolean>
  draftDevVersionId: Ref<string> | { value: string }
  formDirty: { value: boolean }
  editingRow: { value: MasterAnchorBindingRow | null }
  previewRefreshing: { value: boolean }
  localPreviewRefreshing: Ref<boolean>
}) {
  const {
    props,
    emit,
    structuredEditorRef,
    panelMode,
    editingAnchorId,
    visibilityEnabled,
    visibilityExpression,
    editorDirty,
    structureRevision,
    previewSyncedRevision,
    editSnapshot,
    suppressStructureBump,
    bindingForm,
    pendingPasteEvidence,
    pendingClearPasteEvidence,
    draftDevVersionId,
    formDirty,
    editingRow,
    previewRefreshing,
    localPreviewRefreshing,
  } = options

  const { t, te } = useI18n()
  const templatesStore = useTemplatesStore()
  const sessionStore = useSessionStore()

  const { openEditPanel, saveBindingDraft } = createTemplateAuthoringBindingsSaveFlow({
    props,
    structuredEditorRef,
    panelMode,
    editingAnchorId,
    visibilityEnabled,
    visibilityExpression,
    editorDirty,
    structureRevision,
    previewSyncedRevision,
    editSnapshot,
    suppressStructureBump,
    bindingForm,
    pendingPasteEvidence,
    pendingClearPasteEvidence,
    draftDevVersionId,
    sessionUsername: () => sessionStore.session?.username,
    upsertBinding: (templateId, anchorId, payload) =>
      templatesStore.upsertBinding(templateId, anchorId, payload),
    saveRules: (templateId, rules) => templatesStore.saveRules(templateId, rules),
  })

  const {
    dialogVisible: dirtyGuardDialogVisible,
    saving: dirtyGuardSaving,
    showSaveAction: dirtyGuardShowSave,
    handleStay: dirtyGuardStay,
    handleDiscard: dirtyGuardDiscard,
    handleSave: dirtyGuardSave,
    requestLeave: dirtyGuardRequestLeave,
  } = useDirtyGuard({
    isDirty: computed(() => formDirty.value),
    enabled: computed(() => panelMode.value === 'edit'),
    onSave: async () => {
      try {
        await saveBindingDraft()
        return true
      } catch {
        return false
      }
    },
  })

  const {
    bindingHasPasteBlockers,
    editingPasteResidueBlocked,
    pasteResidueItemLabel,
    handlePasteAccepted,
    clearPendingPasteResidue,
  } = useTemplateAuthoringBindingsPasteResidue({
    pendingPasteEvidence,
    pendingClearPasteEvidence,
    editingRow,
    te,
    t,
  })

  function backToList() {
    void dirtyGuardRequestLeave(() => {
      panelMode.value = 'list'
      editingAnchorId.value = null
      editSnapshot.value = null
    })
  }

  async function handleSaveBinding() {
    try {
      await saveBindingDraft()
      panelMode.value = 'list'
      editingAnchorId.value = null
      editSnapshot.value = null
      ElMessage.success(t('templates.authoring.saveBindingSuccess'))
      emit('updated')
    } catch {
      ElMessage.error(t('templates.error.saveBinding'))
    }
  }

  async function handlePreviewRefresh() {
    if (previewRefreshing.value) {
      return
    }
    localPreviewRefreshing.value = true
    try {
      const preview = await templatesStore.testGenerate(props.templateId, {
        testDataSetId: props.selectedTestDataSetId ?? undefined,
      })
      previewSyncedRevision.value = structureRevision.value
      emit('preview-refreshed', preview)
      ElMessage.success(t('templates.testGenerate.success', { previewId: preview.previewId }))
    } catch {
      ElMessage.error(t('templates.error.testGenerate'))
    } finally {
      localPreviewRefreshing.value = false
    }
  }

  function handleEditorDirtyChange(dirty: boolean) {
    editorDirty.value = dirty
  }

  function handleStructureChange() {
    if (suppressStructureBump.value) {
      return
    }
    structureRevision.value += 1
  }

  watch(
    () => [bindingForm.declaredContentType, visibilityEnabled.value, visibilityExpression.value],
    () => {
      if (panelMode.value === 'edit' && !suppressStructureBump.value) {
        structureRevision.value += 1
      }
    },
  )

  return {
    dirtyGuardDialogVisible,
    dirtyGuardSaving,
    dirtyGuardShowSave,
    dirtyGuardStay,
    dirtyGuardDiscard,
    dirtyGuardSave,
    editingPasteResidueBlocked,
    bindingHasPasteBlockers,
    pasteResidueItemLabel,
    handlePasteAccepted,
    clearPendingPasteResidue,
    openEditPanel,
    backToList,
    handleSaveBinding,
    handlePreviewRefresh,
    handleEditorDirtyChange,
    handleStructureChange,
  }
}
