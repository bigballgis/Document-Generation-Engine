<script setup lang="ts">
import PasteCleaningSummaryDialog from '@/components/authoring/PasteCleaningSummaryDialog.vue'
import StructuredContentBlockCard from '@/components/authoring/StructuredContentBlockCard.vue'
import StructuredContentEditorToolbar from '@/components/authoring/StructuredContentEditorToolbar.vue'
import StructuredDraftRecoveryBanner from '@/components/authoring/StructuredDraftRecoveryBanner.vue'
import {
  useControlledStructuredContentEditor,
  type ControlledStructuredContentEditorProps,
} from '@/composables/useControlledStructuredContentEditor'
import type { PasteCleaningEvidence } from '@/types/template'

const props = defineProps<ControlledStructuredContentEditorProps>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'dirty-change': [dirty: boolean]
  'structure-change': []
  /** Fired on Accept with non-sensitive residue for binding upsert (blockedCount=0). */
  'paste-accepted': [evidence: PasteCleaningEvidence]
}>()

const {
  t,
  te,
  isReadonly,
  recoveryDraft,
  editorRootRef,
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
  insertNestedBlock,
  insertInline,
  applySelectedStyle,
  setFocusedPath,
  handlePasteFile,
  removeBlock,
  reorderBlock,
  copyBlock,
  updateInlineChild,
  addInlineToBlock,
  updateBlockField,
  endFieldCoalesce,
  acceptPaste,
  cancelPaste,
  serializeStructuredContent,
  validationIssues,
  validationRan,
  validateStructure,
  scrollToBlock,
  handleScrollToIssue,
} = useControlledStructuredContentEditor(props, emit)

defineExpose({ markPristine, validateStructure, scrollToBlock })
</script>

<template>
  <div
    ref="editorRootRef"
    class="structured-editor"
    data-testid="controlled-structured-content-editor"
    tabindex="-1"
  >
    <StructuredDraftRecoveryBanner
      v-if="recoveryDraft"
      :draft-updated-at="recoveryDraft.draftUpdatedAt"
      :server-updated-at="recoveryDraft.serverUpdatedAt ?? serverUpdatedAt ?? null"
      @restore="handleRestoreDraft"
      @discard="handleDiscardDraft"
    />
    <p v-if="!isReadonly" class="editor-hint">{{ t('templates.structuredEditor.bindingHint') }}</p>
    <p v-else class="editor-hint">{{ t('templates.structuredEditor.readonlyHint') }}</p>

    <StructuredContentEditorToolbar
      v-if="!isReadonly"
      :can-undo="canUndo"
      :can-redo="canRedo"
      :block-node-types="blockNodeTypes"
      :style-options="styleOptions"
      :selected-style-key="selectedStyleKey"
      :loading-catalog="loadingCatalog"
      :style-label="styleLabel"
      :node-label="nodeLabel"
      :compact="Boolean(props.compactToolbar)"
      @undo="doUndo"
      @redo="doRedo"
      @insert-block="insertBlock"
      @insert-inline="insertInline"
      @update:selected-style-key="selectedStyleKey = $event"
      @apply-style="applySelectedStyle"
      @paste-file="handlePasteFile"
    />

    <div v-if="!isReadonly" class="validation-panel">
      <el-button
        size="small"
        data-testid="structured-editor-validate-structure"
        @click="validateStructure"
      >
        {{ t('templates.structuredEditor.validateStructure') }}
      </el-button>
      <el-alert
        v-if="validationIssues.length"
        type="warning"
        show-icon
        :closable="false"
        class="validation-panel__alert"
        :title="t('templates.structuredEditor.validationIssuesTitle', { count: validationIssues.length })"
      >
        <ul class="validation-panel__issues" data-testid="structured-editor-validation-issues">
          <li v-for="(issue, index) in validationIssues" :key="`${issue.location}-${index}`">
            <button
              type="button"
              class="validation-panel__issue-link"
              data-testid="structured-editor-validation-issue"
              @click="handleScrollToIssue(issue)"
            >
              <span class="validation-panel__issue-location">{{ issue.location }}</span>
              <span class="validation-panel__issue-message">
                {{ te(issue.messageKey) ? t(issue.messageKey) : issue.messageKey }}
              </span>
            </button>
          </li>
        </ul>
      </el-alert>
      <el-alert
        v-else-if="validationRan"
        type="success"
        show-icon
        :closable="false"
        class="validation-panel__alert"
        :title="t('templates.structuredEditor.validationPassed')"
      />
    </div>

    <div class="editor-surface" data-testid="editor-paste-area">
      <StructuredContentBlockCard
        v-for="(node, index) in documentModel.nodes"
        :key="`${node.type}-${index}`"
        :node="node"
        :path="[index]"
        :sibling-index="index"
        :sibling-count="documentModel.nodes.length"
        :readonly="isReadonly"
        :variable-select-options="variableSelectOptions"
        :list-variable-options="listVariableOptions"
        :clause-reference-options="clauseReferenceOptions"
        :table-component-options="[]"
        :node-label="nodeLabel"
        @remove="removeBlock"
        @reorder-block="reorderBlock"
        @copy-block="copyBlock"
        @update-inline-child="updateInlineChild"
        @add-inline="addInlineToBlock"
        @update-block-field="updateBlockField"
        @insert-nested-block="insertNestedBlock"
        @focus-path="setFocusedPath"
        @end-field-coalesce="endFieldCoalesce"
      />

      <el-empty
        v-if="!documentModel.nodes.length"
        :description="t('templates.structuredEditor.emptyDocument')"
      />
    </div>

    <details v-if="!isReadonly" class="json-preview">
      <summary>{{ t('templates.structuredEditor.jsonPreview') }}</summary>
      <pre>{{ serializeStructuredContent(documentModel) }}</pre>
    </details>

    <PasteCleaningSummaryDialog
      v-model="pasteSummaryOpen"
      :summary="pasteSummary"
      :blocked="pasteBlocked"
      @accept="acceptPaste"
      @cancel="cancelPaste"
      @undo="cancelPaste"
    />
  </div>
</template>

<style scoped lang="scss">
.structured-editor {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.editor-hint {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.validation-panel {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.validation-panel__alert {
  margin: 0;
}

.validation-panel__issues {
  margin: 0.5rem 0 0;
  padding-left: 1.1rem;
}

.validation-panel__issue-link {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  width: 100%;
  padding: 0.15rem 0;
  border: none;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.validation-panel__issue-location {
  font-family: var(--font-mono, ui-monospace, monospace);
  font-size: 0.8rem;
  color: var(--text-muted);
}

.editor-surface {
  min-height: 8rem;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-color);
}

.json-preview {
  summary {
    cursor: pointer;
    color: var(--text-muted);
    margin-bottom: 0.5rem;
  }

  pre {
    margin: 0;
    padding: 0.75rem;
    border-radius: var(--radius-md);
    background: var(--surface-muted);
    overflow: auto;
    font-size: 0.8rem;
  }
}
</style>
