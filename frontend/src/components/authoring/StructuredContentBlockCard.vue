<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Rank } from '@element-plus/icons-vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import ConditionExpressionInput from '@/components/authoring/ConditionExpressionInput.vue'
import StructuredContentBlockCard from '@/components/authoring/StructuredContentBlockCard.vue'
import {
  STRUCTURED_BLOCK_NODE_TYPES,
} from '@/composables/controlledStructuredContentEditorTypes'
import {
  areSiblingPaths,
  canAddNestedBlockChildren,
  isNestedContainerType,
  pathTestId,
  type NodePath,
} from '@/utils/structuredContentNodePath'
import {
  getStructuredBlockDragPathKey,
  setStructuredBlockDragPathKey,
} from '@/utils/structuredContentDragState'
import type { ConfirmedNodeType, StructuredContentNode } from '@/utils/structuredContentNodes'

const props = withDefaults(
  defineProps<{
    node: StructuredContentNode
    path: NodePath
    siblingIndex: number
    siblingCount: number
    readonly: boolean
    variableSelectOptions: Array<{ value: string; label: string }>
    listVariableOptions: Array<{ value: string; label: string }>
    clauseReferenceOptions: Array<{ value: string; label: string }>
    /** Optional catalogue keys for tableComponentRef (FOS-W3-6). */
    tableComponentOptions?: Array<{ value: string; label: string }>
    nodeLabel: (type: ConfirmedNodeType | string) => string
  }>(),
  {
    tableComponentOptions: () => [],
  },
)

const emit = defineEmits<{
  remove: [path: NodePath]
  'update-inline-child': [path: NodePath, childIndex: number, nextChild: StructuredContentNode]
  'add-inline': [path: NodePath, type: ConfirmedNodeType]
  'update-block-field': [path: NodePath, field: keyof StructuredContentNode, value: string]
  'insert-nested-block': [parentPath: NodePath, type: ConfirmedNodeType]
  'reorder-block': [path: NodePath, toIndex: number]
  'copy-block': [path: NodePath]
  'focus-path': [path: NodePath]
  'end-field-coalesce': []
}>()

const { t } = useI18n()

const nestedBlockTypes = STRUCTURED_BLOCK_NODE_TYPES
const canAddChildren = computed(() => canAddNestedBlockChildren(props.path))
const showNestedPanel = computed(() => isNestedContainerType(props.node.type))
const nestedChildren = computed(() => props.node.children ?? [])
const nestedPathKey = computed(() => pathTestId(props.path))
const canReorder = computed(() => !props.readonly && props.siblingCount > 1)
const dragOver = ref(false)
const conditionVariableKeys = computed(() => props.variableSelectOptions.map((option) => option.value))

function conditionExpression(node: StructuredContentNode): string {
  return node.conditionExpression ?? node.key ?? ''
}

function loopVariable(node: StructuredContentNode): string {
  return node.loopVariable ?? node.key ?? ''
}

function childPath(childIndex: number): NodePath {
  return [...props.path, childIndex]
}

function onDragStart(event: DragEvent) {
  if (!canReorder.value) {
    event.preventDefault()
    return
  }
  setStructuredBlockDragPathKey(pathTestId(props.path))
  event.dataTransfer?.setData('text/plain', pathTestId(props.path))
  event.dataTransfer!.effectAllowed = 'move'
}

function onDragOver(event: DragEvent) {
  if (!canReorder.value) {
    return
  }
  const sourcePathKey = getStructuredBlockDragPathKey()
  if (!sourcePathKey) {
    return
  }
  const sourceSegments = sourcePathKey.split('-').map((segment) => Number.parseInt(segment, 10))
  if (!areSiblingPaths(sourceSegments, props.path)) {
    return
  }
  event.preventDefault()
  dragOver.value = true
}

function onDragLeave() {
  dragOver.value = false
}

function onDrop(event: DragEvent) {
  dragOver.value = false
  if (!canReorder.value) {
    return
  }
  const sourcePathKey = getStructuredBlockDragPathKey() ?? event.dataTransfer?.getData('text/plain')
  if (!sourcePathKey) {
    return
  }
  const sourceSegments = sourcePathKey.split('-').map((segment) => Number.parseInt(segment, 10))
  if (!areSiblingPaths(sourceSegments, props.path)) {
    return
  }
  const fromIndex = sourceSegments[sourceSegments.length - 1]
  const toIndex = props.siblingIndex
  if (fromIndex !== undefined && fromIndex !== toIndex) {
    emit('reorder-block', sourceSegments, toIndex)
  }
  setStructuredBlockDragPathKey(null)
}

function onDragEnd() {
  dragOver.value = false
  setStructuredBlockDragPathKey(null)
}
</script>

