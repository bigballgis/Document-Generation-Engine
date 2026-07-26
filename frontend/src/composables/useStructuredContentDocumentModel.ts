import { ref, type Ref } from 'vue'
import { useStructuredContentHistory } from '@/composables/useStructuredContentHistory'
import { createStructuredContentDocumentMutations } from '@/composables/createStructuredContentDocumentMutations'
import type { NodePath } from '@/utils/structuredContentNodePath'
import {
  parseStructuredContent,
  serializeStructuredContent,
  type ConfirmedNodeType,
  type StructuredContentDocument,
  type StructuredContentNode,
  DEFAULT_STRUCTURED_CONTENT_JSON,
} from '@/utils/structuredContentNodes'

export interface StructuredContentDocumentModelApi {
  documentModel: Ref<StructuredContentDocument>
  canUndo: Ref<boolean>
  canRedo: Ref<boolean>
  history: ReturnType<typeof useStructuredContentHistory>
  getLastCommittedSnapshot: () => string
  setLastCommittedSnapshot: (value: string) => void
  isSyncingFromProps: () => boolean
  setSyncingFromProps: (value: boolean) => void
  getPendingCoalesceKey: () => string | null
  setPendingCoalesceKey: (value: string | null) => void
  applyHistorySnapshot: (snapshot: string) => void
  doUndo: () => void
  doRedo: () => void
  handleEditorKeydown: (event: KeyboardEvent) => void
  endFieldCoalesce: () => void
  focusedPath: Ref<NodePath | null>
  setFocusedPath: (path: NodePath | null) => void
  insertBlock: (type: ConfirmedNodeType, selectedStyleKey: string) => void
  insertNestedBlock: (parentPath: NodePath, type: ConfirmedNodeType, selectedStyleKey: string) => void
  insertInline: (type: ConfirmedNodeType, selectedStyleKey: string) => void
  applySelectedStyle: (selectedStyleKey: string, applicableNodeTypes?: string[]) => void
  updateBlockField: (path: NodePath, field: keyof StructuredContentNode, value: string) => void
  updateInlineChild: (
    path: NodePath,
    childIndex: number,
    nextChild: StructuredContentNode,
  ) => void
  addInlineToBlock: (path: NodePath, type: ConfirmedNodeType, selectedStyleKey: string) => void
  removeBlock: (path: NodePath) => void
  reorderBlock: (path: NodePath, toIndex: number) => void
  copyBlock: (path: NodePath) => void
  resetHistoryWithStructure: (structureJson: string) => void
  clearHistoryOnly: () => void
}

export function useStructuredContentDocumentModel(options: {
  initialModelValue: string
  isReadonly: () => boolean
}): StructuredContentDocumentModelApi {
  /** Structure-level undo/redo — session memory only; never written to C2 drafts (C3-C7). */
  const history = useStructuredContentHistory()
  const canUndo = history.canUndo
  const canRedo = history.canRedo
  let lastCommittedSnapshot = options.initialModelValue || DEFAULT_STRUCTURED_CONTENT_JSON
  let pendingCoalesceKey: string | null = null
  let syncingFromProps = false

  const documentModel = ref<StructuredContentDocument>(
    parseStructuredContent(options.initialModelValue || DEFAULT_STRUCTURED_CONTENT_JSON),
  )

  function getLastCommittedSnapshot() {
    return lastCommittedSnapshot
  }

  function setLastCommittedSnapshot(value: string) {
    lastCommittedSnapshot = value
  }

  function isSyncingFromProps() {
    return syncingFromProps
  }

  function setSyncingFromProps(value: boolean) {
    syncingFromProps = value
  }

  function getPendingCoalesceKey() {
    return pendingCoalesceKey
  }

  function setPendingCoalesceKey(value: string | null) {
    pendingCoalesceKey = value
  }

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
    if (options.isReadonly() || !canUndo.value) {
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
    if (options.isReadonly() || !canRedo.value) {
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
    if (options.isReadonly()) {
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

  const focusedPath = ref<NodePath | null>(null)

  const mutations = createStructuredContentDocumentMutations({
    documentModel,
    isReadonly: options.isReadonly,
    setPendingCoalesceKey,
    focusedPath,
  })

  function resetHistoryWithStructure(structureJson: string) {
    history.clear()
    history.beginApplying()
    try {
      documentModel.value = parseStructuredContent(structureJson)
      lastCommittedSnapshot = serializeStructuredContent(documentModel.value)
    } finally {
      history.endApplying()
    }
  }

  function clearHistoryOnly() {
    history.clear()
  }

  return {
    documentModel,
    canUndo,
    canRedo,
    history,
    getLastCommittedSnapshot,
    setLastCommittedSnapshot,
    isSyncingFromProps,
    setSyncingFromProps,
    getPendingCoalesceKey,
    setPendingCoalesceKey,
    applyHistorySnapshot,
    doUndo,
    doRedo,
    handleEditorKeydown,
    endFieldCoalesce,
    ...mutations,
    resetHistoryWithStructure,
    clearHistoryOnly,
  }
}
