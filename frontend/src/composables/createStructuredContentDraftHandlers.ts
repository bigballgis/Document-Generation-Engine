import type { Ref } from 'vue'
import type { StructuredContentDraftPayload } from '@/utils/structuredContentDraftStorage'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  serializeStructuredContent,
  type StructuredContentDocument,
} from '@/utils/structuredContentNodes'

type LocalDraftApi = {
  evaluateRecovery: (
    serverStructure: string,
    anchorId: string | null,
  ) => StructuredContentDraftPayload | null
  scheduleWrite: (
    structureJson: string,
    meta: { serverUpdatedAt: string | null; anchorId: string | null },
  ) => void
  clearDraft: (options?: { suppressSubsequentWrites?: boolean }) => void
  areWritesSuppressed: () => boolean
}

type DocumentModelApi = {
  documentModel: { value: StructuredContentDocument }
  resetHistoryWithStructure: (structureJson: string) => void
  clearHistoryOnly: () => void
  setLastCommittedSnapshot: (snapshot: string) => void
}

export function createStructuredContentDraftHandlers(options: {
  isReadonly: () => boolean
  modelValue: () => string | undefined
  baselineAnchorId: () => string | null
  serverUpdatedAt: () => string | null
  pristineBaseline: Ref<string>
  recoveryDraft: Ref<StructuredContentDraftPayload | null>
  localDraft: LocalDraftApi
  doc: DocumentModelApi
  emitDirtyChange: (dirty: boolean) => void
}) {
  const {
    isReadonly,
    modelValue,
    baselineAnchorId,
    serverUpdatedAt,
    pristineBaseline,
    recoveryDraft,
    localDraft,
    doc,
    emitDirtyChange,
  } = options

  function evaluateDraftRecovery() {
    if (isReadonly()) {
      recoveryDraft.value = null
      return
    }
    const serverStructure = modelValue() || DEFAULT_STRUCTURED_CONTENT_JSON
    recoveryDraft.value = localDraft.evaluateRecovery(serverStructure, baselineAnchorId())
  }

  function scheduleLocalDraftWrite(structureJson: string) {
    if (isReadonly() || structureJson === pristineBaseline.value) {
      return
    }
    // Post-save suppress: ignore prop-echo / deep-watch races (BDD-LRP-C2-002).
    if (localDraft.areWritesSuppressed()) {
      return
    }
    localDraft.scheduleWrite(structureJson, {
      serverUpdatedAt: serverUpdatedAt(),
      anchorId: baselineAnchorId(),
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

  function markPristine() {
    pristineBaseline.value = serializeStructuredContent(doc.documentModel.value)
    emitDirtyChange(false)
    // Clear-on-save (C2-C9): successful server persistence clears the local draft
    // and suppresses further writes until this editor instance is gone (or allowWrites).
    localDraft.clearDraft({ suppressSubsequentWrites: true })
    recoveryDraft.value = null
    // C3-C8: successful save clears undo/redo stacks.
    doc.clearHistoryOnly()
    doc.setLastCommittedSnapshot(pristineBaseline.value)
  }

  return {
    evaluateDraftRecovery,
    scheduleLocalDraftWrite,
    handleRestoreDraft,
    handleDiscardDraft,
    markPristine,
  }
}
