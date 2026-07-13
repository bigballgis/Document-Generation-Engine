import { computed, reactive, ref, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  type BindingPanelMode,
  type EditSnapshot,
  type StructuredBindingEditorExpose,
  type TemplateAuthoringBindingsPanelProps,
} from '@/composables/templateAuthoringBindingsTypes'
import { useTemplateAuthoringBindingsEditActions } from '@/composables/useTemplateAuthoringBindingsEditActions'
import { buildMasterAnchorBindingRows } from '@/utils/masterAnchorBindingRows'
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
  const route = useRoute()

  const draftDevVersionId = computed(() => {
    const fromRoute = route.params.devVersionId
    return typeof fromRoute === 'string' && fromRoute.length > 0 ? fromRoute : ''
  })

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

  const pendingPasteEvidence = ref<PasteCleaningEvidence | null>(null)
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

  const actions = useTemplateAuthoringBindingsEditActions({
    props,
    emit,
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
    formDirty,
    editingRow,
    previewRefreshing,
    localPreviewRefreshing,
  })

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
    ...actions,
  }
}
