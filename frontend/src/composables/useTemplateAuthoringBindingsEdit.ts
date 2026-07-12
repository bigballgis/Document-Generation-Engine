import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
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
import { buildMasterAnchorBindingRows, type MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import { mergeAnchorVisibilityRule } from '@/utils/mergeAnchorVisibilityRule'
import {
  buildBindingUpsertWithPasteEvidence,
  hasUnresolvedPasteBlockers,
} from '@/utils/pasteCleaningEvidence'
import { clearExactStructuredDraftOnSave } from '@/utils/structuredContentDraftStorage'
import { DEFAULT_STRUCTURED_CONTENT_JSON } from '@/utils/structuredContentNodes'
import type { PasteCleaningEvidence, UpsertBindingPayload } from '@/types/template'
import type { MasterAnchor } from '@/types/master'

export function useTemplateAuthoringBindingsEdit(
  props: TemplateAuthoringBindingsPanelProps,
  emit: {
    (e: 'updated'): void
    (e: 'preview-refreshed', preview: import('@/types/template').PreviewRecord): void
  },
  structuredEditorRef: Ref<StructuredBindingEditorExpose | null>,
  masterAnchors: Ref<MasterAnchor[]>,
) {
  const { t, te } = useI18n()
  const route = useRoute()
  const templatesStore = useTemplatesStore()
  const sessionStore = useSessionStore()

  const draftDevVersionId = computed(() => {
    const fromRoute = route.params.devVersionId
    return typeof fromRoute === 'string' && fromRoute.length > 0 ? fromRoute : ''
  })

  /**
   * C2-C9 / BDD-LRP-C2-005: clear local draft **only on successful save**.
   * Exact key only (BDD-LRP-C2-004) — no templateId sweep.
   * Editor `markPristine` / `clearDraft` already bumps writeEpoch and cancels pending debounce.
   */
  function clearStructuredLocalDraftOnSave() {
    clearExactStructuredDraftOnSave(
      localStorage,
      sessionStore.session?.username,
      props.templateId,
      draftDevVersionId.value,
    )
  }

  const panelMode = ref<BindingPanelMode>('list')
  const editingAnchorId = ref<string | null>(null)
  const visibilityEnabled = ref(false)
  const visibilityExpression = ref('')
  const editorDirty = ref(false)
  const structureRevision = ref(0)
  const previewSyncedRevision = ref(0)
  const editSnapshot = ref<EditSnapshot | null>(null)
  const suppressStructureBump = ref(false)

  const bindingForm = reactive<UpsertBindingPayload>({
    anchorId: '',
    declaredContentType: 'TEXT',
    structuredContentJson: DEFAULT_STRUCTURED_CONTENT_JSON,
  })

  /** Pending paste residue from Accept; sent on next binding upsert (blockedCount=0). */
  const pendingPasteEvidence = ref<PasteCleaningEvidence | null>(null)
  /** Explicit clear path when rewriting without new Accept evidence (S5). */
  const pendingClearPasteEvidence = ref(false)

  const anchorRowsSource = computed(() =>
    buildMasterAnchorBindingRows(masterAnchors.value, props.bindings),
  )

  const editingRow = computed(
    () => anchorRowsSource.value.find((row) => row.anchorId === editingAnchorId.value) ?? null,
  )

  const formDirty = computed(() => {
    if (!editSnapshot.value || panelMode.value !== 'edit') {
      return false
    }
    const snapshot = editSnapshot.value
    return (
      bindingForm.declaredContentType !== snapshot.declaredContentType
      || bindingForm.structuredContentJson !== snapshot.structuredContentJson
      || visibilityEnabled.value !== snapshot.visibilityEnabled
      || visibilityExpression.value !== snapshot.visibilityExpression
      || editorDirty.value
    )
  })

  const previewStale = computed(
    () => props.lastPreview != null && structureRevision.value !== previewSyncedRevision.value,
  )

  const localPreviewRefreshing = ref(false)
  const previewRefreshing = computed(
    () => props.generatingPreview === true || localPreviewRefreshing.value,
  )

  const {
    dialogVisible: dirtyGuardDialogVisible,
    saving: dirtyGuardSaving,
    showSaveAction: dirtyGuardShowSave,
    handleStay: dirtyGuardStay,
    handleDiscard: dirtyGuardDiscard,
    handleSave: dirtyGuardSave,
    requestLeave: dirtyGuardRequestLeave,
  } = useDirtyGuard({
    isDirty: formDirty,
    enabled: computed(() => panelMode.value === 'edit'),
    onSave: async () => {
      try {
        await saveBindingDraft()
        return true
      } catch {
        // BDD-LRP-C2-005: save failure retains localStorage draft (do not clear here).
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

    // PUT /rules reuses a @NotEmpty validation DTO — `[]` is rejected. Skip no-op
    // rule saves when neither the loaded template nor the merge result has rules
    // (typical binding-structure-only edit; BDD-LRP-C2-002).
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

      // Close editor before parent refresh (`updated`) so prop-echo cannot race
      // a debounce rewrite into localStorage after clear-on-save (BDD-LRP-C2-002).
      panelMode.value = 'list'
      editingAnchorId.value = null
      editSnapshot.value = null

      ElMessage.success(t('templates.authoring.saveBindingSuccess'))
      emit('updated')
    } catch {
      // BDD-LRP-C2-005: retain localStorage draft on failure.
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
    draftDevVersionId,
    panelMode,
    editingAnchorId,
    visibilityEnabled,
    visibilityExpression,
    editSnapshot,
    bindingForm,
    anchorRowsSource,
    editingRow,
    previewStale,
    previewRefreshing,
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
