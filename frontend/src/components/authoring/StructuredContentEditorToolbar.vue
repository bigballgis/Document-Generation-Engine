<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MasterStyleCatalog } from '@/types/template'
import type { ConfirmedNodeType } from '@/utils/structuredContentNodes'

defineProps<{
  canUndo: boolean
  canRedo: boolean
  blockNodeTypes: ConfirmedNodeType[]
  styleOptions: MasterStyleCatalog['entries']
  selectedStyleKey: string
  loadingCatalog: boolean
  styleLabel: (styleKey: string) => string
  nodeLabel: (type: ConfirmedNodeType | string) => string
  /** Single cohesive toolbar plane — avoid stacked bordered groups (BEI-C3). */
  compact?: boolean
}>()

const emit = defineEmits<{
  undo: []
  redo: []
  'insert-block': [type: ConfirmedNodeType]
  'insert-inline': [type: ConfirmedNodeType]
  'update:selectedStyleKey': [value: string]
  'apply-style': []
  'paste-file': [event: Event]
}>()

const { t } = useI18n()
const pasteInputRef = ref<HTMLInputElement | null>(null)

function openPastePicker() {
  pasteInputRef.value?.click()
}
</script>

<template>
  <div
    class="toolbar"
    :class="{ 'toolbar--compact': compact }"
    role="toolbar"
    data-testid="structured-editor-toolbar"
    :aria-label="t('templates.structuredEditor.toolbar.label')"
  >
    <div class="toolbar-group" data-testid="structured-editor-toolbar-history">
      <span v-if="!compact" class="group-label">{{ t('templates.structuredEditor.toolbar.history') }}</span>
      <el-button
        size="small"
        data-testid="structured-editor-undo"
        :disabled="!canUndo"
        :aria-label="t('templates.structuredEditor.undo')"
        :title="t('templates.structuredEditor.undoTooltip')"
        @click="emit('undo')"
      >
        {{ t('templates.structuredEditor.undo') }}
      </el-button>
      <el-button
        size="small"
        data-testid="structured-editor-redo"
        :disabled="!canRedo"
        :aria-label="t('templates.structuredEditor.redo')"
        :title="t('templates.structuredEditor.redoTooltip')"
        @click="emit('redo')"
      >
        {{ t('templates.structuredEditor.redo') }}
      </el-button>
    </div>

    <div class="toolbar-group" data-testid="structured-editor-toolbar-blocks">
      <span v-if="!compact" class="group-label">{{ t('templates.structuredEditor.toolbar.blocks') }}</span>
      <el-button
        v-for="type in blockNodeTypes"
        :key="type"
        size="small"
        data-testid="insert-block-node"
        @click="emit('insert-block', type)"
      >
        {{ nodeLabel(type) }}
      </el-button>
    </div>

    <div class="toolbar-group" data-testid="structured-editor-toolbar-inline">
      <span v-if="!compact" class="group-label">{{ t('templates.structuredEditor.toolbar.inline') }}</span>
      <el-button size="small" data-testid="insert-variable" @click="emit('insert-inline', 'variable')">
        {{ nodeLabel('variable') }}
      </el-button>
      <el-button size="small" @click="emit('insert-inline', 'emphasis')">
        {{ nodeLabel('emphasis') }}
      </el-button>
      <el-button size="small" @click="emit('insert-inline', 'lineBreak')">
        {{ nodeLabel('lineBreak') }}
      </el-button>
    </div>

    <div class="toolbar-group" data-testid="structured-editor-toolbar-style">
      <span v-if="!compact" class="group-label">{{ t('templates.structuredEditor.toolbar.style') }}</span>
      <el-select
        :model-value="selectedStyleKey"
        size="small"
        :loading="loadingCatalog"
        data-testid="style-picker"
        :placeholder="t('templates.structuredEditor.stylePicker.placeholder')"
        @update:model-value="(value: string) => emit('update:selectedStyleKey', value)"
      >
        <el-option
          v-for="entry in styleOptions"
          :key="entry.styleKey"
          :label="styleLabel(entry.styleKey)"
          :value="entry.styleKey"
        />
      </el-select>
      <el-button size="small" @click="emit('apply-style')">
        {{ t('templates.structuredEditor.stylePicker.apply') }}
      </el-button>
    </div>

    <div class="toolbar-group" data-testid="structured-editor-toolbar-paste">
      <span v-if="!compact" class="group-label">{{ t('templates.structuredEditor.toolbar.paste') }}</span>
      <input
        ref="pasteInputRef"
        type="file"
        accept=".html,.htm,.txt"
        hidden
        :aria-label="t('templates.structuredEditor.pasteFromFile')"
        @change="emit('paste-file', $event)"
      />
      <el-button size="small" @click="openPastePicker">
        {{ t('templates.structuredEditor.pasteFromFile') }}
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted);

  &--compact {
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
    border: none;
    border-radius: var(--radius-sm);
    background: var(--surface-muted);
    box-shadow: inset 0 0 0 1px var(--border-color);
  }
}

.toolbar-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.toolbar--compact .toolbar-group {
  padding-right: var(--space-2);
  border-right: 1px solid var(--border-color);

  &:last-child {
    padding-right: 0;
    border-right: none;
  }
}

.group-label {
  font-size: 0.85rem;
  color: var(--text-muted);
  min-width: 4.5rem;
}
</style>
