import type { Ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ComposerTranslation } from 'vue-i18n'
import type {
  BindingPanelMode,
  EditSnapshot,
  TemplateAuthoringBindingsPanelProps,
} from '@/composables/templateAuthoringBindingsTypes'
import type { useTemplatesStore } from '@/stores/templates'
import type { UpsertBindingPayload } from '@/types/template'

export function createTemplateAuthoringBindingsEditHandlers(deps: {
  t: ComposerTranslation
  props: TemplateAuthoringBindingsPanelProps
  emit: {
    (e: 'updated'): void
    (e: 'preview-refreshed', preview: import('@/types/template').PreviewRecord): void
  }
  panelMode: Ref<BindingPanelMode>
  editingAnchorId: Ref<string | null>
  editSnapshot: Ref<EditSnapshot | null>
  editorDirty: Ref<boolean>
  structureRevision: Ref<number>
  previewSyncedRevision: Ref<number>
  suppressStructureBump: Ref<boolean>
  bindingForm: UpsertBindingPayload
  visibilityEnabled: Ref<boolean>
  visibilityExpression: Ref<string>
  previewRefreshing: { value: boolean }
  localPreviewRefreshing: Ref<boolean>
  templatesStore: ReturnType<typeof useTemplatesStore>
  saveBindingDraft: () => Promise<void>
  dirtyGuardRequestLeave: (leave: () => void) => void
}) {
  const {
    t,
    props,
    emit,
    panelMode,
    editingAnchorId,
    editSnapshot,
    editorDirty,
    structureRevision,
    previewSyncedRevision,
    suppressStructureBump,
    previewRefreshing,
    localPreviewRefreshing,
    templatesStore,
    saveBindingDraft,
    dirtyGuardRequestLeave,
  } = deps

  function backToList() {
    void dirtyGuardRequestLeave(() => {
      panelMode.value = 'list'
      editingAnchorId.value = null
      editSnapshot.value = null
    })
  }

  async function handleSaveBinding() {
    try {
      await saveBindingDraft()
      panelMode.value = 'list'
      editingAnchorId.value = null
      editSnapshot.value = null
      ElMessage.success(t('templates.authoring.saveBindingSuccess'))
      emit('updated')
    } catch {
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

  return {
    backToList,
    handleSaveBinding,
    handlePreviewRefresh,
    handleEditorDirtyChange,
    handleStructureChange,
  }
}
