import type { Ref } from 'vue'
import type {
  BindingPanelMode,
  EditSnapshot,
  StructuredBindingEditorExpose,
  TemplateAuthoringBindingsPanelProps,
} from '@/composables/templateAuthoringBindingsTypes'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import { mergeAnchorVisibilityRule } from '@/utils/mergeAnchorVisibilityRule'
import { buildBindingUpsertWithPasteEvidence } from '@/utils/pasteCleaningEvidence'
import { clearExactStructuredDraftOnSave } from '@/utils/structuredContentDraftStorage'
import { DEFAULT_STRUCTURED_CONTENT_JSON } from '@/utils/structuredContentNodes'
import type { CompositionRuleInput, PasteCleaningEvidence, UpsertBindingPayload } from '@/types/template'

export function createTemplateAuthoringBindingsSaveFlow(options: {
  props: TemplateAuthoringBindingsPanelProps
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
  sessionUsername: () => string | undefined
  upsertBinding: (
    templateId: string,
    anchorId: string,
    payload: UpsertBindingPayload,
  ) => Promise<unknown>
  saveRules: (
    templateId: string,
    rules: CompositionRuleInput[],
  ) => Promise<unknown>
}) {
  const {
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
    sessionUsername,
    upsertBinding,
    saveRules,
  } = options

  function clearStructuredLocalDraftOnSave() {
    clearExactStructuredDraftOnSave(
      localStorage,
      sessionUsername(),
      props.templateId,
      draftDevVersionId.value,
    )
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

    await upsertBinding(props.templateId, bindingForm.anchorId, payload)

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
      await saveRules(props.templateId, mergedRules)
    }

    structuredEditorRef.value?.markPristine()
    clearStructuredLocalDraftOnSave()
    captureEditSnapshot()
  }

  return {
    openEditPanel,
    saveBindingDraft,
  }
}
