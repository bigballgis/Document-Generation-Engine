import { computed, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  DEFAULT_STYLE_CATALOG,
  STRUCTURED_BLOCK_NODE_TYPES,
  type ControlledStructuredContentEditorEmit,
  type ControlledStructuredContentEditorProps,
} from '@/composables/controlledStructuredContentEditorTypes'
import { useStructuredContentDocumentModel } from '@/composables/useStructuredContentDocumentModel'
import { useStructuredContentLocalDraft } from '@/composables/useStructuredContentLocalDraft'
import { useStructuredContentPasteFlow } from '@/composables/useStructuredContentPasteFlow'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { MasterStyleCatalog, VariableSchema } from '@/types/template'
import { buildVariableOptionLabel } from '@/utils/variableDisplayName'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  parseStructuredContent,
  serializeStructuredContent,
  type ConfirmedNodeType,
} from '@/utils/structuredContentNodes'
import type { StructuredContentDraftPayload } from '@/utils/structuredContentDraftStorage'

export {
  STRUCTURED_BLOCK_NODE_TYPES,
  type ControlledStructuredContentEditorEmit,
  type ControlledStructuredContentEditorProps,
} from '@/composables/controlledStructuredContentEditorTypes'

export function useControlledStructuredContentEditor(
  props: ControlledStructuredContentEditorProps,
  emit: ControlledStructuredContentEditorEmit,
) {
  const pristineBaseline = ref(props.baseline ?? (props.modelValue || DEFAULT_STRUCTURED_CONTENT_JSON))

  const { t, te } = useI18n()
  const sessionStore = useSessionStore()
  const templatesStore = useTemplatesStore()

  const draftUserId = computed(() => sessionStore.session?.username ?? null)
  const draftTemplateId = computed(() => props.templateId ?? null)
  const draftDevVersionId = computed(() => props.devVersionId ?? null)
  const isReadonly = computed(() => props.readonly === true)

  const localDraft = useStructuredContentLocalDraft({
    userId: draftUserId,
    templateId: draftTemplateId,
    devVersionId: draftDevVersionId,
    readonly: isReadonly,
  })

  const doc = useStructuredContentDocumentModel({
    initialModelValue: props.modelValue || DEFAULT_STRUCTURED_CONTENT_JSON,
    isReadonly: () => isReadonly.value,
  })

  const recoveryDraft = ref<StructuredContentDraftPayload | null>(null)

  function evaluateDraftRecovery() {
    if (isReadonly.value) {
      recoveryDraft.value = null
      return
    }
    const serverStructure = props.modelValue || DEFAULT_STRUCTURED_CONTENT_JSON
    recoveryDraft.value = localDraft.evaluateRecovery(serverStructure, props.anchorId ?? null)
  }

  function scheduleLocalDraftWrite(structureJson: string) {
    if (isReadonly.value || structureJson === pristineBaseline.value) {
      return
    }
    // Post-save suppress: ignore prop-echo / deep-watch races (BDD-LRP-C2-002).
    if (localDraft.areWritesSuppressed()) {
      return
    }
    localDraft.scheduleWrite(structureJson, {
      serverUpdatedAt: props.serverUpdatedAt ?? null,
      anchorId: props.anchorId ?? null,
    })
  }

  function handleRestoreDraft() {
    const draft = recoveryDraft.value
    if (!draft) {
      return
    }
    // C3-C9: restore resets history; restored structure becomes sole current state.
    doc.resetHistoryWithStructure(draft.structureJson)
    recoveryDraft.value = null
  }

  function handleDiscardDraft() {
    localDraft.clearDraft()
    recoveryDraft.value = null
    // C3-C10: discard draft resets history; server-loaded structure remains.
    doc.clearHistoryOnly()
  }

  const loadingCatalog = ref(false)
  const styleCatalog = ref<MasterStyleCatalog | null>(null)
  const selectedStyleKey = ref('')
  const editorRootRef = ref<HTMLElement | null>(null)

  const blockNodeTypes = STRUCTURED_BLOCK_NODE_TYPES

  const styleOptions = computed(() => styleCatalog.value?.entries ?? [])

  const clauseReferenceOptions = computed(() =>
    (props.contentModuleReferenceKeys ?? []).map((referenceKey) => ({
      value: referenceKey,
      label: referenceKey,
    })),
  )

  const variableCatalog = computed(() => {
    if (props.variables?.length) {
      return props.variables
    }
    return (props.variableKeys ?? []).map(
      (variableKey): VariableSchema => ({
        variableKey,
        variableType: 'TEXT',
        required: false,
        defaultValue: null,
        enumValues: null,
        description: null,
      }),
    )
  })

  const variableSelectOptions = computed(() =>
    variableCatalog.value.map((variable) => ({
      value: variable.variableKey,
      label: buildVariableOptionLabel(variable),
    })),
  )

  const listVariableOptions = computed(() =>
    variableCatalog.value
      .filter((variable) => variable.variableType === 'LIST' || variable.variableType === 'OBJECT')
      .map((variable) => ({
        value: variable.variableKey,
        label: buildVariableOptionLabel(variable),
      })),
  )

  function styleLabel(styleKey: string): string {
    const key = `templates.structuredEditor.styleCatalog.keys.${styleKey}`
    return te(key) ? t(key) : styleKey
  }

  watch(
    () => props.modelValue,
    (value) => {
      doc.setSyncingFromProps(true)
      try {
        const next = parseStructuredContent(value || DEFAULT_STRUCTURED_CONTENT_JSON)
        doc.documentModel.value = next
        doc.setLastCommittedSnapshot(serializeStructuredContent(next))
        // Post-save server echo can re-enter before unmount; realign baseline and
        // re-clear so canonicalization differences cannot revive a draft.
        if (localDraft.areWritesSuppressed()) {
          pristineBaseline.value = doc.getLastCommittedSnapshot()
          emit('dirty-change', false)
          localDraft.clearDraft({ suppressSubsequentWrites: true })
        }
      } finally {
        doc.setSyncingFromProps(false)
      }
    },
  )

  watch(
    () => props.baseline,
    (value) => {
      if (value !== undefined) {
        pristineBaseline.value = value
      }
    },
  )

  function emitDirtyState() {
    const current = serializeStructuredContent(doc.documentModel.value)
    emit('dirty-change', current !== pristineBaseline.value)
  }

  function markPristine() {
    pristineBaseline.value = serializeStructuredContent(doc.documentModel.value)
    emit('dirty-change', false)
    // Clear-on-save (C2-C9): successful server persistence clears the local draft
    // and suppresses further writes until this editor instance is gone (or allowWrites).
    localDraft.clearDraft({ suppressSubsequentWrites: true })
    recoveryDraft.value = null
    // C3-C8: successful save clears undo/redo stacks.
    doc.clearHistoryOnly()
    doc.setLastCommittedSnapshot(pristineBaseline.value)
  }

  watch(
    doc.documentModel,
    (value) => {
      if (isReadonly.value) {
        return
      }
      const serialized = serializeStructuredContent(value)
      if (!doc.history.isApplying() && !doc.isSyncingFromProps()) {
        doc.history.commit(
          doc.getLastCommittedSnapshot(),
          serialized,
          doc.getPendingCoalesceKey(),
        )
      }
      doc.setLastCommittedSnapshot(serialized)
      emit('update:modelValue', serialized)
      emitDirtyState()
      emit('structure-change')
      scheduleLocalDraftWrite(serialized)
    },
    { deep: true },
  )

  const paste = useStructuredContentPasteFlow({
    templateId: () => props.templateId,
    isReadonly: () => isReadonly.value,
    documentModel: doc.documentModel,
    setPendingCoalesceKey: doc.setPendingCoalesceKey,
    emitPasteAccepted: (evidence) => emit('paste-accepted', evidence),
  })

  onMounted(async () => {
    editorRootRef.value?.addEventListener('keydown', doc.handleEditorKeydown, true)
    evaluateDraftRecovery()
    if (!props.templateId) {
      styleCatalog.value = DEFAULT_STYLE_CATALOG
      selectedStyleKey.value = DEFAULT_STYLE_CATALOG.entries[0]?.styleKey ?? 'BodyText'
      return
    }
    loadingCatalog.value = true
    try {
      styleCatalog.value = await templatesStore.fetchMasterStyleCatalog(props.templateId)
      selectedStyleKey.value = styleCatalog.value.entries[0]?.styleKey ?? 'BodyText'
    } catch {
      ElMessage.error(t('templates.structuredEditor.error.loadCatalog'))
    } finally {
      loadingCatalog.value = false
    }
  })

  onUnmounted(() => {
    editorRootRef.value?.removeEventListener('keydown', doc.handleEditorKeydown, true)
  })

  watch(
    () => [props.anchorId, props.devVersionId, props.templateId, draftUserId.value] as const,
    () => {
      evaluateDraftRecovery()
    },
  )

  function nodeLabel(type: ConfirmedNodeType | string): string {
    const key = `templates.structuredEditor.nodes.${type}`
    return te(key) ? t(key) : type
  }

  return {
    t,
    isReadonly,
    recoveryDraft,
    editorRootRef: editorRootRef as Ref<HTMLElement | null>,
    canUndo: doc.canUndo,
    canRedo: doc.canRedo,
    blockNodeTypes,
    styleOptions,
    selectedStyleKey,
    loadingCatalog,
    documentModel: doc.documentModel,
    pasteSummaryOpen: paste.pasteSummaryOpen,
    pasteSummary: paste.pasteSummary,
    pasteBlocked: paste.pasteBlocked,
    variableSelectOptions,
    listVariableOptions,
    clauseReferenceOptions,
    styleLabel,
    nodeLabel,
    markPristine,
    handleRestoreDraft,
    handleDiscardDraft,
    doUndo: doc.doUndo,
    doRedo: doc.doRedo,
    insertBlock: (type: ConfirmedNodeType) => doc.insertBlock(type, selectedStyleKey.value),
    insertInline: (type: ConfirmedNodeType) => doc.insertInline(type, selectedStyleKey.value),
    applySelectedStyle: () => doc.applySelectedStyle(selectedStyleKey.value),
    handlePasteFile: paste.handlePasteFile,
    removeBlock: doc.removeBlock,
    updateInlineChild: doc.updateInlineChild,
    addInlineToBlock: (blockIndex: number, type: ConfirmedNodeType) =>
      doc.addInlineToBlock(blockIndex, type, selectedStyleKey.value),
    updateBlockField: doc.updateBlockField,
    endFieldCoalesce: doc.endFieldCoalesce,
    acceptPaste: paste.acceptPaste,
    cancelPaste: paste.cancelPaste,
    serializeStructuredContent,
  }
}
