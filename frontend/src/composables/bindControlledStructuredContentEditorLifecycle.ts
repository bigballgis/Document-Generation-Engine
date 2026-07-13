import { onMounted, onUnmounted, watch, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DEFAULT_STYLE_CATALOG,
  type ControlledStructuredContentEditorEmit,
  type ControlledStructuredContentEditorProps,
} from '@/composables/controlledStructuredContentEditorTypes'
import type { useStructuredContentDocumentModel } from '@/composables/useStructuredContentDocumentModel'
import type { MasterStyleCatalog } from '@/types/template'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  parseStructuredContent,
  serializeStructuredContent,
} from '@/utils/structuredContentNodes'

type DocModel = ReturnType<typeof useStructuredContentDocumentModel>
type LocalDraftLike = {
  areWritesSuppressed: () => boolean
  clearDraft: (options: { suppressSubsequentWrites: boolean }) => void
}

export function bindControlledStructuredContentEditorLifecycle(deps: {
  props: ControlledStructuredContentEditorProps
  emit: ControlledStructuredContentEditorEmit
  doc: DocModel
  localDraft: LocalDraftLike
  pristineBaseline: Ref<string>
  styleCatalog: Ref<MasterStyleCatalog | null>
  selectedStyleKey: Ref<string>
  loadingCatalog: Ref<boolean>
  editorRootRef: Ref<HTMLElement | null>
  draftUserId: { value: string | null }
  isReadonly: () => boolean
  evaluateDraftRecovery: () => void
  scheduleLocalDraftWrite: (serialized: string) => void
  fetchMasterStyleCatalog: (templateId: string) => Promise<MasterStyleCatalog>
  t: (key: string) => string
}) {
  const {
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
    isReadonly,
    evaluateDraftRecovery,
    scheduleLocalDraftWrite,
    fetchMasterStyleCatalog,
    t,
  } = deps

  function emitDirtyState() {
    const current = serializeStructuredContent(doc.documentModel.value)
    emit('dirty-change', current !== pristineBaseline.value)
  }

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

  watch(
    doc.documentModel,
    (value) => {
      if (isReadonly()) {
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
      styleCatalog.value = await fetchMasterStyleCatalog(props.templateId)
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
}
