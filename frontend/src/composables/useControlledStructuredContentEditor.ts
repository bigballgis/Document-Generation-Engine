import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  STRUCTURED_BLOCK_NODE_TYPES,
  type ControlledStructuredContentEditorEmit,
  type ControlledStructuredContentEditorProps,
} from '@/composables/controlledStructuredContentEditorTypes'
import { createStructuredContentDraftHandlers } from '@/composables/createStructuredContentDraftHandlers'
import { useControlledStructuredContentCatalogOptions } from '@/composables/useControlledStructuredContentCatalogOptions'
import { useStructuredContentDocumentModel } from '@/composables/useStructuredContentDocumentModel'
import { useStructuredContentLocalDraft } from '@/composables/useStructuredContentLocalDraft'
import { useStructuredContentPasteFlow } from '@/composables/useStructuredContentPasteFlow'
import { bindControlledStructuredContentEditorLifecycle } from '@/composables/bindControlledStructuredContentEditorLifecycle'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { MasterStyleCatalog } from '@/types/template'
import {
  structuredBlockCardTestId,
  validateStructuredContentDocument,
  type StructuredContentValidationIssue,
} from '@/utils/structuredContentBindingValidation'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
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
  const draftAnchorId = computed(() => props.anchorId ?? null)
  const isReadonly = computed(() => props.readonly === true)

  const localDraft = useStructuredContentLocalDraft({
    userId: draftUserId,
    templateId: draftTemplateId,
    devVersionId: draftDevVersionId,
    anchorId: draftAnchorId,
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
  const validationIssues = ref<StructuredContentValidationIssue[]>([])
  const validationRan = ref(false)

  const declaredVariableKeys = computed(() => {
    const keys = new Set<string>()
    for (const key of props.variableKeys ?? []) {
      if (key.trim()) {
        keys.add(key.trim())
      }
    }
    for (const variable of props.variables ?? []) {
      if (variable.variableKey?.trim()) {
        keys.add(variable.variableKey.trim())
      }
    }
    return keys
  })

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

  const paste = useStructuredContentPasteFlow({
    templateId: () => props.templateId,
    isReadonly: () => isReadonly.value,
    documentModel: doc.documentModel,
    setPendingCoalesceKey: doc.setPendingCoalesceKey,
    emitPasteAccepted: (evidence) => emit('paste-accepted', evidence),
  })

  bindControlledStructuredContentEditorLifecycle({
    props,
    emit,
    doc,
    localDraft,
    pristineBaseline,
    styleCatalog,
    selectedStyleKey,
    loadingCatalog,
    editorRootRef,
    draftUserId,
    isReadonly: () => isReadonly.value,
    evaluateDraftRecovery,
    scheduleLocalDraftWrite,
    fetchMasterStyleCatalog: (templateId) => templatesStore.fetchMasterStyleCatalog(templateId),
    t,
  })

  function nodeLabel(type: ConfirmedNodeType | string): string {
    const key = `templates.structuredEditor.nodes.${type}`
    return te(key) ? t(key) : type
  }

  function validateStructure(): StructuredContentValidationIssue[] {
    validationRan.value = true
    validationIssues.value = validateStructuredContentDocument(
      doc.documentModel.value,
      declaredVariableKeys.value,
    )
    return validationIssues.value
  }

  function scrollToBlock(path: number[]): void {
    const root = editorRootRef.value
    if (!root) {
      return
    }
    const target = root.querySelector<HTMLElement>(
      `[data-testid="${structuredBlockCardTestId(path)}"]`,
    )
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    const card = target?.closest('.block-card')
    card?.classList.add('block-card--highlight')
    window.setTimeout(() => card?.classList.remove('block-card--highlight'), 1600)
  }

  function handleScrollToIssue(issue: StructuredContentValidationIssue): void {
    scrollToBlock(issue.blockPath)
  }

  return {
    t,
    te,
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
    insertNestedBlock: (parentPath: number[], type: ConfirmedNodeType) =>
      doc.insertNestedBlock(parentPath, type, selectedStyleKey.value),
    insertInline: (type: ConfirmedNodeType) => doc.insertInline(type, selectedStyleKey.value),
    applySelectedStyle: () => doc.applySelectedStyle(selectedStyleKey.value),
    handlePasteFile: paste.handlePasteFile,
    removeBlock: doc.removeBlock,
    reorderBlock: doc.reorderBlock,
    copyBlock: doc.copyBlock,
    updateInlineChild: doc.updateInlineChild,
    addInlineToBlock: (path: number[], type: ConfirmedNodeType) =>
      doc.addInlineToBlock(path, type, selectedStyleKey.value),
    updateBlockField: doc.updateBlockField,
    endFieldCoalesce: doc.endFieldCoalesce,
    acceptPaste: paste.acceptPaste,
    cancelPaste: paste.cancelPaste,
    serializeStructuredContent,
    validationIssues,
    validationRan,
    validateStructure,
    scrollToBlock,
    handleScrollToIssue,
  }
}
