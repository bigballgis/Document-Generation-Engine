<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import PasteCleaningSummaryDialog from '@/components/authoring/PasteCleaningSummaryDialog.vue'
import * as templatesApi from '@/api/templates'
import type { MasterStyleCatalog, PasteCleaningSummary } from '@/types/template'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  DISABLED_TOOLBAR_CAPABILITIES,
  applyStyleToParagraphs,
  createNodeTemplate,
  insertBlockNode,
  parseStructuredContent,
  serializeStructuredContent,
  type ConfirmedNodeType,
  type StructuredContentDocument,
} from '@/utils/structuredContentNodes'

const props = defineProps<{
  modelValue: string
  templateId: string
  variableKeys?: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t, te } = useI18n()

const loadingCatalog = ref(false)
const styleCatalog = ref<MasterStyleCatalog | null>(null)
const selectedStyleKey = ref('')
const documentModel = ref<StructuredContentDocument>(
  parseStructuredContent(props.modelValue || DEFAULT_STRUCTURED_CONTENT_JSON),
)
const pasteSummaryOpen = ref(false)
const pasteSummary = ref<PasteCleaningSummary | null>(null)
const pasteBlocked = ref(false)
const pendingPasteJson = ref<string | null>(null)
const prePasteSnapshot = ref(props.modelValue || DEFAULT_STRUCTURED_CONTENT_JSON)
const pasteAreaRef = ref<HTMLDivElement | null>(null)

const blockNodeTypes: ConfirmedNodeType[] = [
  'sectionHeading',
  'paragraph',
  'list',
  'conditionBlock',
  'loopBlock',
  'tableComponentRef',
]

const styleOptions = computed(() => styleCatalog.value?.entries ?? [])

watch(
  () => props.modelValue,
  (value) => {
    documentModel.value = parseStructuredContent(value || DEFAULT_STRUCTURED_CONTENT_JSON)
  },
)

watch(documentModel, (value) => {
  emit('update:modelValue', serializeStructuredContent(value))
}, { deep: true })

onMounted(async () => {
  loadingCatalog.value = true
  try {
    styleCatalog.value = await templatesApi.getMasterStyleCatalog(props.templateId)
    selectedStyleKey.value = styleCatalog.value.entries[0]?.styleKey ?? 'BodyText'
  } catch {
    ElMessage.error(t('templates.structuredEditor.error.loadCatalog'))
  } finally {
    loadingCatalog.value = false
  }
})

function nodeLabel(type: ConfirmedNodeType): string {
  const key = `templates.structuredEditor.nodes.${type}`
  return te(key) ? t(key) : type
}

function insertBlock(type: ConfirmedNodeType) {
  documentModel.value = insertBlockNode(documentModel.value, type, selectedStyleKey.value)
}

function applySelectedStyle() {
  if (!selectedStyleKey.value) {
    return
  }
  documentModel.value = applyStyleToParagraphs(documentModel.value, selectedStyleKey.value)
}

function updateParagraphText(index: number, value: string) {
  const nodes = [...documentModel.value.nodes]
  const node = nodes[index]
  if (!node) {
    return
  }
  const children = node.children ? [...node.children] : [{ type: 'textRun', value: '' }]
  const textRun = children[0] ?? { type: 'textRun', value: '' }
  children[0] = { ...textRun, type: 'textRun', value }
  nodes[index] = { ...node, children }
  documentModel.value = { ...documentModel.value, nodes }
}

function paragraphPreview(nodeIndex: number): string {
  const node = documentModel.value.nodes[nodeIndex]
  const textRun = node?.children?.[0]
  return textRun?.value ?? ''
}

async function handlePaste(event: ClipboardEvent) {
  event.preventDefault()
  const html = event.clipboardData?.getData('text/html') ?? event.clipboardData?.getData('text/plain') ?? ''
  if (!html.trim()) {
    return
  }
  prePasteSnapshot.value = serializeStructuredContent(documentModel.value)
  try {
    const result = await templatesApi.pasteClean(props.templateId, {
      sourceHtml: html,
      prePasteStructuredContentJson: prePasteSnapshot.value,
    })
    pasteSummary.value = result.summary
    pasteBlocked.value = result.blocked
    pendingPasteJson.value = result.cleanedStructuredContentJson
    prePasteSnapshot.value = result.prePasteSnapshotJson
    pasteSummaryOpen.value = true
  } catch {
    ElMessage.error(t('templates.structuredEditor.error.pasteClean'))
  }
}

