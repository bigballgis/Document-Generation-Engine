import { nextTick, type Ref } from 'vue'
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
import type {
  AnchorBinding,
  CompositionRuleInput,
  PasteCleaningEvidence,
  UpsertBindingPayload,
} from '@/types/template'

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
  /** CE-U21 — concurrency token for existing bindings; null on first create. */
  expectedUpdatedAt: Ref<string | null>
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
    expectedUpdatedAt,
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
      bindingForm.anchorId || editingAnchorId.value,
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

  function applyBindingToForm(binding: AnchorBinding | undefined) {
    if (binding) {
      bindingForm.declaredContentType = binding.declaredContentType
      bindingForm.structuredContentJson =
        binding.structuredContentJson ?? DEFAULT_STRUCTURED_CONTENT_JSON
      expectedUpdatedAt.value = binding.updatedAt
    } else {
      bindingForm.declaredContentType = 'TEXT'
      bindingForm.structuredContentJson = DEFAULT_STRUCTURED_CONTENT_JSON
      expectedUpdatedAt.value = null
    }
  }

  function openEditPanel(row: MasterAnchorBindingRow) {
    suppressStructureBump.value = true
    editingAnchorId.value = row.anchorId
    bindingForm.anchorId = row.anchorId
    pendingPasteEvidence.value = null
    pendingClearPasteEvidence.value = false
    applyBindingToForm(row.binding)
    loadVisibilityRuleForAnchor(row.anchorId)
    panelMode.value = 'edit'
    captureEditSnapshot()
    previewSyncedRevision.value = structureRevision.value
    suppressStructureBump.value = false
  }

  /**
   * CE-U21 Reload after version conflict — load server binding into the editor,
   * refresh concurrency token, pristine + clear per-anchor draft.
   */
  async function reloadBindingFromServer(row: MasterAnchorBindingRow | null) {
    if (!row) {
      return
    }
    suppressStructureBump.value = true
    bindingForm.anchorId = row.anchorId
    applyBindingToForm(row.binding)
    loadVisibilityRuleForAnchor(row.anchorId)
    await nextTick()
    structuredEditorRef.value?.markPristine()
    clearStructuredLocalDraftOnSave()
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
    if (expectedUpdatedAt.value) {
      payload.expectedUpdatedAt = expectedUpdatedAt.value
    }

    const saved = (await upsertBinding(
      props.templateId,
      bindingForm.anchorId,
      payload,
    )) as AnchorBinding | { bindings?: AnchorBinding[] } | undefined

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

    // Store upsert returns TemplateDetail after refetch; unit tests may return AnchorBinding.
    const fromDetail =
      saved
      && typeof saved === 'object'
      && 'bindings' in saved
      && Array.isArray(saved.bindings)
        ? saved.bindings.find((item) => item.anchorId === bindingForm.anchorId)
        : undefined
    const fromBinding =
      saved
      && typeof saved === 'object'
      && 'anchorId' in saved
      && saved.anchorId === bindingForm.anchorId
      && 'updatedAt' in saved
      && typeof saved.updatedAt === 'string'
        ? (saved as AnchorBinding)
        : undefined
    const nextToken = fromDetail?.updatedAt ?? fromBinding?.updatedAt
    if (nextToken) {
      expectedUpdatedAt.value = nextToken
    }

    structuredEditorRef.value?.markPristine()
    clearStructuredLocalDraftOnSave()
    captureEditSnapshot()
  }

  return {
    openEditPanel,
    reloadBindingFromServer,
    saveBindingDraft,
  }
}
