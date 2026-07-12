import { computed, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useStructuredContentHistory } from '@/composables/useStructuredContentHistory'
import { useStructuredContentLocalDraft } from '@/composables/useStructuredContentLocalDraft'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type {
  MasterStyleCatalog,
  PasteCleaningEvidence,
  PasteCleaningSummary,
  VariableSchema,
} from '@/types/template'
import { buildAcceptedPasteCleaningEvidence } from '@/utils/pasteCleaningEvidence'
import { buildVariableOptionLabel } from '@/utils/variableDisplayName'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  applyStyleToParagraphs,
  createNodeTemplate,
  insertBlockNode,
  parseStructuredContent,
  serializeStructuredContent,
  type ConfirmedNodeType,
  type StructuredContentDocument,
  type StructuredContentNode,
} from '@/utils/structuredContentNodes'
import type { StructuredContentDraftPayload } from '@/utils/structuredContentDraftStorage'

export interface ControlledStructuredContentEditorProps {
  modelValue: string
  templateId?: string
  /** Dev-version scope for local draft keys (LR-C2). When absent, drafts are disabled. */
  devVersionId?: string
  /** Optional binding anchor for draft recovery disambiguation. */
  anchorId?: string
  /** Optional server revision timestamp shown on the recovery banner. */
  serverUpdatedAt?: string | null
  variableKeys?: string[]
  variables?: VariableSchema[]
  contentModuleReferenceKeys?: string[]
  readonly?: boolean
  /** Saved baseline JSON; when omitted, initial modelValue is the baseline. */
  baseline?: string
}

export type ControlledStructuredContentEditorEmit = {
  (event: 'update:modelValue', value: string): void
  (event: 'dirty-change', dirty: boolean): void
  (event: 'structure-change'): void
  /** Fired on Accept with non-sensitive residue for binding upsert (blockedCount=0). */
  (event: 'paste-accepted', evidence: PasteCleaningEvidence): void
}

