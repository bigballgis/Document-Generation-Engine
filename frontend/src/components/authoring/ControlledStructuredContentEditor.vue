<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import PasteCleaningSummaryDialog from '@/components/authoring/PasteCleaningSummaryDialog.vue'
import { useTemplatesStore } from '@/stores/templates'
import type { MasterStyleCatalog, PasteCleaningSummary, VariableSchema } from '@/types/template'
import { buildVariableOptionLabel } from '@/utils/variableDisplayName'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  applyStyleToParagraphs,
  createNodeTemplate,
  insertBlockNode,
  parseStructuredContent,
  serializeStructuredContent,
  type ConfirmedNodeType,
  type StructuredContentDocument,
  type StructuredContentNode,
} from '@/utils/structuredContentNodes'

const props = defineProps<{
  modelValue: string
  templateId?: string
  variableKeys?: string[]
  variables?: VariableSchema[]
  contentModuleReferenceKeys?: string[]
  readonly?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t, te } = useI18n()
const templatesStore = useTemplatesStore()

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
const pasteInputRef = ref<HTMLInputElement | null>(null)

const blockNodeTypes: ConfirmedNodeType[] = [
  'sectionHeading',
  'paragraph',
  'list',
  'conditionBlock',
  'loopBlock',
  'tableComponentRef',
  'contentModuleRef',
]

const styleOptions = computed(() => styleCatalog.value?.entries ?? [])

const clauseReferenceOptions = computed(() =>
  (props.contentModuleReferenceKeys ?? []).map((referenceKey) => ({
    value: referenceKey,
    label: referenceKey,
  })),
)

const isReadonly = computed(() => props.readonly === true)

const variableCatalog = computed(() => {
  if (props.variables?.length) {
    return props.variables
  }
  return (props.variableKeys ?? []).map(
    (variableKey): VariableSchema => ({
      variableKey,
      variableType: 'TEXT',
      required: false,
      defaultValue: null,
      enumValues: null,
      description: null,
    }),
  )
})

const variableSelectOptions = computed(() =>
  variableCatalog.value.map((variable) => ({
    value: variable.variableKey,
    label: buildVariableOptionLabel(variable),
  })),
)

const listVariableOptions = computed(() =>
  variableCatalog.value
    .filter((variable) => variable.variableType === 'LIST' || variable.variableType === 'OBJECT')
    .map((variable) => ({
      value: variable.variableKey,
      label: buildVariableOptionLabel(variable),
    })),
)

function styleLabel(styleKey: string): string {
  const key = `templates.structuredEditor.styleCatalog.keys.${styleKey}`
  return te(key) ? t(key) : styleKey
}

watch(
  () => props.modelValue,
  (value) => {
    documentModel.value = parseStructuredContent(value || DEFAULT_STRUCTURED_CONTENT_JSON)
  },
)

watch(documentModel, (value) => {
  if (isReadonly.value) {
    return
  }
  emit('update:modelValue', serializeStructuredContent(value))
}, { deep: true })

const DEFAULT_STYLE_CATALOG: MasterStyleCatalog = {
  catalogVersion: '1.0',
  entries: [
    { styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' },
    { styleKey: 'Heading1', applicableNodeTypes: ['sectionHeading'], renderPurpose: 'HEADING' },
  ],
}

onMounted(async () => {
  if (!props.templateId) {
    styleCatalog.value = DEFAULT_STYLE_CATALOG
    selectedStyleKey.value = DEFAULT_STYLE_CATALOG.entries[0]?.styleKey ?? 'BodyText'
    return
  }
  loadingCatalog.value = true
  try {
    styleCatalog.value = await templatesStore.fetchMasterStyleCatalog(props.templateId)
    selectedStyleKey.value = styleCatalog.value.entries[0]?.styleKey ?? 'BodyText'
  } catch {
    ElMessage.error(t('templates.structuredEditor.error.loadCatalog'))
  } finally {
    loadingCatalog.value = false
  }
})

function nodeLabel(type: ConfirmedNodeType | string): string {
  const key = `templates.structuredEditor.nodes.${type}`
  return te(key) ? t(key) : type
}

function insertBlock(type: ConfirmedNodeType) {
  if (isReadonly.value) {
    return
  }
  documentModel.value = insertBlockNode(documentModel.value, type, selectedStyleKey.value)
}

function applySelectedStyle() {
  if (!selectedStyleKey.value || isReadonly.value) {
    return
  }
  documentModel.value = applyStyleToParagraphs(documentModel.value, selectedStyleKey.value)
}

function replaceBlock(index: number, next: StructuredContentNode) {
  const nodes = [...documentModel.value.nodes]
  nodes[index] = next
  documentModel.value = { ...documentModel.value, nodes }
}

function updateBlockField(index: number, field: keyof StructuredContentNode, value: string) {
  const node = documentModel.value.nodes[index]
  if (!node) {
    return
  }
  replaceBlock(index, { ...node, [field]: value })
}

function updateInlineChild(blockIndex: number, childIndex: number, nextChild: StructuredContentNode) {
  const node = documentModel.value.nodes[blockIndex]
  if (!node) {
    return
  }
  const children = [...(node.children ?? [])]
  children[childIndex] = nextChild
  replaceBlock(blockIndex, { ...node, children })
}

function addInlineToBlock(blockIndex: number, type: ConfirmedNodeType) {
  const node = documentModel.value.nodes[blockIndex]
  if (!node) {
    return
  }
  const children = [...(node.children ?? []), createNodeTemplate(type, selectedStyleKey.value)]
  replaceBlock(blockIndex, { ...node, children })
}

function removeBlock(index: number) {
  if (isReadonly.value) {
    return
  }
  documentModel.value = {
    ...documentModel.value,
    nodes: documentModel.value.nodes.filter((_, nodeIndex) => nodeIndex !== index),
  }
}

function conditionExpression(node: StructuredContentNode): string {
  return node.conditionExpression ?? node.key ?? ''
}

function loopVariable(node: StructuredContentNode): string {
  return node.loopVariable ?? node.key ?? ''
}

async function handlePasteFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  const html = await file.text()
  await runPasteClean(html)
  input.value = ''
}

