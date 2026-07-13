import { computed, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  DEFAULT_STYLE_CATALOG,
  STRUCTURED_BLOCK_NODE_TYPES,
  type ControlledStructuredContentEditorEmit,
  type ControlledStructuredContentEditorProps,
} from '@/composables/controlledStructuredContentEditorTypes'
import { createStructuredContentDraftHandlers } from '@/composables/createStructuredContentDraftHandlers'
import { useControlledStructuredContentCatalogOptions } from '@/composables/useControlledStructuredContentCatalogOptions'
import { useStructuredContentDocumentModel } from '@/composables/useStructuredContentDocumentModel'
import { useStructuredContentLocalDraft } from '@/composables/useStructuredContentLocalDraft'
import { useStructuredContentPasteFlow } from '@/composables/useStructuredContentPasteFlow'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { MasterStyleCatalog } from '@/types/template'
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

  const {
    evaluateDraftRecovery,
    scheduleLocalDraftWrite,
    handleRestoreDraft,
    handleDiscardDraft,
    markPristine,
  } = createStructuredContentDraftHandlers({
    isReadonly: () => isReadonly.value,
    modelValue: () => props.modelValue,
    baselineAnchorId: () => props.anchorId ?? null,
    serverUpdatedAt: () => props.serverUpdatedAt ?? null,
    pristineBaseline,
    recoveryDraft,
    localDraft,
    doc,
    emitDirtyChange: (dirty) => emit('dirty-change', dirty),
  })

  const loadingCatalog = ref(false)
  const styleCatalog = ref<MasterStyleCatalog | null>(null)
  const selectedStyleKey = ref('')
  const editorRootRef = ref<HTMLElement | null>(null)
  const blockNodeTypes = STRUCTURED_BLOCK_NODE_TYPES

  const {
    styleOptions,
    clauseReferenceOptions,
    variableSelectOptions,
    listVariableOptions,
    styleLabel,
  } = useControlledStructuredContentCatalogOptions({
    props,
    styleCatalog,
    te,
    t,
  })

  watch(
    () => props.modelValue,
    (value) => {
      doc.setSyncingFromProps(true)
      try {
        const next = parseStructuredContent(value || DEFAULT_STRUCTURED_CONTENT_JSON)
        doc.documentModel.value = next
        doc.setLastCommittedSnapshot(serializeStructuredContent(next))
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