const DEFAULT_STYLE_CATALOG: MasterStyleCatalog = {
  catalogVersion: '1.0',
  entries: [
    { styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' },
    { styleKey: 'Heading1', applicableNodeTypes: ['sectionHeading'], renderPurpose: 'HEADING' },
  ],
}

export const STRUCTURED_BLOCK_NODE_TYPES: ConfirmedNodeType[] = [
  'sectionHeading',
  'paragraph',
  'list',
  'conditionBlock',
  'loopBlock',
  'tableComponentRef',
  'contentModuleRef',
]

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

  /** Structure-level undo/redo — session memory only; never written to C2 drafts (C3-C7). */
  const history = useStructuredContentHistory()
  const canUndo = history.canUndo
  const canRedo = history.canRedo
  let lastCommittedSnapshot = props.modelValue || DEFAULT_STRUCTURED_CONTENT_JSON
  let pendingCoalesceKey: string | null = null
  let syncingFromProps = false

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
    history.clear()
    history.beginApplying()
    try {
      documentModel.value = parseStructuredContent(draft.structureJson)
      lastCommittedSnapshot = serializeStructuredContent(documentModel.value)
    } finally {
      history.endApplying()
    }
    recoveryDraft.value = null
  }

  function handleDiscardDraft() {
    localDraft.clearDraft()
    recoveryDraft.value = null
    // C3-C10: discard draft resets history; server-loaded structure remains.
    history.clear()
  }

  const loadingCatalog = ref(false)
  const styleCatalog = ref<MasterStyleCatalog | null>(null)
  const selectedStyleKey = ref('')
  const documentModel = ref<StructuredContentDocument>(
    parseStructuredContent(props.modelValue || DEFAULT_STRUCTURED_CONTENT_JSON),
  )
  const pasteSummaryOpen = ref(false)
  const pasteSummary = ref<PasteCleaningSummary | null>(null)
  const pasteBlocked = ref(false)
  const pendingPasteJson = ref<string | null>(null)
  const prePasteSnapshot = ref(props.modelValue || DEFAULT_STRUCTURED_CONTENT_JSON)
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
      syncingFromProps = true
      try {
        const next = parseStructuredContent(value || DEFAULT_STRUCTURED_CONTENT_JSON)
        documentModel.value = next
        lastCommittedSnapshot = serializeStructuredContent(next)
        // Post-save server echo can re-enter before unmount; realign baseline and
        // re-clear so canonicalization differences cannot revive a draft.
        if (localDraft.areWritesSuppressed()) {
          pristineBaseline.value = lastCommittedSnapshot
          emit('dirty-change', false)
          localDraft.clearDraft({ suppressSubsequentWrites: true })
        }
      } finally {
        syncingFromProps = false
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
    const current = serializeStructuredContent(documentModel.value)
    emit('dirty-change', current !== pristineBaseline.value)
  }

  function markPristine() {
    pristineBaseline.value = serializeStructuredContent(documentModel.value)
    emit('dirty-change', false)
    // Clear-on-save (C2-C9): successful server persistence clears the local draft
    // and suppresses further writes until this editor instance is gone (or allowWrites).
    localDraft.clearDraft({ suppressSubsequentWrites: true })
    recoveryDraft.value = null
    // C3-C8: successful save clears undo/redo stacks.
    history.clear()
    lastCommittedSnapshot = pristineBaseline.value
  }

  watch(
    documentModel,
    (value) => {
      if (isReadonly.value) {
        return
      }
      const serialized = serializeStructuredContent(value)
      if (!history.isApplying() && !syncingFromProps) {
        history.commit(lastCommittedSnapshot, serialized, pendingCoalesceKey)
      }
      lastCommittedSnapshot = serialized
      emit('update:modelValue', serialized)
      emitDirtyState()
      emit('structure-change')
      scheduleLocalDraftWrite(serialized)
    },
    { deep: true },
  )

  function applyHistorySnapshot(snapshot: string) {
    history.beginApplying()
    try {
      documentModel.value = parseStructuredContent(snapshot)
      lastCommittedSnapshot = serializeStructuredContent(documentModel.value)
    } finally {
      history.endApplying()
    }
  }

  function doUndo() {
    if (isReadonly.value || !canUndo.value) {
      return
    }
    const current = serializeStructuredContent(documentModel.value)
    const previous = history.undo(current)
    if (previous == null) {
      return
    }
    pendingCoalesceKey = null
    applyHistorySnapshot(previous)
  }

  function doRedo() {
    if (isReadonly.value || !canRedo.value) {
      return
    }
    const current = serializeStructuredContent(documentModel.value)
    const next = history.redo(current)
    if (next == null) {
      return
    }
    pendingCoalesceKey = null
    applyHistorySnapshot(next)
  }

  function handleEditorKeydown(event: KeyboardEvent) {
    if (isReadonly.value) {
      return
    }
    const mod = event.ctrlKey || event.metaKey
    if (!mod) {
      return
    }
    const key = event.key.toLowerCase()
    if (key === 'z') {
      event.preventDefault()
      if (event.shiftKey) {
        doRedo()
      } else {
        doUndo()
      }
      return
    }
    if (key === 'y') {
      event.preventDefault()
      doRedo()
    }
  }

  function endFieldCoalesce() {
    pendingCoalesceKey = null
    history.endCoalesce()
  }

  onMounted(async () => {
    editorRootRef.value?.addEventListener('keydown', handleEditorKeydown, true)
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
    editorRootRef.value?.removeEventListener('keydown', handleEditorKeydown, true)
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

  function insertBlock(type: ConfirmedNodeType) {
    if (isReadonly.value) {
      return
    }
    pendingCoalesceKey = null
    documentModel.value = insertBlockNode(documentModel.value, type, selectedStyleKey.value)
  }

  function applySelectedStyle() {
    if (!selectedStyleKey.value || isReadonly.value) {
      return
    }
    pendingCoalesceKey = null
    documentModel.value = applyStyleToParagraphs(documentModel.value, selectedStyleKey.value)
  }

  function replaceBlock(index: number, next: StructuredContentNode) {
    const nodes = [...documentModel.value.nodes]
    nodes[index] = next
    documentModel.value = { ...documentModel.value, nodes }
  }

  function updateBlockField(index: number, field: keyof StructuredContentNode, value: string) {
    const node = documentModel.value.nodes[index]
    if (!node) {
      return
    }
    pendingCoalesceKey = `field:${index}:${String(field)}`
    replaceBlock(index, { ...node, [field]: value })
  }

  function updateInlineChild(blockIndex: number, childIndex: number, nextChild: StructuredContentNode) {
    const node = documentModel.value.nodes[blockIndex]
    if (!node) {
      return
    }
    pendingCoalesceKey = `inline:${blockIndex}:${childIndex}`
    const children = [...(node.children ?? [])]
    children[childIndex] = nextChild
    replaceBlock(blockIndex, { ...node, children })
  }

  function addInlineToBlock(blockIndex: number, type: ConfirmedNodeType) {
    const node = documentModel.value.nodes[blockIndex]
    if (!node) {
      return
    }
    pendingCoalesceKey = null
    const children = [...(node.children ?? []), createNodeTemplate(type, selectedStyleKey.value)]
    replaceBlock(blockIndex, { ...node, children })
  }

  function removeBlock(index: number) {
    if (isReadonly.value) {
      return
    }
    pendingCoalesceKey = null
    documentModel.value = {
      ...documentModel.value,
      nodes: documentModel.value.nodes.filter((_, nodeIndex) => nodeIndex !== index),
    }
  }

  async function handlePasteFile(event: Event) {
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file) {
      return
    }
    const html = await file.text()
    await runPasteClean(html)
    input.value = ''
  }

  async function runPasteClean(html: string) {
    if (!html.trim() || isReadonly.value || !props.templateId) {
      return
    }
    prePasteSnapshot.value = serializeStructuredContent(documentModel.value)
    try {
      const result = await templatesStore.pasteClean(props.templateId, {
        sourceHtml: html,
        prePasteStructuredContentJson: prePasteSnapshot.value,
      })
      pasteSummary.value = result.summary
      pasteBlocked.value = result.blocked
      pendingPasteJson.value = result.cleanedStructuredContentJson
      prePasteSnapshot.value = result.prePasteSnapshotJson
      pasteSummaryOpen.value = true
    } catch {
      ElMessage.error(t('templates.structuredEditor.error.pasteClean'))
    }
  }

  function acceptPaste() {
    if (pasteBlocked.value || !pasteSummary.value) {
      return
    }
    if (pendingPasteJson.value) {
      pendingCoalesceKey = null
      documentModel.value = parseStructuredContent(pendingPasteJson.value)
    }
    emit('paste-accepted', buildAcceptedPasteCleaningEvidence(pasteSummary.value))
    pendingPasteJson.value = null
    pasteSummary.value = null
    pasteBlocked.value = false
  }

  function cancelPaste() {
    pendingCoalesceKey = null
    documentModel.value = parseStructuredContent(prePasteSnapshot.value)
    pendingPasteJson.value = null
    pasteSummary.value = null
    pasteBlocked.value = false
  }

  function insertInline(type: ConfirmedNodeType) {
    if (isReadonly.value) {
      return
    }
    pendingCoalesceKey = null
    const nodes = [...documentModel.value.nodes]
    if (!nodes.length) {
      nodes.push(createNodeTemplate('paragraph', selectedStyleKey.value))
    }
    const lastIndex = nodes.length - 1
    const target = nodes[lastIndex]
    if (!target) {
      return
    }
    const children = [...(target.children ?? []), createNodeTemplate(type, selectedStyleKey.value)]
    nodes[lastIndex] = { ...target, children }
    documentModel.value = { ...documentModel.value, nodes }
  }

  return {
    t,
    isReadonly,
    recoveryDraft,
    editorRootRef: editorRootRef as Ref<HTMLElement | null>,
    canUndo,
    canRedo,
    blockNodeTypes,
    styleOptions,
    selectedStyleKey,
    loadingCatalog,
    documentModel,
    pasteSummaryOpen,
    pasteSummary,
    pasteBlocked,
    variableSelectOptions,
    listVariableOptions,
    clauseReferenceOptions,
    styleLabel,
    nodeLabel,
    markPristine,
    handleRestoreDraft,
    handleDiscardDraft,
    doUndo,
    doRedo,
    insertBlock,
    insertInline,
    applySelectedStyle,
    handlePasteFile,
    removeBlock,
    updateInlineChild,
    addInlineToBlock,
    updateBlockField,
    endFieldCoalesce,
    acceptPaste,
    cancelPaste,
    serializeStructuredContent,
  }
}