async function runPasteClean(html: string) {
  if (!html.trim() || isReadonly.value || !props.templateId) {
    return
  }
  prePasteSnapshot.value = serializeStructuredContent(documentModel.value)
  try {
    const result = await templatesStore.pasteClean(props.templateId, {
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
  if (isReadonly.value) {
    return
  }
  const nodes = [...documentModel.value.nodes]
  if (!nodes.length) {
    nodes.push(createNodeTemplate('paragraph', selectedStyleKey.value))
  }
  const lastIndex = nodes.length - 1
  const target = nodes[lastIndex]
  if (!target) {
    return
  }
  const children = [...(target.children ?? []), createNodeTemplate(type, selectedStyleKey.value)]
  nodes[lastIndex] = { ...target, children }
  documentModel.value = { ...documentModel.value, nodes }
}
</script>

<template>
  <div class="structured-editor" data-testid="controlled-structured-content-editor">
    <p v-if="!isReadonly" class="editor-hint">{{ t('templates.structuredEditor.bindingHint') }}</p>
    <p v-else class="editor-hint">{{ t('templates.structuredEditor.readonlyHint') }}</p>

    <div
      v-if="!isReadonly"
      class="toolbar"
      role="toolbar"
      :aria-label="t('templates.structuredEditor.toolbar.label')"
    >
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
            :label="styleLabel(entry.styleKey)"
            :value="entry.styleKey"
          />
        </el-select>
        <el-button size="small" @click="applySelectedStyle">
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
          @change="handlePasteFile"
        />
        <el-button size="small" @click="pasteInputRef?.click()">
          {{ t('templates.structuredEditor.pasteFromFile') }}
        </el-button>
      </div>
    </div>

    <div class="editor-surface" data-testid="editor-paste-area">
      <article
        v-for="(node, index) in documentModel.nodes"
        :key="`${node.type}-${index}`"
        class="block-card"
      >
        <header class="block-card__header">
          <el-tag size="small" type="info">{{ nodeLabel(node.type) }}</el-tag>
          <el-button v-if="!isReadonly" link type="danger" size="small" @click="removeBlock(index)">
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
                :readonly="isReadonly"
                :placeholder="t('templates.structuredEditor.textPlaceholder')"
                @update:model-value="(value: string) => updateInlineChild(index, childIndex, { ...child, type: 'textRun', value })"
              />
              <AppSearchSelect
                v-else-if="child.type === 'variable'"
                :model-value="child.key ?? ''"
                filterable
                :disabled="isReadonly"
                :placeholder="t('templates.structuredEditor.variablePlaceholder')"
                @update:model-value="(value: string) => updateInlineChild(index, childIndex, { ...child, type: 'variable', key: value })"
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
            <el-button v-if="!isReadonly" size="small" plain @click="addInlineToBlock(index, 'textRun')">
              {{ t('templates.structuredEditor.addText') }}
            </el-button>
            <el-button v-if="!isReadonly" size="small" plain @click="addInlineToBlock(index, 'variable')">
              {{ t('templates.structuredEditor.addVariable') }}
            </el-button>
          </div>
        </template>

        <template v-else-if="node.type === 'conditionBlock'">
          <el-input
            :model-value="conditionExpression(node)"
            :readonly="isReadonly"
            :placeholder="t('templates.structuredEditor.conditionPlaceholder')"
            @update:model-value="(value: string) => updateBlockField(index, 'conditionExpression', value)"
          />
        </template>

        <template v-else-if="node.type === 'loopBlock'">
          <AppSearchSelect
            :model-value="loopVariable(node)"
            filterable
            :disabled="isReadonly"
            :placeholder="t('templates.structuredEditor.loopVariablePlaceholder')"
            @update:model-value="(value: string) => updateBlockField(index, 'loopVariable', value)"
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
            :readonly="isReadonly"
            :placeholder="t('templates.structuredEditor.tableRefPlaceholder')"
            @update:model-value="(value: string) => updateBlockField(index, 'tableComponentRef', value)"
          />
        </template>

        <template v-else-if="node.type === 'contentModuleRef'">
          <AppSearchSelect
            v-if="clauseReferenceOptions.length"
            :model-value="node.referenceKey ?? ''"
            filterable
            :disabled="isReadonly"
            :placeholder="t('templates.structuredEditor.clauseRefPlaceholder')"
            @update:model-value="(value: string) => updateBlockField(index, 'referenceKey', value)"
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
            :readonly="isReadonly"
            :placeholder="t('templates.structuredEditor.clauseRefPlaceholder')"
            @update:model-value="(value: string) => updateBlockField(index, 'referenceKey', value)"
          />
        </template>

        <p v-else class="node-meta">{{ node.type }}</p>
      </article>

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
