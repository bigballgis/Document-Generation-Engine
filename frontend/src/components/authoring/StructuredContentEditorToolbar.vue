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
    role="toolbar"
    :aria-label="t('templates.structuredEditor.toolbar.label')"
  >
    <div class="toolbar-group">
      <span class="group-label">{{ t('templates.structuredEditor.toolbar.history') }}</span>
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

    <div class="toolbar-group">
      <span class="group-label">{{ t('templates.structuredEditor.toolbar.blocks') }}</span>
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

    <div class="toolbar-group">
      <span class="group-label">{{ t('templates.structuredEditor.toolbar.inline') }}</span>
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

    <div class="toolbar-group">
      <span class="group-label">{{ t('templates.structuredEditor.toolbar.style') }}</span>
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

    <div class="toolbar-group">
      <span class="group-label">{{ t('templates.structuredEditor.toolbar.paste') }}</span>
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
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}

.toolbar-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.group-label {
  font-size: 0.85rem;
  color: var(--text-muted);
  min-width: 4.5rem;
}
</style>
