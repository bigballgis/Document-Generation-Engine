<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import type { ConfirmedNodeType, StructuredContentNode } from '@/utils/structuredContentNodes'

defineProps<{
  node: StructuredContentNode
  index: number
  readonly: boolean
  variableSelectOptions: Array<{ value: string; label: string }>
  listVariableOptions: Array<{ value: string; label: string }>
  clauseReferenceOptions: Array<{ value: string; label: string }>
  nodeLabel: (type: ConfirmedNodeType | string) => string
}>()

const emit = defineEmits<{
  remove: [index: number]
  'update-inline-child': [blockIndex: number, childIndex: number, nextChild: StructuredContentNode]
  'add-inline': [blockIndex: number, type: ConfirmedNodeType]
  'update-block-field': [index: number, field: keyof StructuredContentNode, value: string]
  'end-field-coalesce': []
}>()

const { t } = useI18n()

function conditionExpression(node: StructuredContentNode): string {
  return node.conditionExpression ?? node.key ?? ''
}

function loopVariable(node: StructuredContentNode): string {
  return node.loopVariable ?? node.key ?? ''
}
</script>

<template>
  <article class="block-card">
    <header class="block-card__header">
      <el-tag size="small" type="info">{{ nodeLabel(node.type) }}</el-tag>
      <el-button
        v-if="!readonly"
        link
        type="danger"
        size="small"
        @click="emit('remove', index)"
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
            @update:model-value="(value: string) => emit('update-inline-child', index, childIndex, { ...child, type: 'textRun', value })"
            @blur="emit('end-field-coalesce')"
          />
          <AppSearchSelect
            v-else-if="child.type === 'variable'"
            :model-value="child.key ?? ''"
            filterable
            :disabled="readonly"
            :placeholder="t('templates.structuredEditor.variablePlaceholder')"
            @update:model-value="(value: string) => emit('update-inline-child', index, childIndex, { ...child, type: 'variable', key: value })"
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
        <el-button v-if="!readonly" size="small" plain @click="emit('add-inline', index, 'textRun')">
          {{ t('templates.structuredEditor.addText') }}
        </el-button>
        <el-button v-if="!readonly" size="small" plain @click="emit('add-inline', index, 'variable')">
          {{ t('templates.structuredEditor.addVariable') }}
        </el-button>
      </div>
    </template>

    <template v-else-if="node.type === 'conditionBlock'">
      <el-input
        :model-value="conditionExpression(node)"
        :readonly="readonly"
        :placeholder="t('templates.structuredEditor.conditionPlaceholder')"
        @update:model-value="(value: string) => emit('update-block-field', index, 'conditionExpression', value)"
        @blur="emit('end-field-coalesce')"
      />
    </template>

    <template v-else-if="node.type === 'loopBlock'">
      <AppSearchSelect
        :model-value="loopVariable(node)"
        filterable
        :disabled="readonly"
        :placeholder="t('templates.structuredEditor.loopVariablePlaceholder')"
        @update:model-value="(value: string) => emit('update-block-field', index, 'loopVariable', value)"
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
        @update:model-value="(value: string) => emit('update-block-field', index, 'tableComponentRef', value)"
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
        @update:model-value="(value: string) => emit('update-block-field', index, 'referenceKey', value)"
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
        @update:model-value="(value: string) => emit('update-block-field', index, 'referenceKey', value)"
        @blur="emit('end-field-coalesce')"
      />
    </template>

    <p v-else class="node-meta">{{ node.type }}</p>
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
</style>