<template>
  <div
    class="block-card"
    :class="{ 'block-card--drag-over': dragOver }"
    :data-testid="`structured-block-card-${nestedPathKey}`"
  >
    <!-- Drop target wraps card for same-layer reorder (CE-U02). -->
    <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -- drag-and-drop reorder is pointer-driven; handle has aria-label -->
    <div
      class="block-card__drop-target"
      role="group"
      :aria-label="nodeLabel(node.type)"
      @dragover="onDragOver"
      @dragleave="onDragLeave"
      @drop="onDrop"
    >
    <header class="block-card__header">
      <div class="block-card__title">
        <button
          v-if="canReorder"
          type="button"
          class="block-card__drag-handle"
          draggable="true"
          :aria-label="t('templates.structuredEditor.dragHandle')"
          data-testid="structured-block-drag-handle"
          @dragstart="onDragStart"
          @dragend="onDragEnd"
        >
          <el-icon><Rank /></el-icon>
        </button>
        <el-tag size="small" type="info">{{ nodeLabel(node.type) }}</el-tag>
      </div>
      <div v-if="!readonly" class="block-card__actions">
        <el-button
          link
          type="primary"
          size="small"
          data-testid="structured-block-copy"
          @click="emit('copy-block', path)"
        >
          {{ t('templates.structuredEditor.copyBlock') }}
        </el-button>
        <el-button
          link
          type="danger"
          size="small"
          data-testid="structured-block-remove"
          @click="emit('remove', path)"
        >
          {{ t('common.delete') }}
        </el-button>
      </div>
    </header>

    <template v-if="node.type === 'paragraph' || node.type === 'sectionHeading'">
      <div class="inline-row" @focusin="emit('focus-path', path)">
        <div
          v-for="(child, childIndex) in node.children ?? []"
          :key="`${child.type}-${childIndex}`"
          class="inline-item"
        >
          <el-input
            v-if="child.type === 'textRun' || child.type === 'text'"
            :model-value="child.value ?? ''"
            data-testid="paragraph-input"
            :readonly="readonly"
            :placeholder="t('templates.structuredEditor.textPlaceholder')"
            @focus="emit('focus-path', path)"
            @update:model-value="(value: string) => emit('update-inline-child', path, childIndex, { ...child, type: 'textRun', value })"
            @blur="emit('end-field-coalesce')"
          />
          <AppSearchSelect
            v-else-if="child.type === 'variable'"
            :model-value="child.key ?? ''"
            filterable
            :disabled="readonly"
            :placeholder="t('templates.structuredEditor.variablePlaceholder')"
            @focus="emit('focus-path', path)"
            @update:model-value="(value: string) => emit('update-inline-child', path, childIndex, { ...child, type: 'variable', key: value })"
          >
            <el-option
              v-for="option in variableSelectOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </AppSearchSelect>
          <el-tag v-else size="small">{{ nodeLabel(child.type) }}</el-tag>
        </div>
        <el-button v-if="!readonly" size="small" plain @click="emit('add-inline', path, 'textRun')">
          {{ t('templates.structuredEditor.addText') }}
        </el-button>
        <el-button v-if="!readonly" size="small" plain @click="emit('add-inline', path, 'variable')">
          {{ t('templates.structuredEditor.addVariable') }}
        </el-button>
      </div>
    </template>

    <template v-else-if="node.type === 'conditionBlock'">
      <ConditionExpressionInput
        :model-value="conditionExpression(node)"
        :variable-keys="conditionVariableKeys"
        :readonly="readonly"
        test-id="condition-expression-input"
        :placeholder="t('templates.structuredEditor.conditionPlaceholder')"
        @update:model-value="(value: string) => emit('update-block-field', path, 'conditionExpression', value)"
        @blur="emit('end-field-coalesce')"
      />
    </template>

    <template v-else-if="node.type === 'loopBlock'">
      <AppSearchSelect
        :model-value="loopVariable(node)"
        filterable
        :disabled="readonly"
        data-testid="loop-variable-select"
        :placeholder="t('templates.structuredEditor.loopVariablePlaceholder')"
        @update:model-value="(value: string) => emit('update-block-field', path, 'loopVariable', value)"
      >
        <el-option
          v-for="option in listVariableOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </AppSearchSelect>
    </template>

    <template v-else-if="node.type === 'tableComponentRef'">
      <!-- FOS-W3-6 — search select (allow-create when no catalogue options). -->
      <AppSearchSelect
        :model-value="node.tableComponentRef ?? ''"
        filterable
        allow-create
        default-first-option
        :disabled="readonly"
        data-testid="table-component-ref-select"
        :placeholder="t('templates.structuredEditor.tableRefPlaceholder')"
        @focus="emit('focus-path', path)"
        @update:model-value="(value: string) => emit('update-block-field', path, 'tableComponentRef', value)"
      >
        <el-option
          v-for="option in tableComponentOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </AppSearchSelect>
    </template>

    <template v-else-if="node.type === 'contentModuleRef'">
      <AppSearchSelect
        v-if="clauseReferenceOptions.length"
        :model-value="node.referenceKey ?? ''"
        filterable
        :disabled="readonly"
        :placeholder="t('templates.structuredEditor.clauseRefPlaceholder')"
        @update:model-value="(value: string) => emit('update-block-field', path, 'referenceKey', value)"
      >
        <el-option
          v-for="option in clauseReferenceOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </AppSearchSelect>
      <el-input
        v-else
        :model-value="node.referenceKey ?? ''"
        :readonly="readonly"
        :placeholder="t('templates.structuredEditor.clauseRefPlaceholder')"
        @update:model-value="(value: string) => emit('update-block-field', path, 'referenceKey', value)"
        @blur="emit('end-field-coalesce')"
      />
    </template>

    <p v-else class="node-meta">{{ node.type }}</p>

    <section
      v-if="showNestedPanel"
      class="nested-blocks"
      :data-testid="`nested-block-children-${nestedPathKey}`"
    >
      <p class="nested-blocks__label">{{ t('templates.structuredEditor.nestedChildrenLabel') }}</p>

      <StructuredContentBlockCard
        v-for="(child, childIndex) in nestedChildren"
        :key="`${child.type}-${childIndex}-${nestedPathKey}`"
        :node="child"
        :path="childPath(childIndex)"
        :sibling-index="childIndex"
        :sibling-count="nestedChildren.length"
        :readonly="readonly"
        :variable-select-options="variableSelectOptions"
        :list-variable-options="listVariableOptions"
        :clause-reference-options="clauseReferenceOptions"
        :table-component-options="tableComponentOptions"
        :node-label="nodeLabel"
        @remove="emit('remove', $event)"
        @update-inline-child="(childPathValue, inlineIndex, nextChild) => emit('update-inline-child', childPathValue, inlineIndex, nextChild)"
        @add-inline="(childPathValue, type) => emit('add-inline', childPathValue, type)"
        @update-block-field="(childPathValue, field, value) => emit('update-block-field', childPathValue, field, value)"
        @insert-nested-block="(parentPath, type) => emit('insert-nested-block', parentPath, type)"
        @reorder-block="(childPathValue, toIndex) => emit('reorder-block', childPathValue, toIndex)"
        @copy-block="(childPathValue) => emit('copy-block', childPathValue)"
        @focus-path="(childPathValue) => emit('focus-path', childPathValue)"
        @end-field-coalesce="emit('end-field-coalesce')"
      />

      <el-empty
        v-if="!nestedChildren.length"
        :description="t('templates.structuredEditor.nestedChildrenEmpty')"
        :image-size="48"
      />

      <div v-if="!readonly && canAddChildren" class="nested-blocks__toolbar">
        <span class="nested-blocks__toolbar-label">{{ t('templates.structuredEditor.nestedAddBlock') }}</span>
        <el-button
          v-for="type in nestedBlockTypes"
          :key="`${nestedPathKey}-${type}`"
          size="small"
          data-testid="insert-nested-block-node"
          @click="emit('insert-nested-block', path, type)"
        >
          {{ nodeLabel(type) }}
        </el-button>
      </div>

      <p v-else-if="!canAddChildren" class="nested-blocks__depth-limit" data-testid="nested-depth-limit">
        {{ t('templates.structuredEditor.nestedDepthLimit', { max: 3 }) }}
      </p>
    </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.block-card {
  margin-bottom: 0.75rem;
}

.block-card__drop-target {
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.block-card--drag-over .block-card__drop-target {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.block-card--highlight .block-card__drop-target {
  border-color: var(--color-warning);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-warning) 35%, transparent);
}

.block-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  gap: 0.5rem;
}

.block-card__title {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.block-card__actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.block-card__drag-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.15rem;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-muted);
  cursor: grab;

  &:active {
    cursor: grabbing;
  }
}

.inline-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.inline-item {
  flex: 1 1 12rem;
  min-width: 10rem;
}

.node-meta {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.nested-blocks {
  margin-top: 0.75rem;
  padding: 0.75rem;
  border-left: 3px solid var(--border-color);
  background: var(--surface-color);
  border-radius: var(--radius-sm);
}

.nested-blocks__label {
  margin: 0 0 0.5rem;
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 600;
}

.nested-blocks__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  margin-top: 0.75rem;
}

.nested-blocks__toolbar-label {
  font-size: 0.8rem;
  color: var(--text-muted);
  min-width: 5rem;
}

.nested-blocks__depth-limit {
  margin: 0.75rem 0 0;
  font-size: 0.8rem;
  color: var(--text-muted);
}
</style>
