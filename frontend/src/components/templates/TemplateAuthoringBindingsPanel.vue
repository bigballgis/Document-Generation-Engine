<script setup lang="ts">
import { computed, ref, toRefs } from 'vue'
import DirtyGuardConfirmDialog from '@/components/common/DirtyGuardConfirmDialog.vue'
import TemplateAuthoringBindingsList from '@/components/templates/TemplateAuthoringBindingsList.vue'
import TemplateAuthoringBindingEditor from '@/components/templates/TemplateAuthoringBindingEditor.vue'
import {
  useTemplateAuthoringBindingsPanel,
  type StructuredBindingEditorExpose,
} from '@/composables/useTemplateAuthoringBindingsPanel'
import type {
  AnchorBinding,
  CompositionRule,
  PreviewRecord,
  TemplateContentModuleReference,
  VariableSchema,
} from '@/types/template'

const props = defineProps<{
  templateId: string
  masterId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[] | null
  contentModuleReferences: TemplateContentModuleReference[]
  lastPreview?: PreviewRecord | null
  selectedTestDataSetId?: string | null
  generatingPreview?: boolean
}>()

const emit = defineEmits<{
  updated: []
  'preview-refreshed': [preview: PreviewRecord]
}>()

const structuredEditorRef = ref<StructuredBindingEditorExpose | null>(null)
const panel = useTemplateAuthoringBindingsPanel(props, emit, structuredEditorRef)

const {
  contentTypes,
  templatesStore,
  panelMode,
  loadingMaster,
  validating,
  editingAnchorId,
  validationResult,
  visibilityEnabled,
  visibilityExpression,
  editSnapshot,
  bindingForm,
  draftDevVersionId,
  bindingColumnFilters,
  filteredAnchorRows,
  contentTypeFilterOptions,
  configuredBindingCount,
  editingRow,
  contentModuleReferenceKeys,
  previewStale,
  previewRefreshing,
  dirtyGuardDialogVisible,
  dirtyGuardSaving,
  dirtyGuardShowSave,
  dirtyGuardStay,
  dirtyGuardDiscard,
  dirtyGuardSave,
  editingPasteResidueBlocked,
  resolveValidationStatusLabel,
  resolveConfiguredLabel,
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
  handleValidateBindings,
} = panel

const { variables, bindings } = toRefs(props)
const lastPreview = computed(() => props.lastPreview ?? null)
</script>

<template>
  <div class="bindings-panel authoring-panel">
    <TemplateAuthoringBindingsList
      v-if="panelMode === 'list'"
      :loading-master="loadingMaster"
      :validating="validating"
      :configured-binding-count="configuredBindingCount"
      :filtered-anchor-rows="filteredAnchorRows"
      :filter-anchor-id="bindingColumnFilters.anchorId"
      :filter-display-label="bindingColumnFilters.displayLabel"
      :filter-declared-content-type="bindingColumnFilters.declaredContentType"
      :filter-validation-status="bindingColumnFilters.validationStatus"
      :content-type-filter-options="contentTypeFilterOptions"
      :validation-result="validationResult"
      :resolve-validation-status-label="resolveValidationStatusLabel"
      :resolve-configured-label="resolveConfiguredLabel"
      :binding-has-paste-blockers="bindingHasPasteBlockers"
      @validate="handleValidateBindings"
      @edit="openEditPanel"
      @update:filter-anchor-id="bindingColumnFilters.anchorId = $event"
      @update:filter-display-label="bindingColumnFilters.displayLabel = $event"
      @update:filter-declared-content-type="bindingColumnFilters.declaredContentType = $event"
      @update:filter-validation-status="bindingColumnFilters.validationStatus = $event"
    />

    <TemplateAuthoringBindingEditor
      v-else
      ref="structuredEditorRef"
      v-model:declared-content-type="bindingForm.declaredContentType"
      v-model:structured-content-json="bindingForm.structuredContentJson"
      :template-id="templateId"
      :editing-row="editingRow"
      :editing-anchor-id="editingAnchorId"
      :draft-dev-version-id="draftDevVersionId"
      :content-types="contentTypes"
      v-model:visibility-enabled="visibilityEnabled"
      v-model:visibility-expression="visibilityExpression"
      :editing-paste-residue-blocked="editingPasteResidueBlocked"
      :variables="variables"
      :content-module-reference-keys="contentModuleReferenceKeys"
      :baseline-structured-content-json="editSnapshot?.structuredContentJson"
      :bindings="bindings"
      :last-preview="lastPreview"
      :preview-stale="previewStale"
      :preview-refreshing="previewRefreshing"
      :submitting="templatesStore.submitting"
      :paste-residue-item-label="pasteResidueItemLabel"
      @back="backToList"
      @save="handleSaveBinding"
      @clear-paste-residue="clearPendingPasteResidue"
      @dirty-change="handleEditorDirtyChange"
      @structure-change="handleStructureChange"
      @paste-accepted="handlePasteAccepted"
      @preview-refresh="handlePreviewRefresh"
    />

    <DirtyGuardConfirmDialog
      v-model="dirtyGuardDialogVisible"
      :show-save="dirtyGuardShowSave"
      :saving="dirtyGuardSaving"
      @stay="dirtyGuardStay"
      @discard="dirtyGuardDiscard"
      @save="dirtyGuardSave"
    />
  </div>
</template>

<style scoped lang="scss">
.bindings-panel {
  .section-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 0.75rem;
  }

  .section-description {
    margin: 0;
    color: var(--text-muted);
    max-width: 40rem;
  }
}
</style>
