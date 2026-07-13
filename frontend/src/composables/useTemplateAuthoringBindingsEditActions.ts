import { computed, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
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
import { createTemplateAuthoringBindingsEditHandlers } from '@/composables/createTemplateAuthoringBindingsEditHandlers'

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

  const paste = useTemplateAuthoringBindingsPasteResidue({
    pendingPasteEvidence,
    pendingClearPasteEvidence,
    editingRow,
    te,
    t,
  })

  const handlers = createTemplateAuthoringBindingsEditHandlers({
    t,
    props,
    emit,
    panelMode,
    editingAnchorId,
    editSnapshot,
    editorDirty,
    structureRevision,
    previewSyncedRevision,
    suppressStructureBump,
    bindingForm,
    visibilityEnabled,
    visibilityExpression,
    previewRefreshing,
    localPreviewRefreshing,
    templatesStore,
    saveBindingDraft,
    dirtyGuardRequestLeave,
  })

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
    editingPasteResidueBlocked: paste.editingPasteResidueBlocked,
    bindingHasPasteBlockers: paste.bindingHasPasteBlockers,
    pasteResidueItemLabel: paste.pasteResidueItemLabel,
    handlePasteAccepted: paste.handlePasteAccepted,
    clearPendingPasteResidue: paste.clearPendingPasteResidue,
    openEditPanel,
    ...handlers,
  }
}
