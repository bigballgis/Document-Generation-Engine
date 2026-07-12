import { computed, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useDirtyGuard } from '@/composables/useDirtyGuard'
import {
  type BindingPanelMode,
  type EditSnapshot,
  type StructuredBindingEditorExpose,
  type TemplateAuthoringBindingsPanelProps,
} from '@/composables/templateAuthoringBindingsTypes'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import { mergeAnchorVisibilityRule } from '@/utils/mergeAnchorVisibilityRule'
import {
  buildBindingUpsertWithPasteEvidence,
  hasUnresolvedPasteBlockers,
} from '@/utils/pasteCleaningEvidence'
import { clearExactStructuredDraftOnSave } from '@/utils/structuredContentDraftStorage'
import { DEFAULT_STRUCTURED_CONTENT_JSON } from '@/utils/structuredContentNodes'
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

  function clearStructuredLocalDraftOnSave() {
    clearExactStructuredDraftOnSave(
      localStorage,
      sessionStore.session?.username,
      props.templateId,
      draftDevVersionId.value,
    )
  }

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

  function bindingHasPasteBlockers(row: MasterAnchorBindingRow): boolean {
    return hasUnresolvedPasteBlockers(row.binding?.pasteCleaningEvidence)
  }

  const editingPasteResidueBlocked = computed(() => {
    if (pendingPasteEvidence.value && !hasUnresolvedPasteBlockers(pendingPasteEvidence.value)) {
      return false
    }
    if (pendingClearPasteEvidence.value) {
      return false
    }
    return hasUnresolvedPasteBlockers(editingRow.value?.binding?.pasteCleaningEvidence)
  })

  function pasteResidueItemLabel(messageKey: string): string {
    return te(messageKey) ? t(messageKey) : messageKey
  }

  function handlePasteAccepted(evidence: PasteCleaningEvidence) {
    pendingPasteEvidence.value = evidence
    pendingClearPasteEvidence.value = false
  }

  function clearPendingPasteResidue() {
    pendingPasteEvidence.value = null
    pendingClearPasteEvidence.value = true
  }

  function loadVisibilityRuleForAnchor(anchorId: string) {
    const rule = (props.rules ?? []).find((item) => item.targetAnchorId === anchorId)
    if (rule) {
      visibilityEnabled.value = true
      visibilityExpression.value = rule.conditionExpression
    } else {
      visibilityEnabled.value = false
      visibilityExpression.value = ''
    }
  }

  function captureEditSnapshot() {
    editSnapshot.value = {
      declaredContentType: bindingForm.declaredContentType,
      structuredContentJson: bindingForm.structuredContentJson,
      visibilityEnabled: visibilityEnabled.value,
      visibilityExpression: visibilityExpression.value,
    }
    editorDirty.value = false
  }

  function openEditPanel(row: MasterAnchorBindingRow) {
    suppressStructureBump.value = true
    editingAnchorId.value = row.anchorId
    bindingForm.anchorId = row.anchorId
    pendingPasteEvidence.value = null
    pendingClearPasteEvidence.value = false
    if (row.binding) {
      bindingForm.declaredContentType = row.binding.declaredContentType
      bindingForm.structuredContentJson =
        row.binding.structuredContentJson ?? DEFAULT_STRUCTURED_CONTENT_JSON
    } else {
      bindingForm.declaredContentType = 'TEXT'
      bindingForm.structuredContentJson = DEFAULT_STRUCTURED_CONTENT_JSON
    }
    loadVisibilityRuleForAnchor(row.anchorId)
    panelMode.value = 'edit'
    captureEditSnapshot()
    previewSyncedRevision.value = structureRevision.value
    suppressStructureBump.value = false
  }

  function backToList() {
    void dirtyGuardRequestLeave(() => {
      panelMode.value = 'list'
      editingAnchorId.value = null
      editSnapshot.value = null
    })
  }

  async function saveBindingDraft() {
    const payload = buildBindingUpsertWithPasteEvidence(
      {
        anchorId: bindingForm.anchorId,
        declaredContentType: bindingForm.declaredContentType,
        structuredContentJson: bindingForm.structuredContentJson,
      },
      {
        pendingPasteEvidence: pendingPasteEvidence.value,
        clearPasteCleaningEvidence: pendingClearPasteEvidence.value,
      },
    )

    await templatesStore.upsertBinding(props.templateId, bindingForm.anchorId, payload)

    pendingPasteEvidence.value = null
    pendingClearPasteEvidence.value = false

    const previousRules = props.rules ?? []
    const mergedRules = mergeAnchorVisibilityRule(
      previousRules,
      bindingForm.anchorId,
      visibilityEnabled.value,
      visibilityExpression.value,
    )

    if (previousRules.length > 0 || mergedRules.length > 0) {
      await templatesStore.saveRules(props.templateId, mergedRules)
    }

    structuredEditorRef.value?.markPristine()
    clearStructuredLocalDraftOnSave()
    captureEditSnapshot()
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