function acceptPaste() {
  if (pendingPasteJson.value) {
    documentModel.value = parseStructuredContent(pendingPasteJson.value)
  }
  pendingPasteJson.value = null
}

function cancelPaste() {
  documentModel.value = parseStructuredContent(prePasteSnapshot.value)
  pendingPasteJson.value = null
}

function insertInline(type: ConfirmedNodeType) {
  const nodes = [...documentModel.value.nodes]
  if (!nodes.length) {
    nodes.push(createNodeTemplate('paragraph', selectedStyleKey.value))
  }
  const lastIndex = nodes.length - 1
  const target = nodes[lastIndex]
  if (!target) {
    return
  }
  const children = [...(target.children ?? [])]
  children.push(createNodeTemplate(type, selectedStyleKey.value))
  nodes[lastIndex] = { ...target, children }
  documentModel.value = { ...documentModel.value, nodes }
}
</script>

<template>
  <div class="structured-editor" data-testid="controlled-structured-content-editor">
    <div class="toolbar" role="toolbar" :aria-label="t('templates.structuredEditor.toolbar.label')">
      <div class="toolbar-group">
        <span class="group-label">{{ t('templates.structuredEditor.toolbar.blocks') }}</span>
        <el-button
          v-for="type in blockNodeTypes"
          :key="type"
          size="small"
          data-testid="insert-block-node"
          @click="insertBlock(type)"
        >
          {{ nodeLabel(type) }}
        </el-button>
      </div>

      <div class="toolbar-group">
        <span class="group-label">{{ t('templates.structuredEditor.toolbar.inline') }}</span>
        <el-button size="small" data-testid="insert-variable" @click="insertInline('variable')">
          {{ nodeLabel('variable') }}
        </el-button>
        <el-button size="small" @click="insertInline('emphasis')">
          {{ nodeLabel('emphasis') }}
        </el-button>
        <el-button size="small" @click="insertInline('underline')">
          {{ nodeLabel('underline') }}
        </el-button>
        <el-button size="small" @click="insertInline('lineBreak')">
          {{ nodeLabel('lineBreak') }}
        </el-button>
      </div>

      <div class="toolbar-group">
        <span class="group-label">{{ t('templates.structuredEditor.toolbar.style') }}</span>
        <el-select
          v-model="selectedStyleKey"
          size="small"
          :loading="loadingCatalog"
          data-testid="style-picker"
          :placeholder="t('templates.structuredEditor.stylePicker.placeholder')"
        >
          <el-option
            v-for="entry in styleOptions"
            :key="entry.styleKey"
            :label="entry.styleKey"
            :value="entry.styleKey"
          />
        </el-select>
        <el-button size="small" @click="applySelectedStyle">
          {{ t('templates.structuredEditor.stylePicker.apply') }}
        </el-button>
      </div>

      <div class="toolbar-group disabled-group">
        <span class="group-label">{{ t('templates.structuredEditor.toolbar.unavailable') }}</span>
        <el-tooltip
          v-for="capability in DISABLED_TOOLBAR_CAPABILITIES"
          :key="capability.id"
          :content="t(capability.reasonKey)"
        >
          <el-button size="small" disabled data-testid="disabled-toolbar-item">
            {{ t(capability.labelKey) }}
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <div
      ref="pasteAreaRef"
      class="editor-surface"
      contenteditable="true"
      data-testid="editor-paste-area"
      :aria-label="t('templates.structuredEditor.editorSurface')"
      @paste="handlePaste"
    >
      <div
        v-for="(node, index) in documentModel.nodes"
        :key="`${node.type}-${index}`"
        class="block-row"
      >
        <el-tag size="small" type="info">{{ nodeLabel(node.type as ConfirmedNodeType) }}</el-tag>
        <el-input
          v-if="node.type === 'paragraph' || node.type === 'sectionHeading'"
          :model-value="paragraphPreview(index)"
          data-testid="paragraph-input"
          @update:model-value="(value: string) => updateParagraphText(index, value)"
        />
        <span v-else class="node-meta">{{ node.type }}</span>
      </div>
      <el-empty
        v-if="!documentModel.nodes.length"
        :description="t('templates.structuredEditor.emptyDocument')"
      />
    </div>

    <details class="json-preview">
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

.editor-surface {
  min-height: 8rem;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-color);
}

.block-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.node-meta {
  font-size: 0.85rem;
  color: var(--text-muted);
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
