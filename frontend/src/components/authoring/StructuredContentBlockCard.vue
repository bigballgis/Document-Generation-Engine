<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import StructuredContentBlockCard from '@/components/authoring/StructuredContentBlockCard.vue'
import {
  STRUCTURED_BLOCK_NODE_TYPES,
} from '@/composables/controlledStructuredContentEditorTypes'
import {
  canAddNestedBlockChildren,
  isNestedContainerType,
  pathTestId,
  type NodePath,
} from '@/utils/structuredContentNodePath'
import type { ConfirmedNodeType, StructuredContentNode } from '@/utils/structuredContentNodes'

const props = defineProps<{
  node: StructuredContentNode
  path: NodePath
  readonly: boolean
  variableSelectOptions: Array<{ value: string; label: string }>
  listVariableOptions: Array<{ value: string; label: string }>
  clauseReferenceOptions: Array<{ value: string; label: string }>
  nodeLabel: (type: ConfirmedNodeType | string) => string
}>()

const emit = defineEmits<{
  remove: [path: NodePath]
  'update-inline-child': [path: NodePath, childIndex: number, nextChild: StructuredContentNode]
  'add-inline': [path: NodePath, type: ConfirmedNodeType]
  'update-block-field': [path: NodePath, field: keyof StructuredContentNode, value: string]
  'insert-nested-block': [parentPath: NodePath, type: ConfirmedNodeType]
  'end-field-coalesce': []
}>()

const { t } = useI18n()

const nestedBlockTypes = STRUCTURED_BLOCK_NODE_TYPES
const canAddChildren = computed(() => canAddNestedBlockChildren(props.path))
const showNestedPanel = computed(() => isNestedContainerType(props.node.type))
const nestedChildren = computed(() => props.node.children ?? [])
const nestedPathKey = computed(() => pathTestId(props.path))

function conditionExpression(node: StructuredContentNode): string {
  return node.conditionExpression ?? node.key ?? ''
}

function loopVariable(node: StructuredContentNode): string {
  return node.loopVariable ?? node.key ?? ''
}

function childPath(childIndex: number): NodePath {
  return [...props.path, childIndex]
}
</script>

<template>
  <article class="block-card" :data-testid="`structured-block-card-${nestedPathKey}`">
    <header class="block-card__header">
      <el-tag size="small" type="info">{{ nodeLabel(node.type) }}</el-tag>
      <el-button
        v-if="!readonly"
        link
        type="danger"
        size="small"
        data-testid="structured-block-remove"
        @click="emit('remove', path)"
      >
        {{ t('common.delete') }}
      </el-button>
    </header>

    <template v-if="node.type === 'paragraph' || node.type === 'sectionHeading'">
      <div class="inline-row">
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
            @update:model-value="(value: string) => emit('update-inline-child', path, childIndex, { ...child, type: 'textRun', value })"
            @blur="emit('end-field-coalesce')"
          />
          <AppSearchSelect
            v-else-if="child.type === 'variable'"
            :model-value="child.key ?? ''"
            filterable
            :disabled="readonly"
            :placeholder="t('templates.structuredEditor.variablePlaceholder')"
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
      <el-input
        :model-value="conditionExpression(node)"
        :readonly="readonly"
        data-testid="condition-expression-input"
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
      <el-input
        :model-value="node.tableComponentRef ?? ''"
        :readonly="readonly"
        :placeholder="t('templates.structuredEditor.tableRefPlaceholder')"
        @update:model-value="(value: string) => emit('update-block-field', path, 'tableComponentRef', value)"
        @blur="emit('end-field-coalesce')"
      />
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
        :readonly="readonly"
        :variable-select-options="variableSelectOptions"
        :list-variable-options="listVariableOptions"
        :clause-reference-options="clauseReferenceOptions"
        :node-label="nodeLabel"
        @remove="emit('remove', $event)"
        @update-inline-child="(childPathValue, inlineIndex, nextChild) => emit('update-inline-child', childPathValue, inlineIndex, nextChild)"
        @add-inline="(childPathValue, type) => emit('add-inline', childPathValue, type)"
        @update-block-field="(childPathValue, field, value) => emit('update-block-field', childPathValue, field, value)"
        @insert-nested-block="(parentPath, type) => emit('insert-nested-block', parentPath, type)"
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
  </article>
</template>

<style scoped lang="scss">
.block-card {
  padding: 0.75rem;
  margin-bottom: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}

.block-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
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
